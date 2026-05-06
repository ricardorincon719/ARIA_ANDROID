package com.ricardo.aria

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ricardo.aria.ui.theme.AriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AriaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AriaHome(this)
                }
            }
        }
    }
}

@Composable
fun AriaHome(context: Context) {

    val memoryStore = remember(context) { AriaMemoryStore(context) }
    val commandProcessor = remember { AriaCommandProcessor() }
    val memory = remember { memoryStore.load() }

    var usuario by remember { mutableStateOf(memory.user) }

    var texto by remember { mutableStateOf("") }
    var respuesta by remember { mutableStateOf("") }

    val recordatorios = remember {
        mutableStateListOf<String>().apply { addAll(memory.reminders) }
    }

    var ultimaConexion by remember { mutableStateOf(memory.lastConnection) }

    fun guardarMemoria() {
        ultimaConexion = memoryStore.save(usuario, recordatorios)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(20.dp)
    ) {

        Text(
            text = "ARIA",
            fontSize = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (usuario.isBlank()) {
            Text(
                text = "Antes de empezar, decime tu nombre:",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Tu nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (texto.isNotBlank()) {
                        usuario = texto.trim().replaceFirstChar { it.uppercase() }
                        texto = ""
                        guardarMemoria()
                        respuesta = "Bienvenido $usuario. Soy ARIA."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar nombre")
            }

            return@Column
        }

        Text(
            text = "Bienvenido de vuelta, $usuario",
            fontSize = 18.sp
        )

        Text(
            text = "Última conexión: $ultimaConexion",
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Escribí un comando...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (texto.isNotBlank()) {
                    respuesta = commandProcessor.process(
                        input = texto,
                        user = usuario,
                        reminders = recordatorios,
                        onMemoryChanged = ::guardarMemoria
                    )
                    texto = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar a ARIA")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (respuesta.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = respuesta,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tus recordatorios:",
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            itemsIndexed(recordatorios) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "${index + 1}. $item",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
