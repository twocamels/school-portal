package com.schoolms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.schoolms.app.ui.navigation.SchoolMsNavGraph
import com.schoolms.app.ui.theme.SchoolMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Required API for Splashscreen per the handoff (androidx.core.splashscreen)
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        setContent {
            SchoolMSTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // Starts at Login by default implicitly
                    SchoolMsNavGraph(navController = navController)
                }
            }
        }
    }
}
