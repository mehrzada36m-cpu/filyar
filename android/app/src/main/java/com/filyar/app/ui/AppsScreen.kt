package com.filyar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.core.faNum
import com.filyar.app.core.formatSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Installed apps: the user's own, the system's, or all of them, with quick actions for each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(vm: BrowserViewModel, snackbar: SnackbarHostState) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    BackHandler { vm.closeApps() }
    val tab by vm.appsTab.collectAsStateWithLifecycle()
    val all = apps.orEmpty()
    val userCount = all.count { !it.isSystem }
    val list = remember(apps, tab) {
        when (tab) {
            2 -> all.filter { it.isSystem }
            else -> all.filter { !it.isSystem }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
        Column {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = vm::closeApps) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                title = {
                    Column {
                        Text("برنامه‌ها", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (apps == null) "در حال خواندن…" else "${faNum(list.size)} برنامه",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = vm::loadApps) { Icon(Icons.Filled.Refresh, "تازه‌سازی") }
                },
            )
                    MiniPlayerBar(vm.audio)
        }
    },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (apps == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            AppsCapsule(selected = tab) { i -> if (i == 0) vm.openClean(CleanMode.Apks) else vm.setAppsTab(i) }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(list, key = { it.pkg }) { e -> AppRow(e, vm) }
            }
        }
    }
}

@Composable
private fun AppRow(e: AppEntry, vm: BrowserViewModel) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 44.dp.roundToPx() }
    var icon by remember(e.pkg) { mutableStateOf<ImageBitmap?>(AppIcons.cached(e.pkg, px)?.asImageBitmap()) }
    if (icon == null) {
        LaunchedEffect(e.pkg) {
            icon = withContext(Dispatchers.IO) { AppIcons.folderIcon(context, e.pkg, px) }?.asImageBitmap()
        }
    }
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            val bmp = icon
            if (bmp != null) Image(bmp, null, Modifier.fillMaxSize()) else Icon(Icons.Filled.Android, null, tint = scheme.outline)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "نسخه ${e.versionName.ifEmpty { "؟" }} • ${formatSize(e.apkSize)}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(e.pkg, style = MaterialTheme.typography.labelSmall, color = scheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(scheme.surfaceContainerHigh),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.launchApp(e.pkg) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.PlayArrow, "اجرا", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { vm.appSettings(e.pkg) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Info, "اطلاعات برنامه", modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Filled.MoreVert, "بیشتر", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("ذخیره‌ی APK در Download") },
                        leadingIcon = { Icon(Icons.Filled.Download, null) },
                        onClick = {
                            menu = false
                            vm.backupApk(e)
                        },
                    )
                    if (!e.isSystem) {
                        DropdownMenuItem(
                            text = { Text("حذف برنامه") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = {
                                menu = false
                                vm.uninstallApp(e.pkg)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Floating capsule shared by the apps screens: 0 = APK files, 1 = installed, 2 = system. */
@Composable
fun AppsCapsule(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val labels = listOf("فایل برنامه‌ها", "نصب‌شده‌ها", "سیستمی")
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
        ) {
            Row(Modifier.padding(4.dp)) {
                labels.forEachIndexed { i, label ->
                    val on = i == selected
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) scheme.primary else Color.Transparent)
                            .clickable { onSelect(i) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
