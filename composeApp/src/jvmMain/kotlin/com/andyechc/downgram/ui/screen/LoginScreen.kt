package com.andyechc.downgram.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.CaretDown
import com.andyechc.downgram.CredentialManager
import com.andyechc.downgram.ui.components.CupertinoButton
import com.andyechc.downgram.ui.components.CupertinoTextField
import downgram.composeapp.generated.resources.Res
import downgram.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

data class CountryCode(val name: String, val code: String, val flag: String)

val countries = listOf(
    CountryCode("Rep. Dominicana", "+1-809", "🇩🇴"),
    CountryCode("Rep. Dominicana", "+1-829", "🇩🇴"),
    CountryCode("Rep. Dominicana", "+1-849", "🇩🇴"),
    CountryCode("USA", "+1", "🇺🇸"),
    CountryCode("Cuba", "+53", "🇨🇺"),
    CountryCode("Brasil", "+55", "🇧🇷"),
    CountryCode("México", "+52", "🇲🇽"),
    CountryCode("España", "+34", "🇪🇸"),
    CountryCode("Argentina", "+54", "🇦🇷"),
    CountryCode("Colombia", "+57", "🇨🇴"),
    CountryCode("Chile", "+56", "🇨🇱"),
    CountryCode("Perú", "+51", "🇵🇪"),
    CountryCode("Venezuela", "+58", "🇻🇪"),
    CountryCode("Ecuador", "+593", "🇪🇨")
)

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(countries[0]) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(400.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Downgram",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                "Inicia sesión con Telegram",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Spacer(Modifier.height(48.dp))

            CupertinoTextField(
                value = apiId,
                onValueChange = { apiId = it.filter { c -> c.isDigit() } },
                placeholder = "API ID",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            CupertinoTextField(
                value = apiHash,
                onValueChange = { apiHash = it },
                placeholder = "API Hash",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(0.4f)) {
                    Surface(
                        onClick = { showCountryDropdown = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${selectedCountry.flag} ${selectedCountry.code}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Icon(PhosphorIcons.Bold.CaretDown, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showCountryDropdown,
                        onDismissRequest = { showCountryDropdown = false },
                        modifier = Modifier.width(200.dp).heightIn(max = 300.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(country.flag, modifier = Modifier.padding(end = 8.dp))
                                        Text("${country.name} ${country.code}", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    selectedCountry = country
                                    showCountryDropdown = false
                                }
                            )
                        }
                    }
                }

                CupertinoTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() } },
                    placeholder = "Teléfono",
                    modifier = Modifier.weight(0.6f)
                )
            }

            Spacer(Modifier.height(40.dp))

            CupertinoButton(
                text = "Continuar",
                onClick = {
                    val fullPhone = selectedCountry.code + phoneNumber
                    if (apiId.isNotBlank() && apiHash.isNotBlank() && phoneNumber.isNotBlank()) {
                        CredentialManager.saveCredentials(apiId, apiHash, fullPhone)
                        onSuccess()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            CupertinoButton(
                text = "Importar sesión (.dwngm)",
                onClick = {
                    val chooser = javax.swing.JFileChooser()
                    chooser.dialogTitle = "Seleccionar archivo de sesión"
                    chooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
                        override fun accept(f: java.io.File) = f.isDirectory || f.name.endsWith(".dwngm")
                        override fun getDescription() = "Sesión Downgram (*.dwngm)"
                    }
                    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        try {
                            val lines = chooser.selectedFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                            if (lines.size < 3) {
                                statusMessage = "El archivo debe tener al menos 3 líneas (API ID, API Hash, Teléfono)"
                            } else {
                                apiId = lines[0]
                                apiHash = lines[1]
                                val fullPhone = lines[2]
                                // Match country code from phone
                                val matched = countries.find { fullPhone.startsWith(it.code) }
                                if (matched != null) {
                                    selectedCountry = matched
                                    phoneNumber = fullPhone.removePrefix(matched.code)
                                } else {
                                    phoneNumber = fullPhone
                                }
                                // If there's a 4th line, save as session
                                if (lines.size >= 4) {
                                    val sessionStr = lines[3]
                                    val sessionDir = java.io.File(System.getProperty("user.home"), ".downgram")
                                    sessionDir.mkdirs()
                                    java.io.File(sessionDir, "telegram_session.string").writeText(sessionStr)
                                }
                                statusMessage = "✅ Datos importados correctamente"
                            }
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.message}"
                        }
                    }
                },
            )

            statusMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
