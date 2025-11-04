package ru.mirtomsk.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.mirtomsk.shared.App
import ru.mirtomsk.shared.config.initAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Android context for resource access
        initAndroidContext(this)
        
        setContent {
            App()
        }
    }
}

