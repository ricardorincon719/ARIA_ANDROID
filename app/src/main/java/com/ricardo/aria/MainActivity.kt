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
import androidx.compose.ui.Alignment
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

private enum class AriaScreen {
    Home,
    Chat,
    AddReminder,
    Reminders
}

@Composable
fun AriaHome(activity: ComponentActivity) {

    val context: Context = activity
    val memoryStore = remember(context) { AriaMemoryStore(context) }
    val commandProcessor = remember { AriaCommandProcessor() }
    val memory = remember { memoryStore.load() }

    var usuario by remember { mutableStateOf(memory.user) }

    var texto by remember { mutableStateOf("") }
    var reminderText by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf(AriaScreen.Home) }
    var pendingChatAction by remember { mutableStateOf<AriaUiAction?>(null) }

    val mensajes = remember {
        mutableStateListOf<AriaChatMessage>().apply {
            if (memory.chatMessages.isNotEmpty()) {
                addAll(memory.chatMessages)
            } else if (memory.user.isNotBlank()) {
                add(AriaChatMessage.SentByAria("Hola ${memory.user}. Lista para ayudarte."))
            }
        }
    }

    val recordatorios = remember {
        mutableStateListOf<String>().apply { addAll(memory.reminders) }
    }

    var ultimaConexion by remember { mutableStateOf(memory.lastConnection) }

    fun guardarMemoria() {
        ultimaConexion = memoryStore.save(usuario, recordatorios, mensajes)
    }

    fun guardarRecordatorio(text: String) {
        val cleanText = text.trim()
        if (cleanText.isNotBlank()) {
            recordatorios.add(cleanText)
            guardarMemoria()
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
                        mensajes.add(AriaChatMessage.SentByAria("Bienvenido $usuario. Soy ARIA."))
                        guardarMemoria()
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

        when (screen) {
            AriaScreen.Home -> HomeScreen(
                user = usuario,
                lastConnection = ultimaConexion,
                reminderCount = recordatorios.size,
                onOpenChat = { screen = AriaScreen.Chat },
                onOpenAddReminder = {
                    reminderText = ""
                    screen = AriaScreen.AddReminder
                },
                onOpenReminders = { screen = AriaScreen.Reminders }
            )

            AriaScreen.Chat -> ChatScreen(
                messages = mensajes,
                input = texto,
                pendingAction = pendingChatAction,
                onInputChange = { texto = it },
                onBack = {
                    texto = ""
                    pendingChatAction = null
                    screen = AriaScreen.Home
                },
                onOpenReminders = {
                    pendingChatAction = null
                    screen = AriaScreen.Reminders
                },
                onSend = {
                    if (texto.isNotBlank()) {
                        val entrada = texto.trim()
                        mensajes.add(AriaChatMessage.SentByUser(entrada))

                        val result = commandProcessor.processDetailed(
                            input = entrada,
                            user = usuario,
                            reminders = recordatorios,
                            onMemoryChanged = ::guardarMemoria
                        )
                        mensajes.add(AriaChatMessage.SentByAria(result.response))
                        guardarMemoria()
                        texto = ""

                        when (result.uiAction) {
                            AriaUiAction.SHOW_REMINDERS -> pendingChatAction = AriaUiAction.SHOW_REMINDERS
                            AriaUiAction.CLOSE_APP -> activity.finish()
                            null -> pendingChatAction = null
                        }
                    }
                }
            )

            AriaScreen.AddReminder -> AddReminderScreen(
                input = reminderText,
                onInputChange = { reminderText = it },
                onBack = {
                    reminderText = ""
                    screen = AriaScreen.Home
                },
                onAddReminder = {
                    guardarRecordatorio(reminderText)
                    reminderText = ""
                    screen = AriaScreen.Home
                }
            )

            AriaScreen.Reminders -> RemindersScreen(
                reminders = recordatorios,
                onBack = {
                    screen = AriaScreen.Home
                },
                onRemoveReminder = { index ->
                    if (index in recordatorios.indices) {
                        recordatorios.removeAt(index)
                        guardarMemoria()
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    user: String,
    lastConnection: String,
    reminderCount: Int,
    onOpenChat: () -> Unit,
    onOpenAddReminder: () -> Unit,
    onOpenReminders: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Hola $user",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Última conexión: $lastConnection",
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenChat,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hablar con ARIA")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenAddReminder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar recordatorio")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenReminders,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver recordatorios ($reminderCount)")
        }
    }
}

@Composable
private fun ChatScreen(
    messages: List<AriaChatMessage>,
    input: String,
    pendingAction: AriaUiAction?,
    onInputChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenReminders: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chat",
                fontSize = 24.sp
            )

            TextButton(onClick = onBack) {
                Text("Inicio")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(messages) { _, mensaje ->
                ChatBubble(message = mensaje)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pendingAction == AriaUiAction.SHOW_REMINDERS) {
            OutlinedButton(
                onClick = onOpenReminders,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver recordatorios")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Escribí un comando...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSend,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar a ARIA")
        }
    }
}

@Composable
private fun AddReminderScreen(
    input: String,
    onInputChange: (String) -> Unit,
    onBack: () -> Unit,
    onAddReminder: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Guardar",
                fontSize = 24.sp
            )

            TextButton(onClick = onBack) {
                Text("Inicio")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("Nuevo recordatorio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAddReminder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar recordatorio")
        }
    }
}

@Composable
private fun RemindersScreen(
    reminders: List<String>,
    onBack: () -> Unit,
    onRemoveReminder: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recordatorios",
                fontSize = 24.sp
            )

            TextButton(onClick = onBack) {
                Text("Inicio")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reminders.isEmpty()) {
            Text(
                text = "No tenés recordatorios pendientes.",
                fontSize = 16.sp
            )
        } else {
            LazyColumn {
                itemsIndexed(reminders) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. $item",
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(onClick = { onRemoveReminder(index) }) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AriaChatMessage) {
    val alignment = when (message) {
        is AriaChatMessage.SentByUser -> Alignment.End
        is AriaChatMessage.SentByAria -> Alignment.Start
    }

    val label = when (message) {
        is AriaChatMessage.SentByUser -> "Vos"
        is AriaChatMessage.SentByAria -> "ARIA"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            fontSize = 12.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
