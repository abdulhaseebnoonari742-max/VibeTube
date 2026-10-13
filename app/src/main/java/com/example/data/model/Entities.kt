package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val passwordHash: String = "password123",
    val bio: String = "",
    val avatarUrl: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isCurrentUser: Boolean = false,
    val isPrivate: Boolean = false,
    val isOnline: Boolean = true,
    val lastActive: Long = System.currentTimeMillis(),
    val whoCanMessage: String = "EVERYONE", // "EVERYONE", "FRIENDS_ONLY"
    val whoCanCall: String = "EVERYONE"    // "EVERYONE", "FRIENDS_ONLY"
)

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorUsername: String,
    val creatorName: String,
    val creatorAvatar: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val thumbnailGradientStart: String = "#7C3AED",
    val thumbnailGradientEnd: String = "#EC4899",
    val isShort: Boolean = false,
    val durationSeconds: Int = 45,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val category: String = "General",
    val hashtags: String = "", // comma-separated e.g. "tech,coding,vibe"
    val createdAt: Long = System.currentTimeMillis(),
    val isReported: Boolean = false
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val userId: String,
    val username: String,
    val userAvatar: String,
    val text: String,
    val likesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "interactions")
data class UserInteractionEntity(
    @PrimaryKey val id: String, // e.g. "LIKE_${userId}_${targetId}"
    val userId: String,
    val targetId: String, // videoId or creatorId
    val type: String, // "LIKE", "SAVE", "FOLLOW"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String, // recipient
    val actorName: String,
    val actorAvatar: String,
    val title: String,
    val message: String,
    val type: String, // "LIKE", "FOLLOW", "COMMENT", "SYSTEM", "MESSAGE", "CALL_MISSED"
    val targetVideoId: String? = null,
    val targetConversationId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val recommendedVideoIds: String? = null, // comma-separated video IDs
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey val id: String, // "${userId}_${blockedUserId}"
    val userId: String,
    val blockedUserId: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- Direct Messaging & Groups ---

@Entity(tableName = "conversations")
data class DirectConversationEntity(
    @PrimaryKey val id: String,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupAvatar: String? = null,
    val adminUserId: String? = null,
    val participantIds: String, // Comma-separated user IDs, e.g. "user_me,user_maya"
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isMuted: Boolean = false
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val sharedVideoId: String? = null,
    val sharedImageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDeleted: Boolean = false
)

// --- Voice & Video Call History ---

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val callerId: String,
    val callerName: String,
    val callerAvatar: String,
    val receiverId: String,
    val receiverName: String,
    val receiverAvatar: String,
    val isVideo: Boolean, // true = video, false = voice
    val status: String,   // "MISSED", "COMPLETED", "DECLINED", "OUTGOING"
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
