package com.muse.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Pill(text: String, onClick: () -> Unit, filled: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Text(
        text,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = if (filled) cs.onSurface else cs.onSurfaceVariant,
        modifier = Modifier
            .widthIn(max = 220.dp)
            .clip(CircleShape)
            .background(if (filled) cs.surfaceVariant else Color.Transparent)
            .border(1.dp, cs.outline, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontFamily = FontFamily.Serif,
        fontSize = 20.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
fun Thumb(vm: AppViewModel, name: String, size: Dp = 96.dp, onRemove: (() -> Unit)? = null) {
    val bmp = remember(name) { vm.store.loadBitmap(name, 2)?.asImageBitmap() }
    Box(Modifier.size(size)) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            )
        }
        if (onRemove != null) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clip(CircleShape)
                    .background(Color(0x99000000)).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun Composer(
    vm: AppViewModel,
    images: List<String>,
    streaming: Boolean,
    placeholder: String,
    personaLabel: String?,
    onPersona: (() -> Unit)?,
    onAttach: (List<Uri>) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var text by rememberSaveable { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
        if (uris.isNotEmpty()) onAttach(uris)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outline),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (images.isNotEmpty()) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    images.forEach { n -> Thumb(vm, n, 64.dp) { onRemoveImage(n) } }
                }
            }
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Default.Add, "Attach image", tint = cs.onSurfaceVariant) }
                if (personaLabel != null && onPersona != null) {
                    Text(
                        personaLabel,
                        fontSize = 12.sp,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp).clip(CircleShape)
                            .border(1.dp, cs.outlineVariant, CircleShape)
                            .clickable(onClick = onPersona)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (streaming) {
                    FilledIconButton(onClick = onStop) {
                        Box(Modifier.size(12.dp).background(cs.onPrimary, RoundedCornerShape(2.dp)))
                    }
                } else {
                    FilledIconButton(
                        onClick = { onSend(text); text = "" },
                        enabled = text.isNotBlank() || images.isNotEmpty(),
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                }
            }
        }
    }
}

@Composable
fun ModelPickerDialog(
    vm: AppViewModel,
    multi: Boolean,
    selected: List<ModelRef>,
    onToggle: (ModelRef) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var q by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (multi) "Done" else "Close") } },
        title = { Text(if (multi) "Pick up to 3 models" else "Choose a model", fontFamily = FontFamily.Serif) },
        text = {
            Column {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val providers = vm.configured
                if (providers.isEmpty()) {
                    Text(
                        "No providers yet. Add an API key in Settings.",
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                    providers.forEach { p ->
                        val models = (vm.modelLists[p.id] ?: emptyList()).filter { q.isBlank() || it.contains(q, ignoreCase = true) }
                        if (models.isNotEmpty()) {
                            item(key = "h_${p.id}") {
                                Text(
                                    p.name,
                                    fontSize = 12.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(models, key = { "${p.id}|$it" }) { m ->
                                val ref = ModelRef(p.id, m)
                                val on = ref in selected
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .clickable { onToggle(ref) }.padding(vertical = 10.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        m,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (on) cs.primary else cs.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (on) Icon(Icons.Default.Check, null, tint = cs.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
