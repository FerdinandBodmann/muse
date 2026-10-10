package com.muse.app

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AppViewModel, onSettings: () -> Unit, onCompare: () -> Unit) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pickModel by remember { mutableStateOf(false) }
    var pickPersona by remember { mutableStateOf(false) }
    var showParams by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ChatMessage?>(null) }
    val msgs = vm.current.messages

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    vm,
                    onClose = { scope.launch { drawer.close() } },
                    onSettings = onSettings,
                    onCompare = onCompare,
                    onParams = { showParams = true },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Menu") }
                    },
                    title = {
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).clickable { pickModel = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                vm.selected?.model ?: "Choose a model",
                                fontFamily = FontFamily.Serif,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.newChat() }) { Icon(Icons.Default.Add, "New chat") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                if (msgs.isEmpty()) {
                    EmptyState(vm, onSettings, Modifier.weight(1f).fillMaxWidth())
                } else {
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        items(msgs.asReversed(), key = { it.id }) { m ->
                            if (m.role == "user") UserBubble(vm, m) { editing = m }
                            else AssistantBlock(vm, m, isLast = m.id == msgs.last().id)
                        }
                    }
                }
                Composer(
                    vm = vm,
                    images = vm.pendingImages,
                    streaming = vm.streaming,
                    placeholder = "Message",
                    personaLabel = vm.personaName,
                    onPersona = { pickPersona = true },
                    onAttach = { vm.attach(it) },
                    onRemoveImage = { vm.removeImage(it) },
                    onSend = { vm.send(it) },
                    onStop = { vm.stop() },
                )
            }
        }
    }

    if (pickModel) {
        ModelPickerDialog(
            vm = vm,
            multi = false,
            selected = listOfNotNull(vm.selected),
            onToggle = { vm.choose(it); pickModel = false },
            onDismiss = { pickModel = false },
        )
    }
    if (pickPersona) {
        AlertDialog(
            onDismissRequest = { pickPersona = false },
            confirmButton = { TextButton(onClick = { pickPersona = false }) { Text("Close") } },
            title = { Text("Persona", fontFamily = FontFamily.Serif) },
            text = {
                Column {
                    vm.personas.forEach { p ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { vm.setPersona(p.id); pickPersona = false }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                        ) {
                            Text(
                                p.name,
                                color = if (p.id == vm.personaId) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            if (p.prompt.isNotBlank()) {
                                Text(
                                    p.prompt,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Text(
                        "Edit personas in Settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            },
        )
    }
    editing?.let { m ->
        EditMessageDialog(
            initial = m.text,
            onSend = { vm.editMessage(m.id, it); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (showParams) {
        ParamsDialog(vm, onDismiss = { showParams = false })
    }
}

@Composable
private fun EditMessageDialog(initial: String, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit message", fontFamily = FontFamily.Serif) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 3,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSend(text) }, enabled = text.isNotBlank()) { Text("Resend") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ParamsDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var temp by remember { mutableStateOf(vm.current.temperature?.toString().orEmpty()) }
    var maxTok by remember { mutableStateOf(vm.current.maxTokens?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Model parameters", fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Applies to this chat. Leave blank to use the provider's default.",
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = temp,
                    onValueChange = { s -> temp = s.filter { it.isDigit() || it == '.' } },
                    label = { Text("Temperature") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = maxTok,
                    onValueChange = { s -> maxTok = s.filter { it.isDigit() } },
                    label = { Text("Max output tokens") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.setParams(temp.toFloatOrNull(), maxTok.toIntOrNull())
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyState(vm: AppViewModel, onSettings: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✦", color = cs.primary, fontSize = 34.sp)
        Spacer(Modifier.height(12.dp))
        if (vm.configured.isEmpty()) {
            Text("Add an API key to begin", fontFamily = FontFamily.Serif, fontSize = 28.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSettings) { Text("Open settings") }
        } else {
            Text("How can I help?", fontFamily = FontFamily.Serif, fontSize = 30.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun UserBubble(vm: AppViewModel, m: ChatMessage, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (m.images.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { m.images.forEach { Thumb(vm, it, 96.dp) } }
        }
        if (m.text.isNotBlank()) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = cs.surfaceVariant,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                SelectionContainer {
                    Text(m.text, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp))
                }
            }
        }
        TextButton(
            onClick = onEdit,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) { Text("Edit", fontSize = 13.sp, color = cs.onSurfaceVariant) }
    }
}

@Composable
private fun AssistantBlock(vm: AppViewModel, m: ChatMessage, isLast: Boolean) {
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val busy = vm.streaming && isLast
    var showThinking by rememberSaveable(m.id) { mutableStateOf(false) }
    val reasoning = m.reasoning
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = cs.primary, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(m.model ?: "", fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        if (!reasoning.isNullOrBlank() && showThinking) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(14.dp),
                color = cs.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Thinking", fontSize = 11.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer { Markdown(reasoning, fontSize = 14.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (m.text.isBlank() && m.error == null) {
            Text("Thinking…", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = cs.onSurfaceVariant)
        } else if (m.text.isNotBlank()) {
            SelectionContainer { Markdown(m.text) }
        }
        m.error?.let {
            Text(it, color = cs.error, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
        if (!busy && (m.text.isNotBlank() || m.error != null || !reasoning.isNullOrBlank())) {
            Row {
                if (!reasoning.isNullOrBlank()) {
                    TextButton(
                        onClick = { showThinking = !showThinking },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text(if (showThinking) "Hide thinking" else "Show thinking", fontSize = 13.sp, color = cs.onSurfaceVariant) }
                }
                if (m.text.isNotBlank()) {
                    TextButton(
                        onClick = { clipboard.setText(AnnotatedString(m.text)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("Copy", fontSize = 13.sp, color = cs.onSurfaceVariant) }
                }
                if (isLast) {
                    TextButton(
                        onClick = { vm.regenerate() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) { Text("Retry", fontSize = 13.sp, color = cs.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    vm: AppViewModel,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    onCompare: () -> Unit,
    onParams: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<Conversation?>(null) }
    var renameFor by remember { mutableStateOf<Conversation?>(null) }
    val list = vm.conversations.filter { c ->
        query.isBlank() || c.title.contains(query, true) ||
            c.messages.any { it.text.contains(query, true) }
    }

    Column(Modifier.fillMaxHeight().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = cs.primary, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text("Muse", fontFamily = FontFamily.Serif, fontSize = 26.sp)
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = { vm.newChat(); onClose() }, modifier = Modifier.fillMaxWidth()) {
            Text("New chat")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search chats") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(list, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { vm.openConversation(c); onClose() }.padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        c.title,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (c.id == vm.current.id) cs.primary else cs.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { menuFor = c }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.MoreVert, "Chat actions", Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
            if (list.isEmpty() && query.isNotBlank()) {
                item {
                    Text(
                        "No matches",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = cs.outlineVariant)
        DrawerRow("Model parameters") { onClose(); onParams() }
        DrawerRow("Compare models") { onClose(); onCompare() }
        DrawerRow("Settings") { onClose(); onSettings() }
    }

    menuFor?.let { c ->
        ConversationMenuDialog(
            title = c.title,
            onRename = { menuFor = null; renameFor = c },
            onCopy = { clipboard.setText(AnnotatedString(vm.exportMarkdown(c))); menuFor = null },
            onShare = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, vm.exportMarkdown(c))
                    .putExtra(Intent.EXTRA_SUBJECT, c.title)
                context.startActivity(Intent.createChooser(send, "Share chat"))
                menuFor = null
            },
            onDelete = { vm.deleteConversation(c); menuFor = null },
            onDismiss = { menuFor = null },
        )
    }
    renameFor?.let { c ->
        RenameDialog(
            initial = c.title,
            onSave = { vm.renameConversation(c, it); renameFor = null },
            onDismiss = { renameFor = null },
        )
    }
}

@Composable
private fun ConversationMenuDialog(
    title: String,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Text(title, fontFamily = FontFamily.Serif, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column {
                MenuAction("Rename", onRename)
                MenuAction("Copy as Markdown", onCopy)
                MenuAction("Share", onShare)
                MenuAction("Delete", onDelete, danger = true)
            }
        },
    )
}

@Composable
private fun MenuAction(label: String, onClick: () -> Unit, danger: Boolean = false) {
    Text(
        label,
        fontSize = 16.sp,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    )
}

@Composable
private fun RenameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename chat", fontFamily = FontFamily.Serif) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DrawerRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    )
}
