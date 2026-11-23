package com.snapswipe.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import com.snapswipe.app.ui.SnapSwipeApp

class MainActivity : ComponentActivity() {
    private val returnRevision = mutableStateOf(0L)
    private var leftByChoice = false
    private var systemFlowInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales())
        super.onCreate(savedInstanceState)
        setContent {
            SnapSwipeApp(
                returnRevision = returnRevision.value,
                onSystemFlowChanged = { systemFlowInProgress = it }
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!systemFlowInProgress) leftByChoice = true
    }

    override fun onResume() {
        super.onResume()
        if (leftByChoice) {
            leftByChoice = false
            returnRevision.value = SystemClock.elapsedRealtime()
        }
    }
}
