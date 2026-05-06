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
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val prefs = context.getSharedPreferences("aria_memoria", Context.MODE_PRIVATE)

    var usuario by remember {
        mutableStateOf(prefs.getString("usuario", "") ?: "")
    }

    var texto by remember { mutableStateOf("") }
    var respuesta by remember { mutableStateOf("") }

    val recordatorios = remember {
        mutableStateListOf<String>().apply {
            val json = prefs.getString("tareas", "[]") ?: "[]"

            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    add(array.getString(i))
                }
            } catch (_: JSONException) {
                prefs.edit().putString("tareas", "[]").apply()
            }
        }
    }

    var ultimaConexion by remember {
        mutableStateOf(prefs.getString("ultima_conexion", "Primera vez") ?: "Primera vez")
    }

    fun guardarMemoria() {
        val array = JSONArray()
        recordatorios.forEach { array.put(it) }

        val fechaActual = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        prefs.edit()
            .putString("usuario", usuario)
            .putString("tareas", array.toString())
            .putString("ultima_conexion", fechaActual)
            .apply()

        ultimaConexion = fechaActual
    }

    fun procesarComando(entradaOriginal: String): String {
        val entrada = entradaOriginal.lowercase().trim()

        return when {
            entrada == "hola" -> {
                "¡Hola $usuario! ¿Cómo puedo ayudarte hoy?"
            }

            entrada == "hora" -> {
                val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                "Son las $hora horas."
            }

            entrada == "fecha" -> {
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                "Hoy es $fecha."
            }

            entrada == "presentate" -> {
                "Soy ARIA, tu asistente personal. Tengo ${recordatorios.size} recordatorio(s) en memoria."
            }

            entrada.startsWith("recordar") -> {
                val tarea = entradaOriginal.drop(entradaOriginal.indexOf("recordar", ignoreCase = true) + "recordar".length).trim()

                if (tarea.isBlank()) {
                    "¿Qué querés que recuerde?"
                } else {
                    recordatorios.add(tarea)
                    guardarMemoria()
                    "He recordado: '$tarea'. Tenés ${recordatorios.size} recordatorio(s)."
                }
            }

            entrada == "recordatorios" -> {
                if (recordatorios.isEmpty()) {
                    "No tenés recordatorios pendientes."
                } else {
                    "Tenés ${recordatorios.size} recordatorio(s) guardado(s)."
                }
            }

            entrada.startsWith("olvidar") -> {
                val numeroTexto = entrada.removePrefix("olvidar").trim()
                val numero = numeroTexto.toIntOrNull()

                if (numero == null) {
                    "Decime el número del recordatorio. Ejemplo: olvidar 2"
                } else if (numero < 1 || numero > recordatorios.size) {
                    "Número inválido. Usá un número del 1 al ${recordatorios.size}."
                } else {
                    val eliminado = recordatorios.removeAt(numero - 1)
                    guardarMemoria()
                    "Eliminado: '$eliminado'. Te quedan ${recordatorios.size} recordatorio(s)."
                }
            }

            entrada == "ayuda" -> {
                """
                Comandos disponibles:
                hola
                hora
                fecha
                presentate
                recordar comprar pan
                recordatorios
                olvidar 1
                ayuda
                salir
                """.trimIndent()
            }

            entrada == "salir" -> {
                guardarMemoria()
                "Memoria guardada. Hasta luego $usuario."
            }

            else -> {
                "No entendí '$entradaOriginal'. Escribí 'ayuda' para ver los comandos."
            }
        }
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
                    respuesta = procesarComando(texto)
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
