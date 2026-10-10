package com.muse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MuseTheme { App() } }
    }
}

@Composable
fun App(vm: AppViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf("chat") }
    val snack = remember { SnackbarHostState() }

    BackHandler(enabled = screen != "chat") { screen = "chat" }
    LaunchedEffect(vm.notice) {
        vm.notice?.let {
            snack.showSnackbar(it)
            vm.notice = null
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (screen) {
            "compare" -> CompareScreen(vm, onBack = { screen = "chat" })
            "settings" -> SettingsScreen(vm, onBack = { screen = "chat" })
            else -> ChatScreen(vm, onSettings = { screen = "settings" }, onCompare = { screen = "compare" })
        }
        SnackbarHost(
            snack,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp),
        )
    }
}
