package com.example.duyoon.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.duyoon.R
import com.example.duyoon.ui.viewmodel.ThimmaViewModel

enum class AppLockMode {
    UNLOCK,
    CREATE,
    CONFIRM
}

@Composable
fun AppLockScreen(
    mode: AppLockMode = AppLockMode.UNLOCK,
    viewModel: ThimmaViewModel,
    onSuccess: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(mode) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val biometricManager = remember { BiometricManager.from(context) }
    val canUseBiometric = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val triggerBiometricPrompt: () -> Unit = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Thimma (ذمّة)")
                .setSubtitle("Use Fingerprint or Face to unlock")
                .setNegativeButtonText("Use Passcode")
                .build()
            prompt.authenticate(promptInfo)
        }
    }

    // Auto trigger biometric prompt on unlock mode
    LaunchedEffect(currentMode) {
        if (currentMode == AppLockMode.UNLOCK && canUseBiometric) {
            triggerBiometricPrompt()
        }
    }

    val titleText = when (currentMode) {
        AppLockMode.UNLOCK -> "Enter 4-Digit Passcode"
        AppLockMode.CREATE -> "Create 4-Digit Passcode"
        AppLockMode.CONFIRM -> "Confirm 4-Digit Passcode"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Space / Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            if (onCancel != null) {
                IconButton(onClick = onCancel) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }

        // Lock Icon & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 4 Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(4) { idx ->
                    val isFilled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        // Keypad (3x4 Grid) + Fingerprint Button
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (currentMode == AppLockMode.UNLOCK && canUseBiometric) "FINGERPRINT" else "", "0", "⌫")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { digit ->
                        if (digit.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (digit == "FINGERPRINT") {
                            IconButton(
                                onClick = { triggerBiometricPrompt() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                            ) {
                                Text(
                                    text = "👈👆",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    if (digit == "⌫") {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    } else if (enteredPin.length < 4) {
                                        enteredPin += digit
                                        if (enteredPin.length == 4) {
                                            when (currentMode) {
                                                AppLockMode.UNLOCK -> {
                                                    if (viewModel.verifyAppPin(enteredPin)) {
                                                        onSuccess()
                                                    } else {
                                                        errorMessage = "Incorrect Passcode"
                                                        enteredPin = ""
                                                    }
                                                }
                                                AppLockMode.CREATE -> {
                                                    firstPin = enteredPin
                                                    enteredPin = ""
                                                    currentMode = AppLockMode.CONFIRM
                                                }
                                                AppLockMode.CONFIRM -> {
                                                    if (enteredPin == firstPin) {
                                                        viewModel.setAppPin(enteredPin)
                                                        viewModel.setAppLockEnabled(true)
                                                        onSuccess()
                                                    } else {
                                                        errorMessage = "Passcodes do not match"
                                                        enteredPin = ""
                                                        currentMode = AppLockMode.CREATE
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Text(
                                    text = digit,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
