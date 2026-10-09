package com.example.msgedit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private data class MessageEntry(
    val id: Long,
    val original: String,
    val edited: String
)

private val AppBackground = Color(0xFF0B1118)
private val PanelColor = Color(0xFF17222D)
private val Mint = Color(0xFF34D399)
private val Danger = Color(0xFFFCA5A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MsgEditApp() }
    }
}

@Composable
private fun MsgEditApp() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("msg_edit_private", Context.MODE_PRIVATE)
    }

    var original by remember { mutableStateOf("") }
    var edited by remember { mutableStateOf("") }

    val history = remember(prefs) {
        mutableStateListOf<MessageEntry>().apply {
            addAll(loadHistory(prefs.getString("history", "[]") ?: "[]"))
        }
    }

    fun persist() {
        prefs.edit().putString("history", serializeHistory(history)).apply()
    }

    fun copyToClipboard(label: String, text: String) {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint,
            onPrimary = Color(0xFF07140E),
            background = AppBackground,
            surface = PanelColor,
            onSurface = Color(0xFFE5E7EB)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "MsgEdit",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Mint
                )
                Text(
                    text = "Your messages. Your personal copies.",
                    color = Color.LightGray
                )

                HorizontalDivider(color = Color.DarkGray)

                Text(
                    text = "1. Original message",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = original,
                    onValueChange = { original = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Paste the received message here...") },
                    shape = RoundedCornerShape(14.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { copyToClipboard("original", original) },
                        enabled = original.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy") }

                    Button(
                        onClick = { edited = original },
                        enabled = original.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Start editing") }
                }

                Text(
                    text = "2. Edit your personal copy",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = edited,
                    onValueChange = { edited = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Change the message text...") },
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { edited = original },
                        enabled = original.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset") }

                    OutlinedButton(
                        onClick = { copyToClipboard("edited copy", edited) },
                        enabled = edited.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy edited") }

                    Button(
                        onClick = {
                            history.add(
                                0,
                                MessageEntry(
                                    id = System.currentTimeMillis(),
                                    original = original,
                                    edited = edited
                                )
                            )
                            persist()
                            Toast.makeText(context, "Personal copy saved", Toast.LENGTH_SHORT).show()
                        },
                        enabled = original.isNotBlank() && edited.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }

                HorizontalDivider(color = Color.DarkGray)

                Text(
                    text = "Saved messages (${history.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (history.isEmpty()) {
                    Text(
                        text = "Your saved copies will appear here.",
                        color = Color.LightGray
                    )
                }

                history.toList().forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PanelColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ORIGINAL",
                                color = Mint,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(entry.original)

                            HorizontalDivider(color = Color.DarkGray)

                            Text(
                                text = "EDITED COPY",
                                color = Mint,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(entry.edited)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        original = entry.original
                                        edited = entry.edited
                                    }
                                ) { Text("Load") }

                                TextButton(
                                    onClick = { copyToClipboard("edited copy", entry.edited) }
                                ) { Text("Copy") }

                                TextButton(
                                    onClick = {
                                        history.removeAll { it.id == entry.id }
                                        persist()
                                    }
                                ) { Text(text = "Delete", color = Danger) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Saved on this device only. Editing here does not change the source message.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun loadHistory(json: String): List<MessageEntry> {
    val result = mutableListOf<MessageEntry>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                MessageEntry(
                    id = item.optLong("id", System.currentTimeMillis()),
                    original = item.optString("original"),
                    edited = item.optString("edited")
                )
            )
        }
    } catch (_: Exception) {
        // Invalid or older saved data is ignored.
    }
    return result
}

private fun serializeHistory(entries: List<MessageEntry>): String {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject().apply {
                put("id", entry.id)
                put("original", entry.original)
                put("edited", entry.edited)
            }
        )
    }
    return array.toString()
}
