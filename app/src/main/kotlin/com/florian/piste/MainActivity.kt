package com.florian.piste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = WaypointStore(applicationContext)
        setContent {
            var glareMode by remember { mutableStateOf(store.loadSetting("glare_mode", "false") == "true") }
            WaypointTheme(glareMode = glareMode) {
                MapScreen(
                    store = store,
                    onToggleGlare = {
                        glareMode = !glareMode
                        store.saveSetting("glare_mode", glareMode.toString())
                    }
                )
            }
        }
    }
}
