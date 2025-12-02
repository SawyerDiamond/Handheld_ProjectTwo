package com.project2.surfflow.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.project2.surfflow.R
import com.project2.surfflow.MainActivity
import com.project2.surfflow.ui.signup.SignUpActivity
import com.project2.surfflow.ui.theme.SurfFlowTheme
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in and "Remember Me" is active
        val sharedPref = getSharedPreferences("SurfFlowPrefs", MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("remember_me", true)

        if (auth.currentUser != null && rememberMe) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            SurfFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        onNavigateToSignUp = {
                            startActivity(Intent(this, SignUpActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToSignUp: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.remember_me), modifier = Modifier.weight(1f))
            Switch(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    // Save "Remember Me" preference
                                    val sharedPref = context.getSharedPreferences("SurfFlowPrefs", android.content.Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        putBoolean("remember_me", rememberMe)
                                        apply()
                                    }
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.auth_failed, task.exception?.message ?: ""),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, context.getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login_button))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text(stringResource(R.string.signup_prompt))
        }
    }
}
