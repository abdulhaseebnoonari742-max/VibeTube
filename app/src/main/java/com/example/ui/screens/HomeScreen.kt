package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.HomeFeedTab
import com.example.Screen
import com.example.VibeTubeViewModel
import com.example.data.model.CommentEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.ReportDialog
import com.example.ui.components.ShareDialog
import com.example.ui.components.ShareVideoToChatDialog
import com.example.ui.components.VideoCard
import com.example.ui.components.formatCount
import com.example.ui.components.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: VibeTubeViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val homeTab by viewModel.homeTab.collectAsState()
    val videos by viewModel.homeFeedVideos.collectAsState()
    val shorts by viewModel.shorts.collectAsState()
    val unreadNotifs by viewModel.unreadNotifsCount.collectAsState()
    val totalUnreadMessages by viewModel.totalUnreadMessagesCount.collectAsState()
    val likedVideoIds by viewModel.likedVideoIds.collectAsState()
    val savedVideoIds by viewModel.savedVideoIds.collectAsState()

    // Dialog & Sheet States
    var activeCommentsVideoId by remember { mutableStateOf<String?>(null) }
    var activeReportVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var activeShareVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var activeShareToChatVideo by remember { mutableStateOf<VideoEntity?>(null) }

    val commentSheetState = rememberModalBottomSheetState()
    val activeComments by if (activeCommentsVideoId != null) {
        viewModel.getCommentsForVideo(activeCommentsVideoId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<CommentEntity>()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title (Elegant Dark Theme: #D0BCFF container with #381E72 circle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable { viewModel.navigateTo(Screen.HOME) }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }

                Text(
                    text = "VibeTube",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Right Action Icons (Search, Notifications, Profile)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Search button
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.EXPLORE) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Messages / Direct Inbox Button with Badge
                IconButton(
                    onClick = { viewModel.openInbox() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .testTag("home_messages_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (totalUnreadMessages > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("$totalUnreadMessages", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Messages",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Notifications Bell with Badge
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .testTag("home_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifs > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("$unreadNotifs", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // User Avatar
                AsyncImage(
                    model = currentUser?.avatarUrl?.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80" },
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { viewModel.navigateTo(Screen.PROFILE) }
                        .testTag("home_profile_avatar")
                )
            }
        }

        // Home Tabs: "For You" and "Following" (Border bottom #49454F, active #D0BCFF)
        TabRow(
            selectedTabIndex = if (homeTab == HomeFeedTab.FOR_YOU) 0 else 1,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        tabPositions[if (homeTab == HomeFeedTab.FOR_YOU) 0 else 1]
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    height = 2.dp
                )
            },
            divider = {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
            }
        ) {
            Tab(
                selected = (homeTab == HomeFeedTab.FOR_YOU),
                onClick = { viewModel.setHomeTab(HomeFeedTab.FOR_YOU) },
                text = {
                    Text(
                        text = "For You",
                        fontWeight = if (homeTab == HomeFeedTab.FOR_YOU) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (homeTab == HomeFeedTab.FOR_YOU) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("tab_for_you")
            )
            Tab(
                selected = (homeTab == HomeFeedTab.FOLLOWING),
                onClick = { viewModel.setHomeTab(HomeFeedTab.FOLLOWING) },
                text = {
                    Text(
                        text = "Following",
                        fontWeight = if (homeTab == HomeFeedTab.FOLLOWING) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (homeTab == HomeFeedTab.FOLLOWING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("tab_following")
            )
        }

        // Feed Video List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_video_feed"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ask Vibe Assistant Banner (Directly styled from Elegant Dark design)
            item {
                Surface(
                    onClick = { viewModel.navigateTo(Screen.AI) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("home_ask_ai_banner"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask Vibe Assistant",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "\"Find science videos for beginners\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Shorts Shelf Carousel at top of "For You"
            if (homeTab == HomeFeedTab.FOR_YOU && shorts.isNotEmpty()) {
                item {
                    ShortsShelf(
                        shorts = shorts,
                        onShortClick = { short ->
                            viewModel.navigateTo(Screen.SHORTS)
                        },
                        onSeeAllClick = {
                            viewModel.navigateTo(Screen.SHORTS)
                        }
                    )
                }
            }

            if (videos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (homeTab == HomeFeedTab.FOLLOWING)
                                    "No videos from followed creators yet."
                                else
                                    "No videos found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Follow creators or explore trending content!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { viewModel.navigateTo(Screen.EXPLORE) }
                            )
                        }
                    }
                }
            } else {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        isLiked = likedVideoIds.contains(video.id),
                        isSaved = savedVideoIds.contains(video.id),
                        onVideoClick = {
                            if (video.isShort) {
                                viewModel.navigateTo(Screen.SHORTS)
                            } else {
                                viewModel.openWatchScreen(video)
                            }
                        },
                        onCreatorClick = {
                            // Can open creator profile
                            viewModel.navigateTo(Screen.EXPLORE)
                        },
                        onLikeClick = { viewModel.toggleLike(video.id) },
                        onCommentClick = { activeCommentsVideoId = video.id },
                        onSaveClick = { viewModel.toggleSave(video.id) },
                        onShareClick = { activeShareVideo = video },
                        onReportClick = { activeReportVideo = video },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
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
            onDismiss = { activeShareVideo = null },
            onShareToChat = { activeShareToChatVideo = v }
        )
    }

    // Share Directly to Chat Dialog
    activeShareToChatVideo?.let { v ->
        ShareVideoToChatDialog(
            video = v,
            viewModel = viewModel,
            onDismiss = { activeShareToChatVideo = null }
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
fun ShortsShelf(
    shorts: List<VideoEntity>,
    onShortClick: (VideoEntity) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "VibeShorts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "SEE ALL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .testTag("shorts_shelf_see_all")
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shorts, key = { it.id }) { short ->
                ShortPreviewCard(short = short, onClick = { onShortClick(short) })
            }
        }
    }
}

@Composable
fun ShortPreviewCard(
    short: VideoEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(130.dp)
            .height(210.dp)
            .clickable { onClick() }
            .testTag("short_shelf_item_${short.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient shade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 140f
                        )
                    )
            )

            // Likes / Views pill in bottom left: bg-black/40 px-2 py-1 rounded-full text-[10px]
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "${formatCount(short.likesCount)} likes",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Title
            Text(
                text = short.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = 32.dp)
            )
        }
    }
}
