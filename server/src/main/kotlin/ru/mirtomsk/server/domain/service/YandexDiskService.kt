package ru.mirtomsk.server.domain.service

/**
 * Domain service interface for uploading files to Yandex Disk
 */
interface YandexDiskService {
    /**
     * Upload file to Yandex Disk
     * @param filePath Path to the file to upload
     * @return URL or path to the uploaded file, or null if upload failed
     */
    suspend fun uploadFile(filePath: String): String?
}
