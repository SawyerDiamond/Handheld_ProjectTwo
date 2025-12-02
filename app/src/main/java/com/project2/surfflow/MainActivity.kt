package com.project2.surfflow

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.project2.surfflow.ui.SurfForecastScreen
import com.project2.surfflow.ui.ai.AiActivity
import com.project2.surfflow.ui.profile.ProfileActivity
import com.project2.surfflow.ui.theme.SurfFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SurfFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SurfForecastScreen(
                        onNavigateToProfile = {
                            startActivity(Intent(this, ProfileActivity::class.java))
                        },
                        onNavigateToAi = { context ->
                            val intent = Intent(this, AiActivity::class.java)
                            intent.putExtra("SURF_CONTEXT", context)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}