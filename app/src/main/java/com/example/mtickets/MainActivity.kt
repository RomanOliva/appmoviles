package com.example.mtickets

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mtickets.data.AppDatabase
import com.example.mtickets.data.DatosPrueba
import com.example.mtickets.ui.componentes.BarraSuperior
import com.example.mtickets.ui.theme.MTicketsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MTicketsTheme {
                PantallaLogin()
            }
        }
    }
}

@Composable
fun PantallaLogin() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var baseLista by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DatosPrueba.sembrarSiVacio(db)
        baseLista = true
    }

    Scaffold(
        topBar = { BarraSuperior("MTickets") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Solicitudes de mantención",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Escuela Escritora Marcela Paz",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    error = ""
                },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = ""
                },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(), // ocultar la contraseña
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    scope.launch {
                        val usuario = db.usuarioDao().login(correo.trim(), password)
                        if (usuario == null) {
                            error = "Correo o contraseña incorrectos"
                        } else {
                            val intent = Intent(context, ListaTicketsActivity::class.java)
                            intent.putExtra(EXTRA_USUARIO_ID, usuario.id)
                            context.startActivity(intent)
                            password = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = baseLista && validarLogin(correo, password)
            ) {
                Text("Ingresar")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Usuarios de prueba (contraseña ${DatosPrueba.PASSWORD})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    DatosPrueba.usuariosDemo.forEach { demo ->
                        TextButton(
                            onClick = {
                                correo = demo.correo
                                password = demo.password
                                error = ""
                            }
                        ) {
                            Text("${demo.rol.etiqueta}: ${demo.nombre} (${demo.correo})")
                        }
                    }
                }
            }
        }
    }
}

fun validarLogin(correo: String, password: String): Boolean {
    return correo.isNotBlank() &&
            correo.contains("@") &&
            password.isNotBlank() &&
            password.length >= 6
}
