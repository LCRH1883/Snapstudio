package com.snapswipe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.appcompat.app.AppCompatDelegate
import com.snapswipe.app.ui.SnapSwipeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales())
        super.onCreate(savedInstanceState)
        setContent {
            SnapSwipeApp()
        }
    }
}
