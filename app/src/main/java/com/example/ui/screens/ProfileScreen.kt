package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.Screen
import com.example.VibeTubeViewModel
import com.example.data.model.UserEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.CommunityGuidelinesDialog
import com.example.ui.components.DeleteVideoDialog
import com.example.ui.components.VideoCard
import com.example.ui.components.formatCount

enum class ProfileTab {
    MY_VIDEOS,
    SAVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: VibeTubeViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val userUploads by viewModel.userUploads.collectAsState()
    val userSaved by viewModel.userSavedVideos.collectAsState()
    val creators by viewModel.creatorsList.collectAsState()
    val likedVideoIds by viewModel.likedVideoIds.collectAsState()
    val savedVideoIds by viewModel.savedVideoIds.collectAsState()
    val totalUnreadMessages by viewModel.totalUnreadMessagesCount.collectAsState()

    var selectedTab by remember { mutableStateOf(ProfileTab.MY_VIDEOS) }
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showGuidelinesDialog by remember { mutableStateOf(false) }
    var showSwitchUserDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoEntity?>(null) }

    val user = currentUser
    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { viewModel.navigateTo(Screen.AUTH) }) {
                Text("Sign In or Register")
            }
        }
        return
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Messages / Direct Inbox Button with Badge
                IconButton(
                    onClick = { viewModel.openInbox() },
                    modifier = Modifier.testTag("profile_messages_button")
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

                // Switch Demo Account
                IconButton(
                    onClick = { showSwitchUserDialog = true },
                    modifier = Modifier.testTag("profile_switch_user_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch User",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Guidelines
                IconButton(
                    onClick = { showGuidelinesDialog = true },
                    modifier = Modifier.testTag("profile_guidelines_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Community Guidelines",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Profile Details & Tabs
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_main_content"),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    AsyncImage(
                        model = user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80" },
                        contentDescription = user.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (user.isPrivate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Private Account",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bio
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats row: Followers & Following
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn(number = formatCount(userUploads.size), label = "Uploads")
                        StatColumn(number = formatCount(user.followersCount), label = "Followers")
                        StatColumn(number = formatCount(user.followingCount), label = "Following")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("edit_profile_button"),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.AUTH) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("sign_in_or_switch_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                "Sign In / Register",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Tabs: My Videos vs Saved
            item {
                TabRow(
                    selectedTabIndex = if (selectedTab == ProfileTab.MY_VIDEOS) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                tabPositions[if (selectedTab == ProfileTab.MY_VIDEOS) 0 else 1]
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = (selectedTab == ProfileTab.MY_VIDEOS),
                        onClick = { selectedTab = ProfileTab.MY_VIDEOS },
                        text = {
                            Text("My Videos (${userUploads.size})", fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.testTag("tab_my_videos")
                    )
                    Tab(
                        selected = (selectedTab == ProfileTab.SAVED),
                        onClick = { selectedTab = ProfileTab.SAVED },
                        text = {
                            Text("Saved (${userSaved.size})", fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.testTag("tab_saved_videos")
                    )
                }
            }

            // Videos List
            val currentList = if (selectedTab == ProfileTab.MY_VIDEOS) userUploads else userSaved
            if (currentList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == ProfileTab.MY_VIDEOS)
                                "You haven't uploaded any videos yet."
                            else
                                "No saved videos in your library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(currentList, key = { it.id }) { video ->
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
                        onCreatorClick = {},
                        onLikeClick = { viewModel.toggleLike(video.id) },
                        onCommentClick = { viewModel.openWatchScreen(video) },
                        onSaveClick = { viewModel.toggleSave(video.id) },
                        onShareClick = {},
                        onReportClick = {},
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Edit Profile Bottom Sheet
    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            user = user,
            onDismiss = { showEditProfileSheet = false },
            onSave = { displayName, username, bio, avatarUrl, isPrivate ->
                viewModel.updateProfile(displayName, username, bio, avatarUrl, isPrivate)
                showEditProfileSheet = false
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Guidelines Dialog
    if (showGuidelinesDialog) {
        CommunityGuidelinesDialog(onDismiss = { showGuidelinesDialog = false })
    }

    // Switch Demo User Dialog
    if (showSwitchUserDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchUserDialog = false },
            title = { Text("Switch Demo Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a creator profile to test following, uploads, and interactions:", fontSize = 13.sp)
                    creators.forEach { creator ->
                        Surface(
                            onClick = {
                                viewModel.switchUser(creator.id)
                                showSwitchUserDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (creator.id == user.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = creator.avatarUrl,
                                    contentDescription = creator.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(creator.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("@${creator.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatColumn(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, bio: String, avatarUrl: String, isPrivate: Boolean) -> Unit
) {
    var displayName by remember { mutableStateOf(user.displayName) }
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var avatarUrl by remember { mutableStateOf(user.avatarUrl) }
    var isPrivate by remember { mutableStateOf(user.isPrivate) }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            OutlinedTextField(
                value = avatarUrl,
                onValueChange = { avatarUrl = it },
                label = { Text("Avatar Image URL") },
                modifier = Modifier.fillMaxWidth().testTag("edit_avatar_url_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Privacy Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Private Account", fontWeight = FontWeight.Bold)
                    Text("Only approved followers can view videos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    onSave(displayName, username, bio, avatarUrl, isPrivate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}
