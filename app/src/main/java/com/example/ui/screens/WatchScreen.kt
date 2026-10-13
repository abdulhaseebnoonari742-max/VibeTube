package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.Screen
import com.example.VibeTubeViewModel
import com.example.data.model.CommentEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.DeleteVideoDialog
import com.example.ui.components.ReportDialog
import com.example.ui.components.ShareDialog
import com.example.ui.components.ShareVideoToChatDialog
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(viewModel: VibeTubeViewModel) {
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val likedVideoIds by viewModel.likedVideoIds.collectAsState()
    val savedVideoIds by viewModel.savedVideoIds.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()

    var isDescExpanded by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showShareToChatDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val commentSheetState = rememberModalBottomSheetState()
    val comments by if (selectedVideo != null) {
        viewModel.getCommentsForVideo(selectedVideo!!.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<CommentEntity>()) }
    }

    val video = selectedVideo
    if (video == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "No video selected", color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.navigateTo(Screen.HOME) }) {
                    Text("Go Home")
                }
            }
        }
        return
    }

    val isLiked = likedVideoIds.contains(video.id)
    val isSaved = savedVideoIds.contains(video.id)
    val isFollowed = followedCreatorIds.contains(video.creatorId)
    val isOwnVideo = (currentUser?.id == video.creatorId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Video Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            VideoPlayerView(
                videoUrl = video.videoUrl,
                thumbnailUrl = video.thumbnailUrl,
                isShort = false,
                autoPlay = true,
                showControls = true,
                modifier = Modifier.fillMaxSize()
            )

            // Back button on player overlay
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("watch_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // Scrollable Video Details & Recommendations
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Video Title
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Stats & Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${formatCount(video.viewsCount)} views",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Category: ${video.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Creator Bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = video.creatorAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80" },
                                    contentDescription = video.creatorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = video.creatorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${video.creatorUsername}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isOwnVideo) {
                                Button(
                                    onClick = { viewModel.toggleFollow(video.creatorId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowed) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("watch_follow_button")
                                ) {
                                    Text(
                                        text = if (isFollowed) "Following" else "Follow",
                                        color = if (isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row (Like, Save, Share, Comments, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button
                        WatchActionButton(
                            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = formatCount(video.likesCount),
                            tint = if (isLiked) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurface,
                            onClick = { viewModel.toggleLike(video.id) },
                            testTag = "watch_like_button"
                        )

                        // Save Button
                        WatchActionButton(
                            icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            label = if (isSaved) "Saved" else "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            onClick = { viewModel.toggleSave(video.id) },
                            testTag = "watch_save_button"
                        )

                        // Share Button
                        WatchActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = { showShareDialog = true },
                            testTag = "watch_share_button"
                        )

                        // Report Button
                        WatchActionButton(
                            icon = Icons.Default.Flag,
                            label = "Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showReportDialog = true },
                            testTag = "watch_report_button"
                        )

                        // Delete button if own video
                        if (isOwnVideo) {
                            WatchActionButton(
                                icon = Icons.Default.Delete,
                                label = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteDialog = true },
                                testTag = "watch_delete_button"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expandable Description Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDescExpanded = !isDescExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Description & Tags",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isDescExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle description"
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = video.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 2,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (video.hashtags.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = video.hashtags.split(",").joinToString(" ") { "#${it.trim()}" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comments Preview Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCommentsSheet = true }
                            .testTag("watch_comments_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Comments (${comments.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "View all",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val topComment = comments.firstOrNull()
                            if (topComment != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = topComment.userAvatar,
                                        contentDescription = topComment.username,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = topComment.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Text(
                                    text = "Add a comment to join the discussion...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Up Next Section Header
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Up Next Videos
            val relatedVideos = allVideos.filter { it.id != video.id }
            items(relatedVideos, key = { it.id }) { related ->
                VideoCard(
                    video = related,
                    isLiked = likedVideoIds.contains(related.id),
                    isSaved = savedVideoIds.contains(related.id),
                    onVideoClick = {
                        if (related.isShort) {
                            viewModel.navigateTo(Screen.SHORTS)
                        } else {
                            viewModel.openWatchScreen(related)
                        }
                    },
                    onCreatorClick = { viewModel.navigateTo(Screen.EXPLORE) },
                    onLikeClick = { viewModel.toggleLike(related.id) },
                    onCommentClick = {
                        viewModel.openWatchScreen(related)
                        showCommentsSheet = true
                    },
                    onSaveClick = { viewModel.toggleSave(related.id) },
                    onShareClick = { showShareDialog = true },
                    onReportClick = { showReportDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Comments Sheet
    if (showCommentsSheet) {
        CommentsBottomSheet(
            sheetState = commentSheetState,
            comments = comments,
            onDismiss = { showCommentsSheet = false },
            onAddComment = { text ->
                viewModel.addComment(video.id, text)
            }
        )
    }

    // Share Dialog
    if (showShareDialog) {
        ShareDialog(
            videoTitle = video.title,
            videoId = video.id,
            onDismiss = { showShareDialog = false },
            onShareToChat = { showShareToChatDialog = true }
        )
    }

    // Share to Chat Dialog
    if (showShareToChatDialog) {
        ShareVideoToChatDialog(
            video = video,
            viewModel = viewModel,
            onDismiss = { showShareToChatDialog = false }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        ReportDialog(
            videoTitle = video.title,
            onDismiss = { showReportDialog = false },
            onReportConfirmed = { reason ->
                viewModel.reportVideo(video.id, reason)
                showReportDialog = false
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog) {
        DeleteVideoDialog(
            videoTitle = video.title,
            onDismiss = { showDeleteDialog = false },
            onDeleteConfirmed = {
                viewModel.deleteVideo(video.id)
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun WatchActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
