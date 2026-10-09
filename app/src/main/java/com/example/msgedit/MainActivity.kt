package com.example.msgedit

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private data class MessageEntry(
    val id: Long,
    val original: String,
    val edited: String
)

private data class SmsItem(
    val sender: String,
    val body: String,
    val date: Long
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

    var hasSmsPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val inbox = remember { mutableStateListOf<SmsItem>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) {
            inbox.clear()
            inbox.addAll(readInbox(context))
        } else {
            Toast.makeText(
                context,
                "SMS permission is needed to load the inbox",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission && inbox.isEmpty()) {
            inbox.addAll(readInbox(context))
        }
    }

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
                    text = "0. Load a received message (optional)",
                    style = MaterialTheme.typography.titleMedium
                )

                if (!hasSmsPermission) {
                    Text(
                        text = "Allow SMS access to read messages already received on this device. Nothing is sent anywhere, and the received message itself is never changed.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Allow SMS access") }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                inbox.clear()
                                inbox.addAll(readInbox(context))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Refresh inbox") }

                        Text(
                            text = "${inbox.size} received",
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }

                    if (inbox.isEmpty()) {
                        Text(
                            text = "No received messages found on this device.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    inbox.take(25).forEach { sms ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { original = sms.body },
                            colors = CardDefaults.cardColors(containerColor = PanelColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = sms.sender,
                                    color = Mint,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = sms.body,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to use as original",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

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

private fun readInbox(context: Context): List<SmsItem> {
    val items = mutableListOf<SmsItem>()
    try {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )
        cursor?.use { c ->
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
            var count = 0
            while (c.moveToNext() && count < 100) {
                val sender = if (addressIndex >= 0) c.getString(addressIndex) else null
                val body = if (bodyIndex >= 0) c.getString(bodyIndex) else null
                val date = if (dateIndex >= 0) c.getLong(dateIndex) else 0L
                items.add(
                    SmsItem(
                        sender = sender ?: "Unknown",
                        body = body ?: "",
                        date = date
                    )
                )
                count++
            }
        }
    } catch (_: Exception) {
        // If the inbox cannot be read, return what we have.
    }
    return items
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
