package com.zenevo.core_ATS

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zenevo.core_ATS.ui.AppNavigation
import com.zenevo.core_ATS.ui.theme.CoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoreTheme {
                // AppNavigation will now handle which screen is shown
                AppNavigation()
            }
        }
    }
}
