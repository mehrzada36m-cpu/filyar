package com.filyar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.filyar.app.RemoteFile
import com.filyar.app.core.FileBackend
import com.filyar.app.core.faNum
import com.filyar.app.core.formatDate
import com.filyar.app.core.formatSize

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    selectBaseName: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val end = if (selectBaseName) initial.lastIndexOf('.').takeIf { it > 0 } ?: initial.length else initial.length
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, end))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.text.isNotBlank()) onConfirm(value.text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }, enabled = value.text.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, danger: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun ProgressDialog(p: OpProgress, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(p.title) },
        text = {
            Column {
                val f = p.fraction
                if (f != null) {
                    LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${faNum((f * 100).toInt())}٪", style = MaterialTheme.typography.labelMedium)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (p.current.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        p.current,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text("لغو") } },
    )
}

@Composable
fun PropertiesDialog(f: RemoteFile, measure: suspend (RemoteFile) -> LongArray?, onDismiss: () -> Unit) {
    var info by remember { mutableStateOf<LongArray?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(f.path) {
        info = measure(f)
        loading = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, null) },
        title = { Text("مشخصات", fontWeight = FontWeight.Bold) },
        text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PropRow("نام", f.name)
                    PropRow("مسیر", f.path)
                    PropRow("نوع", kindOf(f).label + if (f.extension.isNotEmpty()) " (.${f.extension})" else "")
                    val i = info
                    PropRow(
                        "حجم",
                        when {
                            loading -> "در حال محاسبه…"
                            i == null -> "نامشخص"
                            else -> "${formatSize(i[0])}  (${faNum(i[0])} بایت)"
                        },
                    )
                    if (f.isDirectory && i != null) PropRow("محتوا", "${faNum(i[1])} فایل، ${faNum(i[2])} پوشه")
                    PropRow("آخرین تغییر", formatDate(f.lastModified))
                    PropRow("دسترسی", buildString {
                        append(if (f.canRead) "خواندن" else "بدون خواندن")
                        append("، ")
                        append(if (f.canWrite) "نوشتن" else "فقط خواندنی")
                    })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

data class SheetAction(val icon: ImageVector, val label: String, val danger: Boolean = false, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    header: (@Composable () -> Unit)?,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (header != null) {
                header()
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            actions.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            a.onClick()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val c = if (a.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    Icon(a.icon, null, tint = if (a.danger) c else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(18.dp))
                    Text(a.label, style = MaterialTheme.typography.bodyLarge, color = c)
                }
            }
        }
    }
}

@Composable
fun FileSheetHeader(f: RemoteFile, backend: FileBackend) {
    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FileIcon(f, backend, 52.dp, selected = false)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(f.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(metaLine(f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ShizukuGuideDialog(onDismiss: () -> Unit, onOpenShizuku: () -> Unit) {
    val steps = listOf(
        "Shizuku را از گوگل‌پلی یا سایت shizuku.rikka.app نصب کنید.",
        "در تنظیمات گوشی › درباره‌ی تلفن، هفت بار روی «شماره ساخت» بزنید تا «گزینه‌های توسعه‌دهنده» فعال شود.",
        "به وای‌فای وصل شوید و در گزینه‌های توسعه‌دهنده، «اشکال‌زدایی بی‌سیم» را روشن کنید.",
        "در Shizuku روی «جفت‌سازی» بزنید، سپس در اشکال‌زدایی بی‌سیم «جفت‌سازی با کد» را بزنید و کد را در اعلان Shizuku وارد کنید.",
        "در Shizuku روی «شروع» بزنید، به فایل‌یار برگردید و اجازه بدهید.",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("راه‌اندازی Shizuku بدون کامپیوتر", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(faNum(i + 1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(s, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "نکته: بعد از هر بار خاموش و روشن کردن گوشی، باید Shizuku را دوباره «شروع» کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onOpenShizuku) { Text("باز کردن Shizuku") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
    )
}

@Suppress("unused")
private val transparent = Color(0x00000000)
