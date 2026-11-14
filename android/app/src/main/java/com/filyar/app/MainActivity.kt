package com.filyar.app

import com.filyar.app.ui.AppsScreen
import com.filyar.app.ui.FileListScreen
import com.filyar.app.ui.MediaFoldersScreen
import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.ui.BrowserScreen
import com.filyar.app.ui.BrowserViewModel
import com.filyar.app.ui.CleanScreen
import com.filyar.app.ui.EditorScreen
import com.filyar.app.ui.LocalFolderStyles
import com.filyar.app.ui.LocalViewPrefs
import com.filyar.app.ui.SettingsScreen
import com.filyar.app.ui.ImageViewerScreen
import com.filyar.app.ui.ProgressDialog
import com.filyar.app.ui.ApkInstallDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.filyar.app.ui.PlayerScreen
import com.filyar.app.ui.ZipPreviewDialog
import com.filyar.app.ui.UiEvent
import com.filyar.app.ui.theme.FilyarTheme
import com.filyar.app.ui.theme.isDarkTheme

private sealed interface Screen {
    data object Browser : Screen
    data class Editor(val file: RemoteFile) : Screen
    data class Viewer(val file: RemoteFile) : Screen
    data class Player(val file: RemoteFile) : Screen
    data object Settings : Screen
    data object Clean : Screen
    data object Media : Screen
    data object Listing : Screen
    data object Apps : Screen
}

class MainActivity : ComponentActivity() {
    private val vm: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vp by vm.viewPrefs.collectAsStateWithLifecycle()
            val styles by vm.folderStyles.collectAsStateWithLifecycle()
            // The bars follow the app's own theme, not the phone's, so clock and battery icons stay readable.
            val dark = isDarkTheme(vp.theme)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            FilyarTheme(vp.theme, vp.dynamicColor, vp.accent, vp.bgColor) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalViewPrefs provides vp,
                    LocalFolderStyles provides styles,
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppRoot(vm)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }
}

@Composable
private fun AppRoot(vm: BrowserViewModel) {
    var base by remember { mutableStateOf<Screen>(Screen.Browser) }
    val clean by vm.clean.collectAsStateWithLifecycle()
    val media by vm.media.collectAsStateWithLifecycle()
    val listing by vm.listing.collectAsStateWithLifecycle()
    val appsOpen by vm.appsOpen.collectAsStateWithLifecycle()
    val screen: Screen = when {
        base == Screen.Browser && listing != null -> Screen.Listing
        base == Screen.Browser && media.kind != null -> Screen.Media
        base == Screen.Browser && clean.mode != null -> Screen.Clean
        base == Screen.Browser && appsOpen -> Screen.Apps
        else -> base
    }
    val snackbar = remember { SnackbarHostState() }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect {
            when (it) {
                is UiEvent.Edit -> base = Screen.Editor(it.file)
                is UiEvent.ViewImage -> base = Screen.Viewer(it.file)
                is UiEvent.Play -> base = Screen.Player(it.file)
                is UiEvent.Launch -> try {
                    context.startActivity(it.intent)
                } catch (_: ActivityNotFoundException) {
                    vm.toast("برنامه‌ای برای باز کردن این فایل پیدا نشد")
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        vm.messages.collect {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it)
        }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState == Screen.Browser) {
                // Coming back home: a plain, quick crossfade with no sliding.
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            } else {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 14 }) togetherWith fadeOut(tween(140))
            }
        },
        label = "screen",
    ) { s ->
        when (s) {
            Screen.Browser -> BrowserScreen(vm, snackbar) { base = Screen.Settings }
            Screen.Clean -> CleanScreen(vm, snackbar)
            Screen.Media -> MediaFoldersScreen(vm, snackbar)
            Screen.Listing -> FileListScreen(vm, snackbar)
            Screen.Apps -> AppsScreen(vm, snackbar)
            Screen.Settings -> SettingsScreen(vm, snackbar) { base = Screen.Browser }
            is Screen.Editor -> EditorScreen(s.file, vm, snackbar) { base = Screen.Browser }
            is Screen.Viewer -> ImageViewerScreen(s.file, vm) { base = Screen.Browser }
            is Screen.Player -> PlayerScreen(s.file) { base = Screen.Browser }
        }
    }

    val apkPrompt by vm.apkPrompt.collectAsStateWithLifecycle()
    val zipPrompt by vm.zipPrompt.collectAsStateWithLifecycle()
    apkPrompt?.let { ApkInstallDialog(it, vm) }
    zipPrompt?.let { ZipPreviewDialog(it, vm) }

    progress?.let { ProgressDialog(it, onCancel = vm::cancelOp) }
}
