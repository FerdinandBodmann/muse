package com.muse.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(vm: AppViewModel, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var picker by remember { mutableStateOf(false) }
    val slots = vm.compareSlots

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text("Compare", fontFamily = FontFamily.Serif, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.compareRefs.forEach { r -> Pill("${r.model}  ✕", onClick = { vm.toggleCompare(r) }, filled = true) }
                if (vm.compareRefs.size < 3) Pill("+ Add model", onClick = { picker = true })
            }

            if (slots.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Pick up to three models, then ask one question and read the answers side by side.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                        color = cs.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    vm.comparePrompt.ifBlank { "(image)" },
                    fontSize = 14.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val pager = rememberPagerState { slots.size }
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp,
                ) { i -> SlotCard(slots[i]) }
            }

            Composer(
                vm = vm,
                images = vm.compareImages,
                streaming = slots.any { it.running },
                placeholder = "Ask every model",
                personaLabel = null,
                onPersona = null,
                onAttach = { vm.attach(it, forCompare = true) },
                onRemoveImage = { vm.removeImage(it, forCompare = true) },
                onSend = { vm.runCompare(it) },
                onStop = { vm.cancelCompare() },
            )
        }
    }

    if (picker) {
        ModelPickerDialog(
            vm = vm,
            multi = true,
            selected = vm.compareRefs,
            onToggle = { vm.toggleCompare(it) },
            onDismiss = { picker = false },
        )
    }
}

@Composable
private fun SlotCard(slot: CompareSlot) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = cs.primary, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    slot.ref.model,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (slot.running) Text("…", color = cs.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                if (slot.text.isBlank() && slot.error == null) {
                    Text("Thinking…", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = cs.onSurfaceVariant)
                } else if (slot.text.isNotBlank()) {
                    SelectionContainer { Markdown(slot.text, fontSize = 15.sp) }
                }
                slot.error?.let { Text(it, color = cs.error, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}
