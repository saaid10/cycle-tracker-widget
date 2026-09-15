package com.cycletracker.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cycletracker.widget.auth.AuthResult
import com.cycletracker.widget.work.StatusRefreshWorker
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CycleTrackerApp

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        alreadySignedIn = app.authRepository.isSignedIn(),
                        onSignInSuccess = {
                            StatusRefreshWorker.enqueuePeriodic(applicationContext)
                            StatusRefreshWorker.enqueueOneTime(applicationContext)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    alreadySignedIn: Boolean,
    onSignInSuccess: () -> Unit
) {
    val app = LocalContext.current.applicationContext as CycleTrackerApp
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (alreadySignedIn) "Cycle Tracker" else "Sign in to Cycle Tracker",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))

        if (alreadySignedIn) {
            Text("You're already signed in. Add the widget to your home screen.")
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(top = 16.dp))

            if (errorMessage != null) {
                Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        when (val result = app.authRepository.signIn(email.trim(), password)) {
                            is AuthResult.Success -> onSignInSuccess()
                            is AuthResult.InvalidCredentials -> {
                                isLoading = false
                                errorMessage = "Incorrect email or password."
                            }
                            AuthResult.NetworkError -> {
                                isLoading = false
                                errorMessage = "Network error -- check your connection and try again."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Sign in")
                }
            }
        }
    }
}
