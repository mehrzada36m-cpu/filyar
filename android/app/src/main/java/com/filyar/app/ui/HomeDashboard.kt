package com.filyar.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filyar.app.core.ShizukuStatus
import com.filyar.app.core.formatSize
import com.filyar.app.ui.theme.Filyar

/** Persian names for the default quick access folders. */
private val friendlyNames = mapOf(
    "Download" to "دانلودها",
    "DCIM" to "دوربین",
    "Pictures" to "تصاویر",
    "Documents" to "اسناد",
    "Music" to "موسیقی",
    "Movies" to "فیلم‌ها",
    "Android/obb" to "فایل‌های بازی",
    "Android/data" to "دیتای برنامه‌ها",
)

@Composable
fun HomeDashboard(
    rootPath: String,
    shizuku: ShizukuStatus,
    localAccess: Boolean,
    storage: LongArray?,
    quickAccess: List<String>,
    onOpen: (String) -> Unit,
    onShizukuAction: () -> Unit,
    onShowGuide: () -> Unit,
    onGrantLocal: () -> Unit,
    onRemoveQuick: (String) -> Unit,
    onAddQuick: () -> Unit,
    onOpenCleaner: (CleanMode) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var cleanerOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)) {
        // Shizuku's state lives in the header text now; tapping it when it is off starts the setup.

        if (!localAccess && shizuku != ShizukuStatus.Ready) {
            item(key = "local") {
                InfoCard(
                    icon = Icons.Filled.LockOpen,
                    title = "دسترسی به حافظه",
                    body = "برای مرور پوشه‌های معمولی بدون Shizuku، اجازه‌ی دسترسی به همه‌ی فایل‌ها را بدهید.",
                    action = "اجازه دادن",
                    onAction = onGrantLocal,
                )
            }
        }

        item(key = "storage") { StorageCard(storage) { onOpen(rootPath) } }
        item(key = "data") { AndroidDataCard(shizuku == ShizukuStatus.Ready) { onOpen("$rootPath/Android/data") } }

        item(key = "cleaner") {
            CleanerCard(
                expanded = cleanerOpen,
                onToggle = { cleanerOpen = !cleanerOpen },
                onOpen = onOpenCleaner,
            )
        }

        item(key = "quickTitle") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("دسترسی سریع", Modifier.weight(1f))
                TextButton(onClick = { editing = !editing }) { Text(if (editing) "تمام" else "ویرایش") }
            }
        }
        val tiles: List<String?> = quickAccess + listOf(null)
        items(tiles.chunked(2), key = { row -> row.joinToString("|") { it ?: "+add" } }) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { path ->
                    if (path == null) {
                        AddTile(Modifier.weight(1f), onAddQuick)
                    } else {
                        QuickTile(
                            path = path,
                            rootPath = rootPath,
                            editing = editing,
                            modifier = Modifier.weight(1f),
                            onClick = { if (editing) onRemoveQuick(path) else onOpen(path) },
                            onLongClick = { editing = true },
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (editing) {
            item(key = "editHint") {
                Text(
                    "برای حذف، روی پوشه بزنید. برای افزودن، «افزودن پوشه» را بزنید یا در هر پوشه از منوی سه‌نقطه «افزودن به دسترسی سریع» را انتخاب کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ShizukuHero(status: ShizukuStatus, onAction: () -> Unit, onGuide: () -> Unit) {
    val t = Filyar.tokens
    val (title, body, action) = when (status) {
        ShizukuStatus.Ready -> Triple("Shizuku متصل است", "دسترسی کامل به Android/data و Android/obb فعال است.", null)
        ShizukuStatus.Connecting -> Triple("در حال اتصال…", "سرویس فایل‌یار در Shizuku راه‌اندازی می‌شود.", null)
        ShizukuStatus.NoPermission -> Triple("اجازه لازم است", "به فایل‌یار اجازه‌ی استفاده از Shizuku را بدهید.", "اجازه دادن")
        ShizukuStatus.NotRunning -> Triple("Shizuku اجرا نیست", "برنامه‌ی Shizuku را باز کنید و آن را روشن کنید.", "باز کردن Shizuku")
        ShizukuStatus.NotInstalled -> Triple("Shizuku نصب نیست", "برای دسترسی به Android/data باید Shizuku را نصب کنید.", "دریافت Shizuku")
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(t.heroBrush)
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(t.onHero.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Bolt, null, tint = t.warning) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = t.onHero)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = t.onHero.copy(alpha = 0.82f))
                }
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = t.onHero, contentColor = t.onHeroButton),
                    ) { Text(action, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onGuide,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = t.onHero),
                        border = BorderStroke(1.dp, t.onHero.copy(alpha = 0.5f)),
                    ) { Text("راهنمای راه‌اندازی") }
                }
            }
        }
    }
}

@Composable
private fun CardIcon(icon: ImageVector, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = fg) }
}

@Composable
private fun StorageCard(storage: LongArray?, onClick: () -> Unit) {
    val total = storage?.getOrNull(0) ?: 0L
    val free = storage?.getOrNull(1) ?: 0L
    val used = (total - free).coerceAtLeast(0L)
    val fraction = if (total > 0) used.toFloat() / total else 0f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardIcon(Icons.Filled.PhoneAndroid, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("حافظه داخلی", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (total > 0) "${formatSize(free)} خالی از ${formatSize(total)}" else "مرور همه‌ی فایل‌ها",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
            }
            if (total > 0) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Filyar.tokens.heroBrush),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatSize(used)} استفاده شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AndroidDataCard(ready: Boolean, onClick: () -> Unit) {
    val t = Filyar.tokens
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CardIcon(Icons.Filled.FolderSpecial, t.warning.copy(alpha = 0.16f), t.warning)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Android/data", style = MaterialTheme.typography.titleMedium)
                Text(
                    "دیتای برنامه‌ها و بازی‌ها",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val color = if (ready) t.success else t.warning
            Text(
                if (ready) "دسترسی کامل" else "نیاز به Shizuku",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun CleanerCard(expanded: Boolean, onToggle: () -> Unit, onOpen: (CleanMode) -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .combinedClickableCompat(onToggle)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardIcon(Icons.Filled.AutoDelete, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("حذف پیشرفته", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "آزاد کردن فضا با حذف فایل‌های بزرگ و تکراری",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.rotate(rotation))
            }
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CleanerOption(Icons.Filled.SdStorage, "فایل‌های بزرگ", "فایل‌های حجیم، از بزرگ به کوچک، با فیلتر حجم") { onOpen(CleanMode.Large) }
                    CleanerOption(Icons.Filled.FileCopy, "فایل‌های تکراری", "فایل‌های یکسانی که چند جا ذخیره شده‌اند") { onOpen(CleanMode.Duplicates) }
                }
            }
        }
    }
}

@Composable
private fun CleanerOption(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickTile(
    path: String,
    rootPath: String,
    editing: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val rel = path.removePrefix(rootPath).trimStart('/')
    val name = path.substringAfterLast('/')
    val label = friendlyNames[rel] ?: name
    val sub = if (friendlyNames.containsKey(rel)) rel else path.substringBeforeLast('/').removePrefix(rootPath).trimStart('/').ifEmpty { "حافظه داخلی" }
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FolderGlyph(path, name, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedVisibility(editing) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, "حذف", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun AddTile(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(10.dp))
            Text("افزودن پوشه", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f))
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}
