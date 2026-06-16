package com.andyechc.downgram.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.Eye
import com.adamglin.phosphoricons.bold.EyeSlash
import com.andyechc.downgram.CredentialManager
import com.andyechc.downgram.data.repository.TelegramRepository
import com.andyechc.downgram.ui.components.CupertinoButton
import com.andyechc.downgram.ui.components.CupertinoTextButton
import com.andyechc.downgram.ui.components.SkeletonItem
import com.andyechc.downgram.ui.theme.CupertinoRed
import kotlinx.coroutines.launch

@Composable
fun OtpScreen(
    backendPort: Int,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var needs2FA by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("Ingresa el código que recibiste en Telegram") }

    val scope = rememberCoroutineScope()
    val repository = remember(backendPort) { TelegramRepository(backendPort) }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(400.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Verificación",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                infoMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            if (needs2FA) {
                var passwordVisible by remember { mutableStateOf(false) }
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    placeholder = { Text("Contraseña 2FA", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) PhosphorIcons.Bold.Eye else PhosphorIcons.Bold.EyeSlash,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            } else {
                com.andyechc.downgram.ui.components.CupertinoTextField(
                    value = code,
                    onValueChange = { if (it.all { c -> c.isDigit() }) code = it },
                    placeholder = "Código de verificación",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = CupertinoRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(40.dp))

            CupertinoButton(
                text = "Verificar",
                onClick = {
                    val creds = CredentialManager.getCredentials() ?: return@CupertinoButton
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        val result = repository.verifyCode(
                            apiId = creds["API_ID"]?.toIntOrNull() ?: 0,
                            apiHash = creds["API_HASH"] ?: "",
                            phone = creds["PHONE"] ?: "",
                            code = code,
                            password = if (needs2FA) password else null
                        )
                        result.onSuccess { response ->
                            if (response.success) onSuccess()
                            else if (response.need_password == true) {
                                needs2FA = true
                                infoMessage = "Ingresa tu contraseña de verificación en dos pasos."
                            } else errorMessage = "Código incorrecto"
                        }.onFailure { error ->
                            errorMessage = error.message ?: "Error desconocido"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && (code.length >= 4 || needs2FA)
            )

            Spacer(Modifier.height(24.dp))

            CupertinoTextButton(text = "Volver", onClick = onBack)
        }
    }
}
