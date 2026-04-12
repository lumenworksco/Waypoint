package com.florian.piste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val viewModel: PisteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var glareMode by remember { mutableStateOf(
                viewModel.repository.loadSetting("glare_mode", "false") == "true"
            ) }
            WaypointTheme(glareMode = glareMode) {
                MapScreen(
                    viewModel = viewModel,
                    onToggleGlare = {
                        glareMode = !glareMode
                        viewModel.repository.saveSetting("glare_mode", glareMode.toString())
                    }
                )
            }
        }
    }
}
