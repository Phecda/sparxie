package me.phecda.sparxie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.phecda.sparxie.ui.SparxieApp
import me.phecda.sparxie.ui.theme.SparxieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SparxieTheme {
                SparxieApp()
            }
        }
    }
}
