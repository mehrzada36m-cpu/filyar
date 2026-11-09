package com.filyar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.RemoteFile
import com.filyar.app.core.FileBackend
import com.filyar.app.core.faNum
import com.filyar.app.core.formatDate
import com.filyar.app.core.formatSize

private const val MB = 1024L * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanScreen(vm: BrowserViewModel, snackbar: SnackbarHostState) {
    val c by vm.clean.collectAsStateWithLifecycle()
    val mode = c.mode ?: return
    BackHandler { vm.closeClean() }

    var confirm by remember { mutableStateOf<List<RemoteFile>?>(null) }
    var minBytes by remember { mutableLongStateOf(BrowserViewModel.DEFAULT_LARGE_MIN) }
    var custom by remember { mutableStateOf("") }
    val backend = vm.backend

    if (mode == CleanMode.Large) {
        // A smaller limit than the one already scanned needs a fresh scan.
        LaunchedEffect(minBytes, c.scanning, c.scannedMin, c.error) {
            if (!c.scanning && c.error == null) vm.ensureLargeScan(minBytes)
        }
    }

    val shown: List<RemoteFile> = if (mode == CleanMode.Large) c.large.filter { it.size >= minBytes } else emptyList()
    val selected = vm.cleanSelectedFiles()
    val selectedSize = selected.sumOf { it.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = vm::closeClean) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                title = {
                    Column {
                        Text(
                            if (mode == CleanMode.Large) "فایل‌های بزرگ" else "فایل‌های تکراری",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val sub = when {
                            c.scanning -> "در حال جستجو…"
                            mode == CleanMode.Large -> "${faNum(shown.size)} فایل • ${formatSize(shown.sumOf { it.size })}"
                            else -> "${faNum(c.groups.size)} گروه • ${formatSize(c.groups.sumOf { g -> g[0].size * (g.size - 1) })} قابل آزاد شدن"
                        }
                        Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (mode == CleanMode.Large && shown.isNotEmpty()) {
                        IconButton(onClick = { vm.cleanSelect(shown.map { it.path }, true) }) { Icon(Icons.Filled.SelectAll, "انتخاب همه") }
                    }
                    IconButton(
                        onClick = { if (mode == CleanMode.Large) vm.scanLarge(minBytes) else vm.scanDuplicates() },
                        enabled = !c.scanning,
                    ) { Icon(Icons.Filled.Refresh, "اسکن دوباره") }
                },
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${faNum(selected.size)} فایل انتخاب شده", style = MaterialTheme.typography.titleSmall)
                            Text(formatSize(selectedSize), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton2("لغو انتخاب") { vm.cleanClearSelection() }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { confirm = selected },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text("حذف", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (c.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    c.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            if (mode == CleanMode.Large) {
                SizeFilter(
                    minBytes = minBytes,
                    custom = custom,
                    onCustom = { text ->
                        custom = text.filter { it.isDigit() }.take(6)
                        custom.toLongOrNull()?.takeIf { it > 0 }?.let { minBytes = it * MB }
                    },
                    onChip = {
                        minBytes = it
                        custom = ""
                    },
                )
            } else if (c.groups.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::cleanSelectExtraCopies) {
                        Icon(Icons.Filled.DoneAll, null)
                        Spacer(Modifier.width(6.dp))
                        Text("انتخاب نسخه‌های اضافه")
                    }
                }
                Text(
                    "قدیمی‌ترین نسخه‌ی هر گروه نگه داشته می‌شود. فایل‌های Android/ بررسی نمی‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }

            when {
                c.error != null && !c.scanning -> StateMessage(Icons.Filled.AutoDelete, "جستجو ممکن نشد", c.error)
                mode == CleanMode.Large -> if (shown.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.SdStorage, "فایل بزرگی پیدا نشد", "حد حجم را کمتر کنید.")
                } else {
                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
                        items(shown, key = { it.path }) { f ->
                            CleanRow(f, backend, vm.rootPath, f.path in c.selection, null) { vm.cleanToggle(f.path) }
                        }
                    }
                }
                else -> if (c.groups.isEmpty() && !c.scanning) {
                    StateMessage(Icons.Filled.FileCopy, "فایل تکراری پیدا نشد", "فایل‌های هم‌اندازه و هم‌محتوا اینجا گروه‌بندی می‌شوند.")
                } else {
                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
                        c.groups.forEachIndexed { i, g ->
                            item(key = "h$i") {
                                Column {
                                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                                    Text(
                                        "گروه ${faNum(i + 1)} • ${faNum(g.size)} نسخه • هر کدام ${formatSize(g[0].size)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                    )
                                }
                            }
                            items(g.size, key = { j -> "$i:${g[j].path}" }) { j ->
                                val f = g[j]
                                CleanRow(f, backend, vm.rootPath, f.path in c.selection, if (j == 0) "قدیمی‌ترین • پیشنهاد نگه‌داشتن" else null) {
                                    vm.cleanToggle(f.path)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirm?.let { items ->
        val allOfGroup = mode == CleanMode.Duplicates && c.groups.any { g -> g.all { it.path in c.selection } }
        ConfirmDialog(
            title = "حذف نهایی؟",
            body = buildString {
                append("${faNum(items.size)} فایل (${formatSize(items.sumOf { it.size })}) برای همیشه حذف می‌شود و قابل بازگشت نیست.")
                if (allOfGroup) append("\n\nدر بعضی گروه‌ها همه‌ی نسخه‌ها انتخاب شده‌اند و هیچ کپی‌ای باقی نمی‌ماند.")
            },
            confirm = "حذف",
            danger = true,
            onDismiss = { confirm = null },
        ) {
            confirm = null
            vm.cleanDelete(items)
        }
    }
}

@Composable
private fun TextButton2(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}

@Composable
private fun SizeFilter(minBytes: Long, custom: String, onCustom: (String) -> Unit, onChip: (Long) -> Unit) {
    val options = listOf(10 * MB, 50 * MB, 100 * MB, 500 * MB, 1024 * MB)
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            "فایل‌های بزرگ‌تر از",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { o ->
                FilterChip(
                    selected = custom.isEmpty() && minBytes == o,
                    onClick = { onChip(o) },
                    label = { Text(formatSize(o)) },
                )
            }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = onCustom,
            label = { Text("حجم دلخواه (مگابایت)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CleanRow(f: RemoteFile, backend: FileBackend, root: String, checked: Boolean, tag: String?, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        FileIcon(f, backend, 46.dp, selected = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatSize(f.size)} • ${formatDate(f.lastModified)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                f.parent.removePrefix(root).ifEmpty { "/" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tag != null) Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
