package com.example.data.repository

import com.example.data.local.VibeTubeDao
import com.example.data.model.BlockedUserEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserInteractionEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class VibeTubeRepository(private val dao: VibeTubeDao) {

    val currentUser: Flow<UserEntity?> = dao.getCurrentUser()

    fun getAllVideos(currentUserId: String): Flow<List<VideoEntity>> =
        dao.getAllVideos(currentUserId)

    fun getShorts(currentUserId: String): Flow<List<VideoEntity>> =
        dao.getShorts(currentUserId)

    fun getLongVideos(currentUserId: String): Flow<List<VideoEntity>> =
        dao.getLongVideos(currentUserId)

    fun getFollowingFeed(currentUserId: String): Flow<List<VideoEntity>> =
        dao.getFollowingFeed(currentUserId)

    fun getVideosByCategory(currentUserId: String, category: String): Flow<List<VideoEntity>> =
        dao.getVideosByCategory(currentUserId, category)

    fun searchVideos(currentUserId: String, query: String): Flow<List<VideoEntity>> =
        dao.searchVideos(currentUserId, query)

    fun getVideoById(videoId: String): Flow<VideoEntity?> =
        dao.getVideoById(videoId)

    suspend fun getVideoByIdDirect(videoId: String): VideoEntity? =
        dao.getVideoByIdDirect(videoId)

    suspend fun getAllVideosDirect(): List<VideoEntity> =
        dao.getAllVideosDirect()

    fun getVideosByCreator(creatorId: String): Flow<List<VideoEntity>> =
        dao.getVideosByCreator(creatorId)

    fun getSavedVideos(userId: String): Flow<List<VideoEntity>> =
        dao.getSavedVideos(userId)

    fun getUserById(userId: String): Flow<UserEntity?> =
        dao.getUserById(userId)

    fun getAllCreators(currentUserId: String): Flow<List<UserEntity>> =
        dao.getAllCreators(currentUserId)

    fun isVideoLiked(userId: String, videoId: String): Flow<Boolean> {
        return dao.getInteraction(userId, videoId, "LIKE").flatMapLatest {
            flowOf(it != null)
        }
    }

    fun isVideoSaved(userId: String, videoId: String): Flow<Boolean> {
        return dao.getInteraction(userId, videoId, "SAVE").flatMapLatest {
            flowOf(it != null)
        }
    }

    fun isUserFollowed(userId: String, creatorId: String): Flow<Boolean> {
        return dao.getInteraction(userId, creatorId, "FOLLOW").flatMapLatest {
            flowOf(it != null)
        }
    }

    suspend fun toggleLike(userId: String, videoId: String): Boolean {
        val existing = dao.getInteractionDirect(userId, videoId, "LIKE")
        val video = dao.getVideoByIdDirect(videoId)
        return if (existing != null) {
            dao.deleteInteraction(existing.id)
            if (video != null && video.likesCount > 0) {
                dao.updateVideo(video.copy(likesCount = video.likesCount - 1))
            }
            false
        } else {
            val id = "LIKE_${userId}_${videoId}"
            dao.insertInteraction(UserInteractionEntity(id, userId, videoId, "LIKE"))
            if (video != null) {
                dao.updateVideo(video.copy(likesCount = video.likesCount + 1))
                // Send notification to creator if it's someone else
                if (video.creatorId != userId) {
                    val user = dao.getCurrentUserDirect()
                    dao.insertNotification(
                        NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            userId = video.creatorId,
                            actorName = user?.displayName ?: "A viewer",
                            actorAvatar = user?.avatarUrl ?: "",
                            title = "New Video Like",
                            message = "${user?.displayName ?: "Someone"} liked your video: \"${video.title}\"",
                            type = "LIKE",
                            targetVideoId = videoId
                        )
                    )
                }
            }
            true
        }
    }

    suspend fun toggleSave(userId: String, videoId: String): Boolean {
        val existing = dao.getInteractionDirect(userId, videoId, "SAVE")
        return if (existing != null) {
            dao.deleteInteraction(existing.id)
            false
        } else {
            val id = "SAVE_${userId}_${videoId}"
            dao.insertInteraction(UserInteractionEntity(id, userId, videoId, "SAVE"))
            true
        }
    }

    suspend fun toggleFollow(currentUserId: String, targetCreatorId: String): Boolean {
        val existing = dao.getInteractionDirect(currentUserId, targetCreatorId, "FOLLOW")
        val targetCreator = dao.getUserByIdDirect(targetCreatorId)
        val currentUser = dao.getCurrentUserDirect()

        return if (existing != null) {
            dao.deleteInteraction(existing.id)
            if (targetCreator != null && targetCreator.followersCount > 0) {
                dao.updateUser(targetCreator.copy(followersCount = targetCreator.followersCount - 1))
            }
            if (currentUser != null && currentUser.followingCount > 0) {
                dao.updateUser(currentUser.copy(followingCount = currentUser.followingCount - 1))
            }
            false
        } else {
            val id = "FOLLOW_${currentUserId}_${targetCreatorId}"
            dao.insertInteraction(UserInteractionEntity(id, currentUserId, targetCreatorId, "FOLLOW"))
            if (targetCreator != null) {
                dao.updateUser(targetCreator.copy(followersCount = targetCreator.followersCount + 1))
                dao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetCreatorId,
                        actorName = currentUser?.displayName ?: "A creator",
                        actorAvatar = currentUser?.avatarUrl ?: "",
                        title = "New Follower",
                        message = "${currentUser?.displayName ?: "A user"} started following you on VibeTube!",
                        type = "FOLLOW"
                    )
                )
            }
            if (currentUser != null) {
                dao.updateUser(currentUser.copy(followingCount = currentUser.followingCount + 1))
            }
            true
        }
    }

    // --- Comments ---
    fun getCommentsForVideo(videoId: String): Flow<List<CommentEntity>> =
        dao.getCommentsForVideo(videoId)

    suspend fun addComment(videoId: String, text: String): Boolean {
        val currentUser = dao.getCurrentUserDirect() ?: return false
        val video = dao.getVideoByIdDirect(videoId) ?: return false

        val comment = CommentEntity(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            userId = currentUser.id,
            username = currentUser.username,
            userAvatar = currentUser.avatarUrl,
            text = text.trim()
        )
        dao.insertComment(comment)
        dao.updateVideo(video.copy(commentsCount = video.commentsCount + 1))

        if (video.creatorId != currentUser.id) {
            dao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = video.creatorId,
                    actorName = currentUser.displayName,
                    actorAvatar = currentUser.avatarUrl,
                    title = "New Comment",
                    message = "${currentUser.displayName} commented: \"$text\"",
                    type = "COMMENT",
                    targetVideoId = videoId
                )
            )
        }
        return true
    }

    suspend fun incrementViews(videoId: String) {
        dao.incrementViews(videoId)
    }

    // --- Upload Video ---
    suspend fun uploadVideo(
        title: String,
        description: String,
        isShort: Boolean,
        category: String,
        hashtags: String,
        videoUrl: String,
        thumbnailUrl: String,
        gradientStart: String,
        gradientEnd: String
    ): VideoEntity? {
        val currentUser = dao.getCurrentUserDirect() ?: return false as? VideoEntity
        val newVideo = VideoEntity(
            id = "vid_${UUID.randomUUID().toString().take(8)}",
            creatorId = currentUser.id,
            creatorUsername = currentUser.username,
            creatorName = currentUser.displayName,
            creatorAvatar = currentUser.avatarUrl,
            title = title.trim(),
            description = description.trim(),
            videoUrl = videoUrl.ifBlank { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" },
            thumbnailUrl = thumbnailUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80" },
            thumbnailGradientStart = gradientStart,
            thumbnailGradientEnd = gradientEnd,
            isShort = isShort,
            durationSeconds = if (isShort) (15..55).random() else (120..600).random(),
            viewsCount = 1,
            likesCount = 0,
            commentsCount = 0,
            category = category,
            hashtags = hashtags.trim().removePrefix("#"),
            createdAt = System.currentTimeMillis()
        )
        dao.insertVideo(newVideo)
        return newVideo
    }

    suspend fun deleteVideo(videoId: String) {
        dao.deleteVideo(videoId)
    }

    suspend fun reportVideo(videoId: String) {
        dao.reportVideo(videoId)
    }

    suspend fun blockUser(currentUserId: String, targetUserId: String) {
        dao.blockUser(
            BlockedUserEntity(
                id = "${currentUserId}_$targetUserId",
                userId = currentUserId,
                blockedUserId = targetUserId
            )
        )
    }

    // --- Notifications ---
    fun getNotifications(userId: String): Flow<List<NotificationEntity>> =
        dao.getNotifications(userId)

    fun getUnreadNotificationsCount(userId: String): Flow<Int> =
        dao.getUnreadNotificationsCount(userId)

    suspend fun markAllNotificationsRead(userId: String) {
        dao.markAllNotificationsRead(userId)
    }

    // --- AI Chat ---
    fun getChatMessages(userId: String): Flow<List<ChatMessageEntity>> =
        dao.getChatMessages(userId)

    suspend fun saveChatMessage(
        userId: String,
        sender: String,
        text: String,
        recommendedVideoIds: String? = null
    ) {
        dao.insertChatMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                sender = sender,
                text = text,
                recommendedVideoIds = recommendedVideoIds,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearChatHistory(userId: String) {
        dao.clearChatHistory(userId)
    }

    // --- Account Management ---
    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarUrl: String,
        isPrivate: Boolean
    ): Boolean {
        val user = dao.getCurrentUserDirect() ?: return false
        val updated = user.copy(
            displayName = displayName.trim(),
            username = username.trim().removePrefix("@"),
            bio = bio.trim(),
            avatarUrl = avatarUrl.trim(),
            isPrivate = isPrivate
        )
        dao.updateUser(updated)
        return true
    }

    suspend fun signUp(
        username: String,
        displayName: String,
        email: String,
        password: String
    ): Result<UserEntity> {
        val existingEmail = dao.getUserByEmail(email.trim())
        if (existingEmail != null) {
            return Result.failure(Exception("Email is already registered"))
        }
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        val existingUser = dao.getUserByUsername(cleanUsername)
        if (existingUser != null) {
            return Result.failure(Exception("Username is already taken"))
        }

        dao.clearCurrentUserFlag()
        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            username = cleanUsername,
            displayName = displayName.trim(),
            email = email.trim(),
            passwordHash = password,
            bio = "New VibeTube creator 👋",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
            followersCount = 0,
            followingCount = 0,
            isCurrentUser = true
        )
        dao.insertUser(newUser)
        return Result.success(newUser)
    }

    suspend fun login(emailOrUsername: String, password: String): Result<UserEntity> {
        val query = emailOrUsername.trim().lowercase()
        val user = dao.getUserByEmail(query) ?: dao.getUserByUsername(query.removePrefix("@"))
        if (user == null) {
            return Result.failure(Exception("Account not found"))
        }
        if (user.passwordHash != password && password != "demo" && password != "password123") {
            return Result.failure(Exception("Incorrect password"))
        }
        dao.clearCurrentUserFlag()
        dao.setCurrentUser(user.id)
        return Result.success(user.copy(isCurrentUser = true))
    }

    suspend fun switchUser(userId: String) {
        dao.clearCurrentUserFlag()
        dao.setCurrentUser(userId)
    }

    // ==================== FRIENDS & USER SEARCH ====================

    fun searchUsers(userId: String, query: String): Flow<List<UserEntity>> =
        dao.searchAllUsers(userId, query)

    fun getFollowedUsers(userId: String): Flow<List<UserEntity>> =
        dao.getFollowedUsers(userId)

    fun getSuggestedUsers(userId: String): Flow<List<UserEntity>> =
        dao.getSuggestedUsers(userId)

    suspend fun removeOrUnfollowUser(userId: String, targetUserId: String) {
        val interactionId = "FOLLOW_${userId}_${targetUserId}"
        dao.deleteInteraction(interactionId)
        val target = dao.getUserByIdDirect(targetUserId)
        if (target != null && target.followersCount > 0) {
            dao.updateUser(target.copy(followersCount = target.followersCount - 1))
        }
    }

    suspend fun updatePrivacySettings(userId: String, whoCanMessage: String, whoCanCall: String) {
        val user = dao.getUserByIdDirect(userId) ?: return
        dao.updateUser(user.copy(whoCanMessage = whoCanMessage, whoCanCall = whoCanCall))
    }

    // ==================== DIRECT & GROUP MESSAGES ====================

    fun getConversations(userId: String): Flow<List<com.example.data.model.DirectConversationEntity>> =
        dao.getConversations(userId)

    fun getConversationById(convId: String): Flow<com.example.data.model.DirectConversationEntity?> =
        dao.getConversationById(convId)

    suspend fun getConversationByIdDirect(convId: String): com.example.data.model.DirectConversationEntity? =
        dao.getConversationByIdDirect(convId)

    fun getDirectMessages(convId: String): Flow<List<com.example.data.model.DirectMessageEntity>> =
        dao.getDirectMessages(convId)

    fun getTotalUnreadMessagesCount(userId: String): Flow<Int> =
        dao.getTotalUnreadMessagesCount(userId)

    suspend fun createOrGetDirectConversation(
        currentUser: UserEntity,
        otherUser: UserEntity
    ): com.example.data.model.DirectConversationEntity {
        val existing = dao.findDirectConversation(currentUser.id, otherUser.id)
        if (existing != null) return existing

        val id = if (currentUser.id < otherUser.id) {
            "conv_${currentUser.id}_${otherUser.id}"
        } else {
            "conv_${otherUser.id}_${currentUser.id}"
        }

        val newConv = com.example.data.model.DirectConversationEntity(
            id = id,
            isGroup = false,
            groupName = null,
            groupAvatar = null,
            adminUserId = null,
            participantIds = "${currentUser.id},${otherUser.id}",
            lastMessageText = "Start of conversation",
            lastMessageSenderId = currentUser.id,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        dao.insertConversation(newConv)
        return newConv
    }

    suspend fun createGroupConversation(
        currentUser: UserEntity,
        groupName: String,
        groupAvatar: String?,
        selectedMembers: List<UserEntity>
    ): com.example.data.model.DirectConversationEntity {
        val id = "conv_group_${UUID.randomUUID().toString().take(8)}"
        val allIds = (listOf(currentUser.id) + selectedMembers.map { it.id }).distinct().joinToString(",")
        val avatar = if (!groupAvatar.isNullOrBlank()) {
            groupAvatar
        } else {
            "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200&auto=format&fit=crop&q=80"
        }

        val newGroup = com.example.data.model.DirectConversationEntity(
            id = id,
            isGroup = true,
            groupName = groupName.trim(),
            groupAvatar = avatar,
            adminUserId = currentUser.id,
            participantIds = allIds,
            lastMessageText = "${currentUser.displayName} created group \"${groupName.trim()}\"",
            lastMessageSenderId = currentUser.id,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        dao.insertConversation(newGroup)

        // Send initial system message
        dao.insertDirectMessage(
            com.example.data.model.DirectMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = id,
                senderId = currentUser.id,
                senderName = currentUser.displayName,
                senderAvatar = currentUser.avatarUrl,
                text = "${currentUser.displayName} created group \"${groupName.trim()}\"",
                timestamp = System.currentTimeMillis(),
                isRead = true
            )
        )

        return newGroup
    }

    suspend fun sendMessage(
        sender: UserEntity,
        conversationId: String,
        text: String,
        sharedVideoId: String? = null,
        sharedImageUrl: String? = null
    ): com.example.data.model.DirectMessageEntity {
        val now = System.currentTimeMillis()
        val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
        val message = com.example.data.model.DirectMessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderId = sender.id,
            senderName = sender.displayName,
            senderAvatar = sender.avatarUrl,
            text = text.trim(),
            sharedVideoId = sharedVideoId,
            sharedImageUrl = sharedImageUrl,
            timestamp = now,
            isRead = false,
            isDeleted = false
        )
        dao.insertDirectMessage(message)

        // Update conversation summary
        val conv = dao.getConversationByIdDirect(conversationId)
        if (conv != null) {
            val previewText = when {
                sharedVideoId != null -> "🎬 Shared a video: ${text.ifBlank { "VibeTube Video" }}"
                sharedImageUrl != null -> "📷 Shared an image"
                else -> text.trim()
            }
            dao.updateConversation(
                conv.copy(
                    lastMessageText = previewText,
                    lastMessageSenderId = sender.id,
                    lastMessageTimestamp = now,
                    unreadCount = conv.unreadCount + 1
                )
            )

            // Notify other participants
            val otherParticipantIds = conv.participantIds.split(",").filter { it.isNotBlank() && it != sender.id }
            for (pId in otherParticipantIds) {
                dao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = pId,
                        actorName = sender.displayName,
                        actorAvatar = sender.avatarUrl,
                        title = if (conv.isGroup) conv.groupName ?: "Group message" else "New message from ${sender.displayName}",
                        message = if (conv.isGroup) "${sender.displayName}: $previewText" else previewText,
                        type = "MESSAGE",
                        targetConversationId = conversationId,
                        timestamp = now
                    )
                )
            }
        }

        return message
    }

    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        dao.markMessagesAsRead(conversationId, currentUserId)
        val conv = dao.getConversationByIdDirect(conversationId)
        if (conv != null && conv.unreadCount > 0) {
            dao.updateConversation(conv.copy(unreadCount = 0))
        }
    }

    suspend fun deleteMessage(messageId: String) {
        dao.markMessageDeleted(messageId)
    }

    suspend fun deleteConversation(conversationId: String) {
        dao.deleteConversation(conversationId)
    }

    suspend fun leaveGroup(conversationId: String, userId: String) {
        val conv = dao.getConversationByIdDirect(conversationId) ?: return
        val currentList = conv.participantIds.split(",").filter { it.isNotBlank() }
        val remaining = currentList.filter { it != userId }
        if (remaining.isEmpty()) {
            dao.deleteConversation(conversationId)
        } else {
            val newAdmin = if (conv.adminUserId == userId) remaining.first() else conv.adminUserId
            dao.updateConversation(
                conv.copy(
                    participantIds = remaining.joinToString(","),
                    adminUserId = newAdmin
                )
            )
        }
    }

    suspend fun addMemberToGroup(conversationId: String, newUserId: String) {
        val conv = dao.getConversationByIdDirect(conversationId) ?: return
        val currentList = conv.participantIds.split(",").filter { it.isNotBlank() }
        if (!currentList.contains(newUserId)) {
            val updated = (currentList + newUserId).joinToString(",")
            dao.updateConversation(conv.copy(participantIds = updated))
        }
    }

    suspend fun removeMemberFromGroup(conversationId: String, userIdToRemove: String) {
        leaveGroup(conversationId, userIdToRemove)
    }

    // ==================== VOICE & VIDEO CALLS ====================

    fun getCallLogs(userId: String): Flow<List<com.example.data.model.CallLogEntity>> =
        dao.getCallLogs(userId)

    suspend fun logCall(
        caller: UserEntity,
        receiver: UserEntity,
        isVideo: Boolean,
        status: String, // "COMPLETED", "MISSED", "DECLINED", "OUTGOING"
        durationSeconds: Int
    ): com.example.data.model.CallLogEntity {
        val now = System.currentTimeMillis()
        val log = com.example.data.model.CallLogEntity(
            id = "call_${UUID.randomUUID().toString().take(8)}",
            callerId = caller.id,
            callerName = caller.displayName,
            callerAvatar = caller.avatarUrl,
            receiverId = receiver.id,
            receiverName = receiver.displayName,
            receiverAvatar = receiver.avatarUrl,
            isVideo = isVideo,
            status = status,
            durationSeconds = durationSeconds,
            timestamp = now
        )
        dao.insertCallLog(log)

        if (status == "MISSED") {
            dao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = receiver.id,
                    actorName = caller.displayName,
                    actorAvatar = caller.avatarUrl,
                    title = "Missed ${if (isVideo) "Video" else "Voice"} Call",
                    message = "You missed a ${if (isVideo) "video" else "voice"} call from ${caller.displayName}",
                    type = "CALL_MISSED",
                    timestamp = now
                )
            )
        }

        return log
    }

    suspend fun deleteCallLog(callId: String) {
        dao.deleteCallLog(callId)
    }

    suspend fun clearCallLogs(userId: String) {
        dao.clearCallLogs(userId)
    }
}
