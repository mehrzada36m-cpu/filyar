package com.filyar.app.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.clickable
import com.filyar.app.core.HomeStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Android
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.runtime.remember
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
import com.filyar.app.core.faNum
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
    onCustomizeQuick: (String) -> Unit,
    onOpenCleaner: (CleanMode) -> Unit,
    style: HomeStyle = HomeStyle.Cards,
    onOpenMedia: (MediaKind) -> Unit = {},
    onOpenBackups: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    stats: MediaStats = MediaStats(),
    onOpenApps: () -> Unit = {},
) {
    if (style == HomeStyle.Classic || style == HomeStyle.Glass) {
        CompositionLocalProvider(LocalGlass provides (style == HomeStyle.Glass)) {
            ClassicHome(rootPath, shizuku, localAccess, storage, quickAccess, onOpen, onGrantLocal, onRemoveQuick, onAddQuick, onCustomizeQuick, onOpenCleaner, onOpenMedia, onOpenBackups, listState, stats, onOpenApps)
        }
    } else {
        CardsHome(rootPath, shizuku, localAccess, storage, quickAccess, onOpen, onShizukuAction, onShowGuide, onGrantLocal, onRemoveQuick, onAddQuick, onCustomizeQuick, onOpenCleaner, onOpenBackups, listState)
    }
}

@Composable
private fun CardsHome(
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
    onCustomizeQuick: (String) -> Unit,
    onOpenCleaner: (CleanMode) -> Unit,
    onOpenBackups: () -> Unit,
    listState: LazyListState,
) {
    var cleanerOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(state = listState, contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)) {
        // Shizuku's state lives in the header text now; tapping it when it is off starts the setup.

        if (!localAccess && shizuku != ShizukuStatus.Ready) {
            item(key = "local") {
                PermissionCapsule(onGrantLocal)
            }
        }

        item(key = "storage") { StorageCard(storage) { onOpen(rootPath) } }
        item(key = "data") { AndroidDataCard(shizuku == ShizukuStatus.Ready) { onOpen("$rootPath/Android/data") } }

        item(key = "apks") {
            ActionCard(Icons.Filled.Android, "فایل‌های برنامه‌ها", "همه‌ی APKهای حافظه، با نسخه و مسیر هر کدام") { onOpenCleaner(CleanMode.Apks) }
        }
        item(key = "recent") {
            ActionCard(Icons.Filled.History, "فایل‌های اخیر", "تازه‌ترین فایل‌های ساخته یا تغییریافته") { onOpenCleaner(CleanMode.Recent) }
        }
        item(key = "backups") {
            ActionCard(Icons.Filled.Backup, "پشتیبان‌ها", "پشتیبان دیتای برنامه‌ها (Android/data)") { onOpenBackups() }
        }

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
                IconButton(onClick = onAddQuick) { Icon(Icons.Filled.Add, "افزودن پوشه") }
            }
        }
        items(quickAccess.chunked(2), key = { row -> "q" + row.joinToString("|") }) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { path ->
                    QuickTile(
                        path = path,
                        rootPath = rootPath,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpen(path) },
                        onCustomize = { onCustomizeQuick(path) },
                        onRemove = { onRemoveQuick(path) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CardIcon(icon, scheme.primary.copy(alpha = 0.14f), scheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = scheme.outline)
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
                        "آزاد کردن فضا با حذف فایل‌های بزرگ، تکراری و پوشه‌های باقی‌مانده",
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
                    CleanerOption(Icons.Filled.FolderOff, "پوشه‌های باقی‌مانده", "پوشه‌های Android/data برنامه‌های حذف‌شده") { onOpen(CleanMode.Leftovers) }
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
    modifier: Modifier,
    onClick: () -> Unit,
    onCustomize: () -> Unit,
    onRemove: () -> Unit,
    card: Boolean = true,
) {
    var menu by remember { mutableStateOf(false) }
    val rel = path.removePrefix(rootPath).trimStart('/')
    val name = path.substringAfterLast('/')
    val label = friendlyNames[rel] ?: name
    val sub = if (friendlyNames.containsKey(rel)) rel else path.substringBeforeLast('/').removePrefix(rootPath).trimStart('/').ifEmpty { "حافظه داخلی" }
    val glass = LocalGlass.current
    val tileShape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .then(if (glass) Modifier.padding(5.dp).glassCard(22.dp) else Modifier.clip(tileShape))
            .then(if (card && !glass) Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (glass) {
                FolderGlyph(path, name, 46.dp * LocalViewPrefs.current.quickScale)
            } else {
                FolderGlyph(path, name, 40.dp * LocalViewPrefs.current.quickScale)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("تغییر آیکون، رنگ و شکل") },
                leadingIcon = { Icon(Icons.Filled.Palette, null) },
                onClick = {
                    menu = false
                    onCustomize()
                },
            )
            DropdownMenuItem(
                text = { Text("حذف از دسترسی سریع") },
                leadingIcon = { Icon(Icons.Filled.Close, null) },
                onClick = {
                    menu = false
                    onRemove()
                },
            )
        }
    }
}

// Shown only on the first launch after install; the × hides it right away.
private var permCardFirstRun: Boolean? = null
private var permCardDismissed by mutableStateOf(false)

@Composable
private fun PermissionCapsule(onGrant: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (permCardFirstRun == null) {
        val sp = context.getSharedPreferences("filyar_ui", android.content.Context.MODE_PRIVATE)
        permCardFirstRun = !sp.getBoolean("perm_card_seen", false)
        sp.edit().putBoolean("perm_card_seen", true).apply()
    }
    if (permCardFirstRun != true || permCardDismissed) return
    val fg = MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.LockOpen, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "دسترسی به همه‌ی فایل‌ها",
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onGrant,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) { Text("اجازه دادن", style = MaterialTheme.typography.labelMedium) }
            IconButton(onClick = { permCardDismissed = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, "بستن", tint = fg, modifier = Modifier.size(18.dp))
            }
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

// ---------- classic home: sectioned list ----------

/** Key of the "recent files" tile that sits at the end of the quick access grid. */
private const val QUICK_RECENT = "\u0000recent"

/** The recent-files shortcut, drawn like a quick access tile. */
@Composable
private fun QuickRecentTile(modifier: Modifier, onClick: () -> Unit) {
    val glass = LocalGlass.current
    val tileShape = RoundedCornerShape(22.dp)
    val scale = LocalViewPrefs.current.quickScale
    val blue = Color(0xFF2F7DE1)
    Box(
        modifier
            .then(if (glass) Modifier.padding(5.dp).glassCard(22.dp) else Modifier.clip(tileShape))
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (glass) {
                GlassBubble(blue, 46.dp * scale) { Icon(Icons.Filled.History, null, tint = blue) }
            } else {
                Box(
                    Modifier.size(40.dp * scale).clip(CircleShape).background(blue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.History, null, tint = blue) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("فایل‌های اخیر", style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "تازه‌ترین فایل‌ها",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ClassicSection(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = if (LocalGlass.current && isDarkSurface()) Color(0xFF8AD8FF) else MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

// ---------- "liquid glass" look (third home style) ----------
private val LocalGlass = compositionLocalOf { false }

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Frosted, translucent card: soft fill, bright rim light, darker lower edge and a hand-drawn soft shadow. */
@Composable
private fun Modifier.glassCard(radius: Dp = 20.dp): Modifier {
    val dark = isDarkSurface()
    val shape = RoundedCornerShape(radius)
    val fill = if (dark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.04f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.18f)))
    }
    val rim = if (dark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.06f)))
    } else {
        Brush.linearGradient(listOf(Color.White, Color(0xFFB8C0CC).copy(alpha = 0.55f)))
    }
    val shadowColor = if (dark) Color.Black else Color(0xFF50607A)
    return this
        .drawBehind {
            val r = radius.toPx()
            val dy = 3.dp.toPx()
            val layers = if (dark) listOf(8.dp to 0.10f, 4.dp to 0.16f, 2.dp to 0.24f) else listOf(7.dp to 0.012f, 4.dp to 0.018f, 2.dp to 0.026f)
            layers.forEach { (grow, alpha) ->
                val g = grow.toPx()
                drawRoundRect(
                    color = shadowColor.copy(alpha = alpha),
                    topLeft = Offset(-g, -g + dy),
                    size = Size(size.width + 2 * g, size.height + 2 * g),
                    cornerRadius = CornerRadius(r + g),
                )
            }
        }
        .clip(shape)
        .background(fill)
        .border(1.dp, rim, shape)
}

/** Glossy round bubble that holds an icon, tinted by [tint]. */
@Composable
private fun GlassBubble(tint: Color, size: Dp = 52.dp, content: @Composable () -> Unit) {
    val dark = isDarkSurface()
    Box(
        Modifier
            .size(size)
            .drawBehind {
                (if (dark) listOf(7.dp to 0.20f, 3.dp to 0.30f) else listOf(6.dp to 0.06f, 3.dp to 0.09f)).forEach { (grow, alpha) ->
                    drawCircle(
                        color = tint.copy(alpha = alpha),
                        radius = this.size.minDimension / 2 + grow.toPx(),
                        center = center + Offset(0f, 2.dp.toPx()),
                    )
                }
            }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.42f else 0.95f), tint.copy(alpha = if (dark) 0.50f else 0.26f)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.65f else 1f), tint.copy(alpha = if (dark) 0.25f else 0.18f))),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ClassicTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    vPad: Dp = 10.dp,
    iconSize: Dp? = null,
    onClick: () -> Unit,
) {
    val glass = LocalGlass.current
    Row(
        modifier
            .then(if (glass) Modifier.padding(5.dp).glassCard() else Modifier.clip(RoundedCornerShape(16.dp)))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glass) {
            GlassBubble(tint, iconSize ?: 52.dp) { Icon(icon, null, tint = tint) }
        } else {
            Box(
                Modifier
                    .size(iconSize ?: 48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = if (small) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (small) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ClassicHome(
    rootPath: String,
    shizuku: ShizukuStatus,
    localAccess: Boolean,
    storage: LongArray?,
    quickAccess: List<String>,
    onOpen: (String) -> Unit,
    onGrantLocal: () -> Unit,
    onRemoveQuick: (String) -> Unit,
    onAddQuick: () -> Unit,
    onCustomizeQuick: (String) -> Unit,
    onOpenCleaner: (CleanMode) -> Unit,
    onOpenMedia: (MediaKind) -> Unit,
    onOpenBackups: () -> Unit,
    listState: LazyListState,
    stats: MediaStats,
    onOpenApps: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val total = storage?.getOrNull(0) ?: 0L
    val free = storage?.getOrNull(1) ?: 0L
    val blue = Color(0xFF2F7DE1)
    val red = Color(0xFFE5484D)
    val teal = Color(0xFF1F9E8F)
    val green = Color(0xFF4C9A2A)
    val purple = Color(0xFF7C6BE0)
    val amber = Color(0xFFD98A1B)

    LazyColumn(state = listState, contentPadding = PaddingValues(top = 0.dp, bottom = 32.dp)) {
        if (!localAccess && shizuku != ShizukuStatus.Ready) {
            item(key = "local") {
                PermissionCapsule(onGrantLocal)
            }
        }

        item(key = "filesTitle") { ClassicSection("فایل‌ها") }
        item(key = "storage") {
            ClassicTile(
                Icons.Filled.PhoneAndroid, blue, "حافظه داخلی",
                (if (total > 0) "${formatSize(total)} • ${formatSize(free)} خالی\n" else "") + rootPath,
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) { onOpen(rootPath) }
        }
        item(key = "data") {
            ClassicTile(
                Icons.Filled.FolderSpecial, amber, "Android/data",
                "دیتای برنامه‌ها و بازی‌ها",
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) { onOpen("$rootPath/Android/data") }
        }

        item(key = "quickTitle") {
            ClassicSection("دسترسی سریع") {
                IconButton(onClick = onAddQuick, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "افزودن پوشه") }
            }
        }
        items((quickAccess + QUICK_RECENT).chunked(2), key = { row -> "q" + row.joinToString("|") }) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { path ->
                    if (path == QUICK_RECENT) {
                        QuickRecentTile(Modifier.weight(1f)) { onOpenCleaner(CleanMode.Recent) }
                    } else {
                        QuickTile(
                            path = path,
                            rootPath = rootPath,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(path) },
                            onCustomize = { onCustomizeQuick(path) },
                            onRemove = { onRemoveQuick(path) },
                            card = false,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item(key = "mediaTitle") { ClassicSection("رسانه") }
        item(key = "media1") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                ClassicTile(
                    Icons.Filled.Image, blue, "تصاویر",
                    if (stats.loaded) "${faNum(stats.images)} تصویر\n${faNum(stats.cameraImages)} عکس دوربین" else "پوشه‌های دارای عکس",
                    Modifier.weight(1f).heightIn(min = 76.dp), small = true, vPad = 6.dp, iconSize = 44.dp,
                ) { onOpenMedia(MediaKind.Images) }
                ClassicTile(
                    Icons.Filled.Movie, red, "ویدیوها",
                    if (stats.loaded) "${faNum(stats.videos)} ویدیو\n${faNum(stats.cameraVideos)} ویدیوی دوربین" else "پوشه‌های دارای ویدیو",
                    Modifier.weight(1f).heightIn(min = 76.dp), small = true, vPad = 6.dp, iconSize = 44.dp,
                ) { onOpenMedia(MediaKind.Videos) }
            }
        }
        item(key = "media2") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                ClassicTile(
                    Icons.Filled.MusicNote, teal, "موسیقی",
                    if (stats.loaded) {
                        "${faNum(stats.tracks)} آهنگ • ${faNum(stats.audioFolders)} پوشه"
                    } else {
                        "پوشه‌های دارای آهنگ"
                    },
                    Modifier.weight(1f).heightIn(min = 76.dp), small = true, vPad = 6.dp, iconSize = 44.dp,
                ) { onOpenMedia(MediaKind.Audio) }
                ClassicTile(
                    Icons.Filled.Android, green, "برنامه‌ها",
                    if (stats.loaded) "${faNum(stats.userApps + stats.systemApps)} برنامه" else "مدیریت برنامه‌ها و فایل‌های نصبی",
                    Modifier.weight(1f).heightIn(min = 76.dp), small = true, vPad = 6.dp, iconSize = 44.dp,
                ) { onOpenApps() }
            }
        }

        item(key = "toolsTitle") { ClassicSection("ابزارها") }
        item(key = "tools") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                ClassicTile(Icons.Filled.SdStorage, purple, "فایل‌های بزرگ", "پاکسازی فایل‌های حجیم", Modifier.weight(1f), small = true, iconSize = 44.dp) {
                    onOpenCleaner(CleanMode.Large)
                }
                ClassicTile(Icons.Filled.FileCopy, purple, "فایل‌های تکراری", "حذف کپی‌های اضافه", Modifier.weight(1f), small = true, iconSize = 44.dp) {
                    onOpenCleaner(CleanMode.Duplicates)
                }
            }
        }
        item(key = "tools2") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                ClassicTile(Icons.Filled.FolderOff, red, "پوشه‌های باقی‌مانده", "دیتای برنامه‌های حذف‌شده", Modifier.weight(1f), small = true, iconSize = 44.dp) {
                    onOpenCleaner(CleanMode.Leftovers)
                }
                ClassicTile(Icons.Filled.Backup, amber, "پشتیبان‌ها", "پشتیبان دیتای برنامه‌ها", Modifier.weight(1f), small = true, iconSize = 44.dp) { onOpenBackups() }
            }
        }
    }
}
