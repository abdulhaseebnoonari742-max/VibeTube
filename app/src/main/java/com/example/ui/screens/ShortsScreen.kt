package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.Screen
import com.example.VibeTubeViewModel
import com.example.data.model.CommentEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.ReportDialog
import com.example.ui.components.ShareDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(viewModel: VibeTubeViewModel) {
    val shorts by viewModel.shorts.collectAsState()
    val likedVideoIds by viewModel.likedVideoIds.collectAsState()
    val savedVideoIds by viewModel.savedVideoIds.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()

    var activeCommentsVideoId by remember { mutableStateOf<String?>(null) }
    var activeShareVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var activeReportVideo by remember { mutableStateOf<VideoEntity?>(null) }

    val commentSheetState = rememberModalBottomSheetState()
    val activeComments by if (activeCommentsVideoId != null) {
        viewModel.getCommentsForVideo(activeCommentsVideoId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<CommentEntity>()) }
    }

    if (shorts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "No Shorts available yet", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.navigateTo(Screen.CREATE) }) {
                    Text("Upload the First Short")
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shorts.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_vertical_feed")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val short = shorts[page]
            val isCurrentPage = (pagerState.currentPage == page)
            val isLiked = likedVideoIds.contains(short.id)
            val isSaved = savedVideoIds.contains(short.id)
            val isFollowed = followedCreatorIds.contains(short.creatorId)

            ShortPageItem(
                short = short,
                isAutoPlaying = isCurrentPage,
                isLiked = isLiked,
                isSaved = isSaved,
                isFollowed = isFollowed,
                onLikeClick = { viewModel.toggleLike(short.id) },
                onCommentClick = { activeCommentsVideoId = short.id },
                onSaveClick = { viewModel.toggleSave(short.id) },
                onShareClick = { activeShareVideo = short },
                onReportClick = { activeReportVideo = short },
                onFollowClick = { viewModel.toggleFollow(short.creatorId) }
            )
        }

        // Top Overlay Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .testTag("shorts_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Shorts",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Box(modifier = Modifier.size(40.dp))
        }
    }

    // Comments Sheet
    if (activeCommentsVideoId != null) {
        CommentsBottomSheet(
            sheetState = commentSheetState,
            comments = activeComments,
            onDismiss = { activeCommentsVideoId = null },
            onAddComment = { text ->
                viewModel.addComment(activeCommentsVideoId!!, text)
            }
        )
    }

    // Share Dialog
    activeShareVideo?.let { v ->
        ShareDialog(
            videoTitle = v.title,
            videoId = v.id,
            onDismiss = { activeShareVideo = null }
        )
    }

    // Report Dialog
    activeReportVideo?.let { v ->
        ReportDialog(
            videoTitle = v.title,
            onDismiss = { activeReportVideo = null },
            onReportConfirmed = { reason ->
                viewModel.reportVideo(v.id, reason)
                activeReportVideo = null
            }
        )
    }
}

@Composable
fun ShortPageItem(
    short: VideoEntity,
    isAutoPlaying: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowed: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Video Player
        VideoPlayerView(
            videoUrl = short.videoUrl,
            thumbnailUrl = short.thumbnailUrl,
            isShort = true,
            autoPlay = isAutoPlaying,
            showControls = false,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f
                    )
                )
        )

        // Right Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Creator Avatar with Follow Button
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = short.creatorAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80" },
                    contentDescription = short.creatorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )

                if (!isFollowed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEC4899))
                            .clickable { onFollowClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Follow",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Like Action
            ShortActionItem(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = formatCount(short.likesCount),
                tint = if (isLiked) Color(0xFFF43F5E) else Color.White,
                onClick = onLikeClick,
                testTag = "short_like_button_${short.id}"
            )

            // Comment Action
            ShortActionItem(
                icon = Icons.Default.ChatBubble,
                label = formatCount(short.commentsCount),
                tint = Color.White,
                onClick = onCommentClick,
                testTag = "short_comment_button_${short.id}"
            )

            // Save Action
            ShortActionItem(
                icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = "Save",
                tint = if (isSaved) Color(0xFFA78BFA) else Color.White,
                onClick = onSaveClick,
                testTag = "short_save_button_${short.id}"
            )

            // Share Action
            ShortActionItem(
                icon = Icons.Default.Share,
                label = "Share",
                tint = Color.White,
                onClick = onShareClick,
                testTag = "short_share_button_${short.id}"
            )

            // More / Report
            IconButton(
                onClick = onReportClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Report",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            // Rotating Vinyl Sound Disc
            RotatingSoundDisc()
        }

        // Bottom Creator Info & Title Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            // Creator Handle & Follow Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "@${short.creatorUsername}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Surface(
                    onClick = onFollowClick,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFollowed) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("short_follow_toggle")
                ) {
                    Text(
                        text = if (isFollowed) "Following" else "Follow",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = short.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sound Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Sound track",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Original Audio • ${short.creatorName}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ShortActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RotatingSoundDisc() {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .rotate(angle)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF7C3AED),
                        Color.Black
                    )
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
