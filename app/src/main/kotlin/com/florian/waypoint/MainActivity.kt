package com.florian.waypoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = WaypointStore(applicationContext)
        setContent {
            WaypointTheme {
                MapScreen(store = store)
            }
        }
    }
}
