package com.project2.surfflow.ui.ai

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.project2.surfflow.BuildConfig
import com.project2.surfflow.ui.theme.SurfFlowTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.ui.unit.sp

@Serializable
data class AiSurfAdvice(
    val summary: String,
    val bestTime: String,
    val bestBoard: String,
    val skillLevel: String,
    val wetsuit: String
)
class AiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SurfFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiScreen()
                }
            }
        }
    }
}


//Here I used Gemini in order to guide me on how best to display the data from the API in the context of a user request for a summary. Gemini provided me with a JSON object that I could use to display the data. 
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ContextCastToActivity")
@Composable
fun AiScreen() {
    val context = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)?.intent?.getStringExtra("SURF_CONTEXT") ?: ""
    
    var advice by remember { mutableStateOf<AiSurfAdvice?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { 
                text("You are a helpful surf advisor. Analyze the provided surf conditions and return a JSON object with the following fields: 'summary' (concise summary of conditions), 'bestTime' (best time to surf), 'bestBoard' (recommended board), 'skillLevel' (Beginner, Intermediate, or Advanced), and 'wetsuit' (wetsuit recommendation). Do not use markdown code blocks, just raw JSON.") 
            }
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val prompt = "Analyze these conditions: $context"
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text?.replace("```json", "")?.replace("```", "")?.trim()
                
                if (responseText != null) {
                    try {
                        advice = Json { ignoreUnknownKeys = true }.decodeFromString<AiSurfAdvice>(responseText)
                    } catch (e: Exception) {
                        advice = AiSurfAdvice(
                            summary = responseText,
                            bestTime = "See summary",
                            bestBoard = "See summary",
                            skillLevel = "See summary",
                            wetsuit = "See summary"
                        )
                    }
                } else {
                    error = "No response generated"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Surf Advisor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing surf conditions...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                error != null -> {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                advice != null -> {
                    AdviceContent(advice!!)
                }
            }
        }
    }
}

@Composable
fun AdviceContent(advice: AiSurfAdvice) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShadCard(
            title = "Summary",
            icon = Icons.Default.AutoAwesome,
            content = advice.summary
        )

        ShadCard(
            title = "Best Time",
            icon = Icons.Default.AccessTime,
            content = advice.bestTime
        )
        
        ShadCard(
            title = "Skill Level",
            icon = Icons.Default.SignalCellularAlt,
            content = advice.skillLevel
        )
        
        ShadCard(
            title = "Recommended Board",
            icon = Icons.Default.Surfing,
            content = advice.bestBoard
        )
        
        ShadCard(
            title = "Wetsuit Recommendation",
            icon = Icons.Default.Waves,
            content = advice.wetsuit
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ShadCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )
        }
    }
}
