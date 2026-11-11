package com.filyar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filyar.app.core.faNum

/** A flat list of files (all photos, all tracks, the ringtones…) or of groups (artists, albums). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(vm: BrowserViewModel, snackbar: SnackbarHostState) {
    val current by vm.listing.collectAsStateWithLifecycle()
    val state = current ?: return
    BackHandler { vm.closeListing() }
    val groups = state.groups

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(onClick = vm::closeListing) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                title = {
                    Column {
                        Text(state.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                state.loading -> "در حال خواندن…"
                                groups != null -> "${faNum(groups.size)} مورد"
                                else -> "${faNum(state.files.size)} فایل"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                state.error != null -> StateMessage(Icons.Filled.FolderOff, "خواندن ممکن نشد", state.error)
                groups != null -> LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(groups, key = { it.key }) { g ->
                        GroupRow(g, byArtist = state.byArtist) { vm.openGroup(g) }
                    }
                }
                state.files.isEmpty() && !state.loading -> StateMessage(Icons.Filled.FolderOff, "موردی پیدا نشد", null)
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(state.files, key = { it.path }) { f ->
                        RecentRow(f, vm.backend, vm.rootPath, onOpen = { vm.open(f) }, onFolder = { vm.showListingFolder(f.path) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(g: ListGroup, byArtist: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (byArtist) Icons.Filled.Person else Icons.Filled.Album, null, tint = scheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${faNum(g.count)} آهنگ", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}
