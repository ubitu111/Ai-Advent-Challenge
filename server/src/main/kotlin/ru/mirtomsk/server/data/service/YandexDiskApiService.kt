package ru.mirtomsk.server.data.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import ru.mirtomsk.server.domain.service.YandexDiskService
import java.io.File

/**
 * Configuration for Yandex Disk API
 */
object YandexDiskConfig {
    /**
     * Get OAuth token from environment variable or system property
     * Priority: System property > Environment variable
     */
    fun getOAuthToken(): String? {
        return System.getProperty("yandex.disk.token")
            ?: System.getenv("YANDEX_DISK_TOKEN")
    }
}

/**
 * Implementation of YandexDiskService using Yandex Disk API
 */
class YandexDiskApiService(
    private val httpClient: HttpClient
) : YandexDiskService {

    companion object {
        private const val API_BASE_URL = "https://cloud-api.yandex.net/v1/disk"
        private val logger = LoggerFactory.getLogger(YandexDiskApiService::class.java)
    }

    override suspend fun uploadFile(filePath: String): String? {
        val rawToken = requireOAuthToken()
        // Clean token from whitespace and newlines
        val token = rawToken.trim().replace("\n", "").replace("\r", "")
        
        if (token.isEmpty()) {
            logger.error("Yandex Disk token is empty")
            return null
        }
        
        // Log token info for debugging (without exposing full token)
        logger.debug("Using Yandex Disk token (length: ${token.length}, starts with: ${token.take(10)}...)")
        
        // Verify token by making a test request
        if (!verifyToken(token)) {
            logger.error("Token verification failed. Please check your YANDEX_DISK_TOKEN")
            return null
        }
        
        val file = File(filePath)

        if (!file.exists()) {
            logger.error("File not found: $filePath")
            return null
        }

        if (!file.isFile) {
            logger.error("Path is not a file: $filePath")
            return null
        }

        return try {
            withContext(Dispatchers.IO) {
                // Step 1: Get upload URL
                val uploadUrl = getUploadUrl(token, file.name)
                    ?: return@withContext null

                // Step 2: Upload file
                val publicUrl = uploadFileToUrl(uploadUrl, file)
                    ?: return@withContext null

                // Step 3: Publish file (make it publicly accessible)
                val publishedUrl = publishFile(token, file.name)
                    ?: return@withContext null

                logger.info("File uploaded successfully: $publishedUrl")
                publishedUrl
            }
        } catch (e: Exception) {
            logger.error("Error uploading file to Yandex Disk: ${e.message}", e)
            null
        }
    }

    /**
     * Get upload URL from Yandex Disk API
     */
    private suspend fun getUploadUrl(token: String, fileName: String): String? {
        return try {
            logger.debug("Requesting upload URL for file: $fileName")
            val response: HttpResponse = httpClient.get("$API_BASE_URL/resources/upload") {
                header("Authorization", "OAuth $token")
                parameter("path", "/$fileName")
                parameter("overwrite", "true")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val result: UploadUrlResponse = response.body()
                    logger.debug("Upload URL received: ${result.href}")
                    result.href
                }
                HttpStatusCode.Unauthorized -> {
                    val errorText = response.body<String>()
                    logger.error("Authentication failed (401). Token may be invalid or expired. Error: $errorText")
                    logger.error("Please check:")
                    logger.error("1. Token is correct and not expired")
                    logger.error("2. Token has required permissions (cloud_api:disk.write)")
                    logger.error("3. Token format is correct (no extra spaces or characters)")
                    null
                }
                else -> {
                    val errorText = response.body<String>()
                    logger.error("Failed to get upload URL: ${response.status}, $errorText")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error getting upload URL: ${e.message}", e)
            null
        }
    }

    /**
     * Upload file to the provided URL
     */
    private suspend fun uploadFileToUrl(uploadUrl: String, file: File): String? {
        return try {
            val fileSize = file.length()
            val fileSizeMB = fileSize / (1024.0 * 1024.0)
            logger.info("Starting file upload: ${file.name} (${String.format("%.2f", fileSizeMB)} MB)")
            
            val fileBytes = file.readBytes()
            
            val response: HttpResponse = httpClient.put(uploadUrl) {
                contentType(ContentType.Application.OctetStream)
                setBody(fileBytes)
                // Override timeout for this specific request if needed (up to 15 minutes for very large files)
                timeout {
                    requestTimeoutMillis = 900_000L // 15 minutes for very large files
                    socketTimeoutMillis = 900_000L // 15 minutes for socket
                }
            }

            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> {
                    logger.info("File uploaded successfully: ${file.name}")
                    uploadUrl
                }
                else -> {
                    val errorText = response.body<String>()
                    logger.error("Failed to upload file: ${response.status}, $errorText")
                    null
                }
            }
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            logger.error("Upload timeout: File is too large or connection is too slow. File size: ${file.length() / (1024 * 1024)} MB")
            logger.error("Consider increasing timeout or checking network connection")
            null
        } catch (e: Exception) {
            logger.error("Error uploading file: ${e.message}", e)
            null
        }
    }

    /**
     * Publish file (make it publicly accessible)
     */
    private suspend fun publishFile(token: String, fileName: String): String? {
        return try {
            // First, check if file is already published
            logger.debug("Checking if file is already published: $fileName")
            val infoResponse: HttpResponse = httpClient.get("$API_BASE_URL/resources") {
                header("Authorization", "OAuth $token")
                parameter("path", "/$fileName")
            }

            if (infoResponse.status == HttpStatusCode.Unauthorized) {
                val errorText = infoResponse.body<String>()
                logger.error("Authentication failed (401) when checking file info. Error: $errorText")
                return null
            }

            if (infoResponse.status == HttpStatusCode.OK) {
                val result: FileInfoResponse = infoResponse.body()
                if (result.publicUrl != null) {
                    logger.info("File is already published: ${result.publicUrl}")
                    return result.publicUrl
                }
            }

            // If not published, publish it
            logger.debug("Publishing file: $fileName")
            val publishResponse: HttpResponse = httpClient.put("$API_BASE_URL/resources/publish") {
                header("Authorization", "OAuth $token")
                parameter("path", "/$fileName")
            }

            if (publishResponse.status == HttpStatusCode.Unauthorized) {
                val errorText = publishResponse.body<String>()
                logger.error("Authentication failed (401) when publishing file. Error: $errorText")
                return null
            }

            if (publishResponse.status != HttpStatusCode.OK && publishResponse.status != HttpStatusCode.Accepted) {
                val errorText = publishResponse.body<String>()
                logger.error("Failed to publish file: ${publishResponse.status}, $errorText")
                return null
            }

            // Wait a bit for the file to be published
            delay(2000)

            // Get public URL (retry up to 3 times)
            var retries = 3
            while (retries > 0) {
                val finalInfoResponse: HttpResponse = httpClient.get("$API_BASE_URL/resources") {
                    header("Authorization", "OAuth $token")
                    parameter("path", "/$fileName")
                }

                if (finalInfoResponse.status == HttpStatusCode.Unauthorized) {
                    val errorText = finalInfoResponse.body<String>()
                    logger.error("Authentication failed (401) when getting file info. Error: $errorText")
                    return null
                }

                if (finalInfoResponse.status == HttpStatusCode.OK) {
                    val result: FileInfoResponse = finalInfoResponse.body()
                    if (result.publicUrl != null) {
                        return result.publicUrl
                    }
                }

                retries--
                if (retries > 0) {
                    delay(1000)
                }
            }

            logger.warn("File published but public URL not available yet")
            // Return file path as fallback
            "https://disk.yandex.ru/client/disk/$fileName"
        } catch (e: Exception) {
            logger.error("Error publishing file: ${e.message}", e)
            null
        }
    }

    /**
     * Verify token by making a simple API request
     */
    private suspend fun verifyToken(token: String): Boolean {
        return try {
            val response: HttpResponse = httpClient.get("$API_BASE_URL") {
                header("Authorization", "OAuth $token")
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    logger.debug("Token verification successful")
                    true
                }
                HttpStatusCode.Unauthorized -> {
                    logger.error("Token verification failed: 401 Unauthorized")
                    logger.error("Possible reasons:")
                    logger.error("1. Token is expired or revoked")
                    logger.error("2. Token format is incorrect")
                    logger.error("3. Token doesn't have required permissions")
                    logger.error("4. Token contains extra spaces or characters")
                    false
                }
                else -> {
                    // Other status codes might be OK (like 404 for root path)
                    logger.debug("Token verification returned status: ${response.status}")
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Error verifying token: ${e.message}", e)
            false
        }
    }

    private fun requireOAuthToken(): String {
        val token = YandexDiskConfig.getOAuthToken()
        if (token == null || token.isBlank()) {
            throw IllegalStateException(
                "Yandex Disk OAuth token is not configured. " +
                "Please set YANDEX_DISK_TOKEN environment variable or yandex.disk.token system property."
            )
        }
        return token
    }

    @Serializable
    private data class UploadUrlResponse(
        val href: String,
        val method: String,
        val templated: Boolean = false
    )

    @Serializable
    private data class FileInfoResponse(
        val public_url: String? = null,
        val file: String? = null,
        val name: String? = null
    ) {
        val publicUrl: String? get() = public_url
    }
}
