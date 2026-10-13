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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.UserEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.BlockUserDialog
import com.example.ui.components.ReportDialog
import com.example.ui.components.ShareDialog
import com.example.ui.components.VideoCard
import com.example.ui.components.formatCount

@Composable
fun ExploreScreen(viewModel: VibeTubeViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val exploreCategory by viewModel.exploreCategory.collectAsState()
    val videos by viewModel.exploreVideos.collectAsState()
    val creators by viewModel.creatorsList.collectAsState()
    val likedVideoIds by viewModel.likedVideoIds.collectAsState()
    val savedVideoIds by viewModel.savedVideoIds.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()

    var activeShareVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var activeReportVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var activeBlockCreator by remember { mutableStateOf<UserEntity?>(null) }

    val categories = listOf("All", "Tech", "Science", "Food", "Music", "Travel")
    val trendingTags = listOf("quantum", "compose", "smashburger", "synthwave", "dolomites", "astronomy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar Top Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search videos, tags, or creators...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = (exploreCategory == category),
                        onClick = { viewModel.setExploreCategory(category) },
                        label = { Text(category, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // Main Explore Content List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_results_list"),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Featured Creators Spotlight
            if (creators.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Trending",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Featured Creators",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(creators, key = { it.id }) { creator ->
                                val isFollowed = followedCreatorIds.contains(creator.id)
                                CreatorSpotlightCard(
                                    creator = creator,
                                    isFollowed = isFollowed,
                                    onFollowClick = { viewModel.toggleFollow(creator.id) },
                                    onCardClick = {
                                        viewModel.setSearchQuery(creator.username)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Trending Tags Chips (When no search query)
            if (searchQuery.isBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Popular Hashtags",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(trendingTags) { tag ->
                                Surface(
                                    onClick = { viewModel.setSearchQuery(tag) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Header if active
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Results for \"$searchQuery\" (${videos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Video Cards Results
            if (videos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos match your search.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            viewModel.setSearchQuery(video.creatorUsername)
                        },
                        onLikeClick = { viewModel.toggleLike(video.id) },
                        onCommentClick = {
                            viewModel.openWatchScreen(video)
                        },
                        onSaveClick = { viewModel.toggleSave(video.id) },
                        onShareClick = { activeShareVideo = video },
                        onReportClick = { activeReportVideo = video },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Dialogs
    activeShareVideo?.let { v ->
        ShareDialog(
            videoTitle = v.title,
            videoId = v.id,
            onDismiss = { activeShareVideo = null }
        )
    }

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

    activeBlockCreator?.let { c ->
        BlockUserDialog(
            username = c.username,
            onDismiss = { activeBlockCreator = null },
            onBlockConfirmed = {
                viewModel.blockUser(c.id)
                activeBlockCreator = null
            }
        )
    }
}

@Composable
fun CreatorSpotlightCard(
    creator: UserEntity,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = creator.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80" },
                contentDescription = creator.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = creator.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "@${creator.username}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Text(
                text = "${formatCount(creator.followersCount)} followers",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onFollowClick,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFollowed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowed) MaterialTheme.colorScheme.onSurface else Color.White
                )
            ) {
                Text(
                    text = if (isFollowed) "Following" else "Follow",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
