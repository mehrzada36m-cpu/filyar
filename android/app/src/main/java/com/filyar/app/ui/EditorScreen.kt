package com.filyar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filyar.app.RemoteFile
import com.filyar.app.core.faNum
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(file: RemoteFile, vm: BrowserViewModel, snackbar: SnackbarHostState, onClose: () -> Unit) {
    var value by remember { mutableStateOf<TextFieldValue?>(null) }
    var original by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var wrap by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(file.path) {
        vm.readText(file)
            .onSuccess {
                original = it
                value = TextFieldValue(it)
            }
            .onFailure { error = it.message ?: "خواندن فایل ممکن نشد" }
    }

    val current = value
    val dirty = current != null && current.text != original
    val lines = remember(current?.text) { current?.text?.count { it == '\n' }?.plus(1) ?: 0 }

    BackHandler { if (dirty) confirmExit = true else onClose() }

    val save: () -> Unit = {
        val text = value?.text
        if (text != null && !saving) {
            saving = true
            scope.launch {
                if (vm.writeText(file, text).isSuccess) original = text
                saving = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                navigationIcon = {
                    IconButton(onClick = { if (dirty) confirmExit = true else onClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                },
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (dirty) "تغییرات ذخیره نشده" else "${faNum(lines)} خط",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { wrap = !wrap }) {
                        Icon(
                            Icons.AutoMirrored.Filled.WrapText,
                            "شکستن خطوط",
                            tint = if (wrap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = save, enabled = dirty && !saving) { Icon(Icons.Filled.Save, "ذخیره") }
                },
            )
        },
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .imePadding(),
        ) {
            when {
                error != null -> StateMessage(Icons.Filled.ErrorOutline, "باز نشد", error)
                current == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val h = rememberScrollState()
                    BasicTextField(
                        value = current,
                        onValueChange = { value = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDirection = TextDirection.Content,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .then(if (wrap) Modifier else Modifier.horizontalScroll(h))
                            .padding(16.dp),
                    )
                }
            }
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "تغییرات ذخیره نشده",
            body = "بدون ذخیره خارج می‌شوید؟",
            confirm = "خروج بدون ذخیره",
            danger = true,
            onDismiss = { confirmExit = false },
        ) {
            confirmExit = false
            onClose()
        }
    }
}
