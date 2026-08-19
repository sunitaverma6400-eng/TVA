package com.sudhanshu.tva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sudhanshu.tva.ui.navigation.TvaNavGraph
import com.sudhanshu.tva.ui.theme.TvaBackground
import com.sudhanshu.tva.ui.theme.TvaTheme

/**
 * TVA — Step 2: Android foundation.
 * Wires up theme, navigation, and the Control Room home screen with a live
 * relay ping button. Individual sections are placeholders until their
 * dedicated build steps.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.sudhanshu.tva.sync.SyncScheduler.ensureScheduled(this)

        // Step 18: prevent screenshots and the recent-apps thumbnail from
        // exposing personal timeline/people data. Trade-off: also blocks
        // the user's own legitimate screenshots of their own data — an
        // acceptable cost given what this app stores.
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            TvaTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvaBackground)
                ) {
                    TvaNavGraph()
                }
            }
        }
    }
}
