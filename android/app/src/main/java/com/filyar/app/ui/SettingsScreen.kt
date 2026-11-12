package com.filyar.app.ui

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import kotlin.math.roundToInt
import com.filyar.app.core.faNum
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Slider
import com.filyar.app.core.HomeStyle
import com.filyar.app.ui.theme.Filyar
import com.filyar.app.ui.theme.AccentColors
import com.filyar.app.core.FolderModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.core.ItemSize
import com.filyar.app.core.LongPressAction
import com.filyar.app.core.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: BrowserViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val vp by vm.viewPrefs.collectAsStateWithLifecycle()
    val homeBg by vm.homeBg.collectAsStateWithLifecycle()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setHomeBackground(uri)
    }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                title = { Text("تنظیمات", style = MaterialTheme.typography.titleLarge) },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Group("ظاهر برنامه")
            Label("تم")
            Chips(
                options = listOf(ThemeMode.System to "هماهنگ با سیستم", ThemeMode.Light to "روشن", ThemeMode.Dark to "تیره"),
                selected = vp.theme,
            ) { m -> vm.updateView { it.copy(theme = m) } }
            Label("چیدمان صفحه‌ی اصلی")
            Chips(
                options = listOf(HomeStyle.Cards to "کارتی (فعلی)", HomeStyle.Classic to "کلاسیک", HomeStyle.Glass to "شیشه‌ای"),
                selected = vp.homeStyle,
            ) { h -> vm.updateView { it.copy(homeStyle = h) } }
            Label("تصویر زمینه‌ی صفحه‌ی اصلی")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { pickImage.launch("image/*") }) { Text("انتخاب از گالری") }
                if (homeBg != null) OutlinedButton(onClick = vm::removeHomeBackground) { Text("حذف تصویر") }
            }
            Label("رنگ پس‌زمینه‌ی برنامه")
            BackgroundPicker(selected = vp.bgColor) { c -> vm.updateView { it.copy(bgColor = c) } }
            ScaleSlider("شدت محو پس‌زمینه‌ی صفحه‌ی اصلی", vp.bgBlur / 100f, range = 0f..1f, steps = 9) { v -> vm.updateView { it.copy(bgBlur = (v * 100).roundToInt()) } }
            val dynamicOn = vp.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Label(if (dynamicOn) "رنگ اصلی (با رنگ پویا خاموش می‌شود)" else "رنگ اصلی")
            AccentPicker(selected = vp.accent, enabled = !dynamicOn) { a -> vm.updateView { it.copy(accent = a) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    "رنگ پویا",
                    "رنگ‌ها از پس‌زمینه‌ی گوشی گرفته می‌شود",
                    vp.dynamicColor,
                ) { v -> vm.updateView { it.copy(dynamicColor = v) } }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 20.dp))
            Group("فایل‌ها و پوشه‌ها")
            Label("اندازه‌ی نمایش")
            Chips(
                options = listOf(ItemSize.Compact to "فشرده", ItemSize.Standard to "استاندارد", ItemSize.Large to "بزرگ"),
                selected = vp.itemSize,
            ) { sz -> vm.updateView { it.copy(itemSize = sz) } }
            ScaleSlider("اندازه‌ی آیکون فایل‌ها", vp.fileScale) { v -> vm.updateView { it.copy(fileScale = v) } }
            ScaleSlider("اندازه‌ی آیکون پوشه‌ها", vp.folderScale) { v -> vm.updateView { it.copy(folderScale = v) } }
            ScaleSlider("اندازه‌ی پوشه‌های دسترسی سریع", vp.quickScale) { v -> vm.updateView { it.copy(quickScale = v) } }
            ScaleSlider("فاصله‌ی بین آیتم‌ها", vp.itemGap, range = 0f..1.6f, steps = 7) { v -> vm.updateView { it.copy(itemGap = v) } }
            Label("تعداد ستون‌ها در نمای شبکه‌ای")
            Chips(
                options = listOf(0 to "خودکار", 3 to "۳", 4 to "۴", 5 to "۵", 6 to "۶"),
                selected = vp.gridColumns,
            ) { n -> vm.updateView { it.copy(gridColumns = n) } }
            Label("مدل پوشه")
            FolderModelPicker(selected = vp.folderModel) { m -> vm.updateView { it.copy(folderModel = m) } }
            Label("چیدمان پیش‌فرض پوشه‌ها")
            Chips(
                options = listOf(false to "فهرستی", true to "شبکه‌ای"),
                selected = vp.grid,
            ) { g -> vm.updateView { it.copy(grid = g) } }
            SwitchRow("کارت پشت آیتم‌ها", "قاب گرد پشت هر فایل و پوشه در نمای شبکه‌ای", vp.gridCards) { v ->
                vm.updateView { it.copy(gridCards = v) }
            }
            Label("رنگ پس‌زمینه‌ی کارت")
            CardColorPicker(selected = vp.cardColor, enabled = vp.gridCards) { c -> vm.updateView { it.copy(cardColor = c) } }
            SwitchRow("پیش‌نمایش تصویر و ویدیو", "نمایش بندانگشتی فایل‌های رسانه‌ای", vp.previews) { v ->
                vm.updateView { it.copy(previews = v) }
            }
            SwitchRow("آیکون برنامه برای پوشه‌ها", "مثلاً آیکون تلگرام برای پوشه‌ی Telegram", vp.appFolderIcons) { v ->
                vm.updateView { it.copy(appFolderIcons = v) }
            }
            SwitchRow("اول پوشه‌ها", "پوشه‌ها بالای فایل‌ها بیایند", vp.foldersFirst) { v ->
                vm.updateView { it.copy(foldersFirst = v) }
            }
            SwitchRow("نمایش فایل‌های مخفی", null, vp.showHidden) { v ->
                vm.updateView { it.copy(showHidden = v) }
            }
            Label("نگه داشتن انگشت روی پوشه")
            Chips(
                options = listOf(LongPressAction.Icon to "تغییر آیکون", LongPressAction.Select to "انتخاب"),
                selected = vp.folderLongPress,
            ) { a -> vm.updateView { it.copy(folderLongPress = a) } }

            HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 20.dp))
            Group("بازنشانی")
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = vm::resetQuickAccess, modifier = Modifier.weight(1f)) { Text("دسترسی سریع") }
                OutlinedButton(onClick = vm::clearFolderStyles, modifier = Modifier.weight(1f)) { Text("آیکون پوشه‌ها") }
            }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = vm::clearFolderLayouts, modifier = Modifier.weight(1f)) { Text("چیدمان پوشه‌ها") }
            }
        }
    }
}

@Composable
private fun Group(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
    )
}

@Composable
private fun <T> Chips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, body: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AccentPicker(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val options = listOf(-1) + AccentColors.indices.toList()
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { i ->
            val c = if (i < 0) Color(0xFF0F766E) else AccentColors[i]
            val sel = i == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.copy(alpha = if (enabled) 1f else 0.4f))
                    .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable(enabled = enabled) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (sel) Icon(Icons.Filled.Check, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun FolderModelPicker(selected: FolderModel, onSelect: (FolderModel) -> Unit) {
    val items = listOf(
        FolderModel.Tile to "کاشی",
        FolderModel.Circle to "دایره",
        FolderModel.Plain to "ساده",
        FolderModel.Outline to "خطی",
    )
    val tint = Filyar.tokens.folder
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (m, label) ->
            val sel = m == selected
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(if (sel) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FolderFrame(m, tint, 48.dp) {
                    Icon(Icons.Filled.Folder, null, tint = tint, modifier = Modifier.size(if (m == FolderModel.Plain) 44.dp else 27.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ScaleSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0.7f..1.6f,
    steps: Int = 8,
    onCommit: (Float) -> Unit,
) {
    var live by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${faNum((live * 100).roundToInt())}٪",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = live,
            onValueChange = { live = it },
            onValueChangeFinished = { onCommit(live) },
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun CardColorPicker(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val options = listOf(-1) + AccentColors.indices.toList()
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { i ->
            // The swatch is a little stronger than the real card tint so the choices are easy to tell apart.
            val swatch = if (i < 0) scheme.surfaceContainerHigh else lerp(scheme.surface, AccentColors[i], 0.35f)
            val sel = i == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(swatch.copy(alpha = if (enabled) 1f else 0.4f))
                    .border(if (sel) 3.dp else 1.dp, if (sel) scheme.primary else scheme.outlineVariant, CircleShape)
                    .clickable(enabled = enabled) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (sel) Icon(Icons.Filled.Check, null, tint = scheme.primary)
            }
        }
    }
}

@Composable
private fun BackgroundPicker(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    // -1 theme default, -2 pure white or black, then one tint per palette colour.
    val options = listOf(-1, -2) + AccentColors.indices.toList()
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { i ->
            val swatch = when (i) {
                -1 -> scheme.surfaceContainerHigh
                -2 -> if (dark) Color.Black else Color.White
                else -> lerp(if (dark) Color(0xFF14181A) else Color(0xFFF4F7F6), AccentColors[i], if (dark) 0.3f else 0.22f)
            }
            val sel = i == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(if (sel) 3.dp else 1.dp, if (sel) scheme.primary else scheme.outlineVariant, CircleShape)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (sel) Icon(Icons.Filled.Check, null, tint = scheme.primary)
            }
        }
    }
}
