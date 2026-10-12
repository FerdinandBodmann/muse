package com.muse.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Persona?>(null) }
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = FontFamily.Serif, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionLabel("Providers") }
            items(Providers.all, key = { it.id }) { p ->
                ProviderCard(vm, p, open = expanded == p.id) { expanded = if (expanded == p.id) null else p.id }
            }
            item { SectionLabel("Personas") }
            items(vm.personas, key = { "persona_${it.id}" }) { p ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cs.surface,
                    border = BorderStroke(1.dp, cs.outlineVariant),
                    modifier = Modifier.fillMaxWidth().clickable { editing = p },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(p.name, fontSize = 15.sp)
                        if (p.prompt.isNotBlank()) {
                            Text(p.prompt, fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { editing = Persona(newId(), "", "") }) { Text("New persona") }
            }
            item {
                Text(
                    "API keys are stored encrypted on this device and only sent to the provider they belong to.",
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    editing?.let { p ->
        PersonaDialog(
            persona = p,
            canDelete = p.id != "default" && vm.personas.any { it.id == p.id },
            onSave = { vm.savePersona(it); editing = null },
            onDelete = { vm.deletePersona(p.id); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ProviderCard(vm: AppViewModel, p: Provider, open: Boolean, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var key by remember(p.id) { mutableStateOf(vm.keys[p.id].orEmpty()) }
    var url by remember(p.id) { mutableStateOf(vm.urls[p.id].orEmpty()) }
    var show by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf("") }
    val count = vm.modelLists[p.id]?.size ?: 0
    val ok = vm.isConfigured(p)
    val offline = p.id in keylessProviders

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 16.sp)
                    Text(
                        if (offline) "Ready, $count models" else if (ok) "Connected, $count models" else "Not set up",
                        fontSize = 12.sp,
                        color = if (offline || ok) cs.primary else cs.onSurfaceVariant,
                    )
                }
                Icon(
                    if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = cs.onSurfaceVariant,
                )
            }
            if (open) {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(if (p.id == "custom" || p.id in keylessProviders) "API key (optional)" else "API key") },
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.saveProvider(p, key, url); vm.notice = "${p.name} saved" }) { Text("Save") }
                        OutlinedButton(
                            onClick = { vm.saveProvider(p, key, url); vm.refreshModels(p) },
                            enabled = (key.isNotBlank() || p.id == "custom" || p.id in keylessProviders) && vm.fetching != p.id,
                        ) { Text(if (vm.fetching == p.id) "Fetching…" else "Fetch models") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { vm.saveProvider(p, key, url); vm.testProvider(p) },
                            enabled = (key.isNotBlank() || p.id == "custom" || p.id in keylessProviders) && vm.testing != p.id,
                        ) { Text(if (vm.testing == p.id) "Testing…" else "Test connection") }
                        if (key.isNotBlank()) {
                            TextButton(onClick = { key = ""; vm.saveProvider(p, "", url) }) { Text("Remove") }
                        }
                    }
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = { Text("Add model ID") },
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = {
                                if (extra.isNotBlank()) {
                                    vm.addModel(p, extra.trim())
                                    extra = ""
                                }
                            }) { Text("Add") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonaDialog(
    persona: Persona,
    canDelete: Boolean,
    onSave: (Persona) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(persona.name) }
    var prompt by remember { mutableStateOf(persona.prompt) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Persona", fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("System prompt") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(persona.copy(name = name.trim(), prompt = prompt.trim())) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (canDelete) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
