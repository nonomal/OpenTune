package com.arturo254.opentune.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ArtistSongSortType
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.ArtistListItem
import com.arturo254.opentune.ui.component.GridMenu
import com.arturo254.opentune.ui.component.GridMenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    ArtistListItem(
        artist = artist,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (artist.artist.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    HorizontalDivider()

    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        if (artist.songCount > 0) {
            GridMenuItem(
                icon = R.drawable.play,
                title = R.string.play,
            ) {
                coroutineScope.launch {
                    val songs =
                        withContext(Dispatchers.IO) {
                            database
                                .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                .first()
                                .map { it.toMediaItem() }
                        }
                    playerConnection.playQueue(
                        ListQueue(
                            title = artist.artist.name,
                            items = songs,
                        ),
                    )
                }
                onDismiss()
            }
            GridMenuItem(
                icon = R.drawable.shuffle,
                title = R.string.shuffle,
            ) {
                coroutineScope.launch {
                    val songs =
                        withContext(Dispatchers.IO) {
                            database
                                .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                .first()
                                .map { it.toMediaItem() }
                                .shuffled()
                        }
                    playerConnection.playQueue(
                        ListQueue(
                            title = artist.artist.name,
                            items = songs,
                        ),
                    )
                }
                onDismiss()
            }
        }
        if (artist.artist.isYouTubeArtist) {
            GridMenuItem(
                icon = R.drawable.share,
                title = R.string.share,
            ) {
                onDismiss()
                val intent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "https://music.youtube.com/channel/${artist.id}"
                        )
                    }
                context.startActivity(Intent.createChooser(intent, null))
            }
        }
    }
}
