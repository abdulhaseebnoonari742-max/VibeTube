package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlockedUserEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CommentEntity
import com.example.data.model.DirectConversationEntity
import com.example.data.model.DirectMessageEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserInteractionEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VibeTubeDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserDirect(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdDirect(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id != :currentUserId AND id NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)")
    fun getAllCreators(currentUserId: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUserFlag()

    @Query("UPDATE users SET isCurrentUser = 1 WHERE id = :userId")
    suspend fun setCurrentUser(userId: String)

    // --- Videos ---
    @Query("""
        SELECT * FROM videos 
        WHERE isReported = 0 
          AND creatorId NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)
        ORDER BY createdAt DESC
    """)
    fun getAllVideos(currentUserId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isReported = 0")
    suspend fun getAllVideosDirect(): List<VideoEntity>

    @Query("""
        SELECT * FROM videos 
        WHERE isShort = 1 
          AND isReported = 0
          AND creatorId NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)
        ORDER BY createdAt DESC
    """)
    fun getShorts(currentUserId: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE isShort = 0 
          AND isReported = 0
          AND creatorId NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)
        ORDER BY createdAt DESC
    """)
    fun getLongVideos(currentUserId: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE isReported = 0 
          AND creatorId IN (SELECT targetId FROM interactions WHERE userId = :currentUserId AND type = 'FOLLOW')
          AND creatorId NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)
        ORDER BY createdAt DESC
    """)
    fun getFollowingFeed(currentUserId: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE isReported = 0 
          AND category = :category
          AND creatorId NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)
        ORDER BY viewsCount DESC
    """)
    fun getVideosByCategory(currentUserId: String, category: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE isReported = 0 
          AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR hashtags LIKE '%' || :query || '%' OR creatorUsername LIKE '%' || :query || '%')
          AND creatorId NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :currentUserId)
        ORDER BY viewsCount DESC
    """)
    fun searchVideos(currentUserId: String, query: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId")
    fun getVideoById(videoId: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoByIdDirect(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE creatorId = :creatorId AND isReported = 0 ORDER BY createdAt DESC")
    fun getVideosByCreator(creatorId: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE id IN (SELECT targetId FROM interactions WHERE userId = :userId AND type = 'SAVE')
        ORDER BY createdAt DESC
    """)
    fun getSavedVideos(userId: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideo(videoId: String)

    @Query("UPDATE videos SET isReported = 1 WHERE id = :videoId")
    suspend fun reportVideo(videoId: String)

    @Query("UPDATE videos SET viewsCount = viewsCount + 1 WHERE id = :videoId")
    suspend fun incrementViews(videoId: String)

    // --- Interactions (Like, Save, Follow) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: UserInteractionEntity)

    @Query("DELETE FROM interactions WHERE id = :id")
    suspend fun deleteInteraction(id: String)

    @Query("SELECT * FROM interactions WHERE userId = :userId AND targetId = :targetId AND type = :type LIMIT 1")
    fun getInteraction(userId: String, targetId: String, type: String): Flow<UserInteractionEntity?>

    @Query("SELECT * FROM interactions WHERE userId = :userId AND targetId = :targetId AND type = :type LIMIT 1")
    suspend fun getInteractionDirect(userId: String, targetId: String, type: String): UserInteractionEntity?

    @Query("SELECT * FROM interactions WHERE userId = :userId AND type = :type")
    fun getUserInteractions(userId: String, type: String): Flow<List<UserInteractionEntity>>

    // --- Comments ---
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY timestamp DESC")
    fun getCommentsForVideo(videoId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadNotificationsCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    // --- Chat Messages (AI) ---
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getChatMessages(userId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatHistory(userId: String)

    // --- Blocked Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockUser(blockedUser: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE userId = :userId AND blockedUserId = :blockedUserId")
    suspend fun unblockUser(userId: String, blockedUserId: String)

    @Query("SELECT * FROM blocked_users WHERE userId = :userId")
    fun getBlockedUsers(userId: String): Flow<List<BlockedUserEntity>>

    // --- Friends & Users Search ---
    @Query("""
        SELECT * FROM users 
        WHERE id != :userId 
          AND (username LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%')
          AND id NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :userId)
    """)
    fun searchAllUsers(userId: String, query: String): Flow<List<UserEntity>>

    @Query("""
        SELECT * FROM users 
        WHERE id IN (SELECT targetId FROM interactions WHERE userId = :userId AND type = 'FOLLOW')
          AND id NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :userId)
    """)
    fun getFollowedUsers(userId: String): Flow<List<UserEntity>>

    @Query("""
        SELECT * FROM users 
        WHERE id != :userId 
          AND id NOT IN (SELECT targetId FROM interactions WHERE userId = :userId AND type = 'FOLLOW')
          AND id NOT IN (SELECT blockedUserId FROM blocked_users WHERE userId = :userId)
        LIMIT 10
    """)
    fun getSuggestedUsers(userId: String): Flow<List<UserEntity>>

    // --- Direct Conversations & Groups ---
    @Query("""
        SELECT * FROM conversations 
        WHERE participantIds LIKE '%' || :userId || '%'
        ORDER BY lastMessageTimestamp DESC
    """)
    fun getConversations(userId: String): Flow<List<DirectConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationById(conversationId: String): Flow<DirectConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationByIdDirect(conversationId: String): DirectConversationEntity?

    @Query("""
        SELECT * FROM conversations 
        WHERE isGroup = 0 
          AND participantIds LIKE '%' || :userId1 || '%' 
          AND participantIds LIKE '%' || :userId2 || '%' 
        LIMIT 1
    """)
    suspend fun findDirectConversation(userId1: String, userId2: String): DirectConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: DirectConversationEntity)

    @Update
    suspend fun updateConversation(conversation: DirectConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    // --- Direct Messages ---
    @Query("SELECT * FROM direct_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getDirectMessages(conversationId: String): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessageEntity)

    @Update
    suspend fun updateDirectMessage(message: DirectMessageEntity)

    @Query("UPDATE direct_messages SET isRead = 1 WHERE conversationId = :conversationId AND senderId != :userId")
    suspend fun markMessagesAsRead(conversationId: String, userId: String)

    @Query("UPDATE direct_messages SET isDeleted = 1, text = 'This message was deleted' WHERE id = :messageId")
    suspend fun markMessageDeleted(messageId: String)

    @Query("DELETE FROM direct_messages WHERE id = :messageId")
    suspend fun deleteDirectMessage(messageId: String)

    @Query("""
        SELECT COUNT(*) FROM direct_messages 
        WHERE senderId != :userId 
          AND isRead = 0 
          AND isDeleted = 0 
          AND conversationId IN (SELECT id FROM conversations WHERE participantIds LIKE '%' || :userId || '%')
    """)
    fun getTotalUnreadMessagesCount(userId: String): Flow<Int>

    // --- Voice & Video Calls ---
    @Query("""
        SELECT * FROM call_logs 
        WHERE callerId = :userId OR receiverId = :userId 
        ORDER BY timestamp DESC
    """)
    fun getCallLogs(userId: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id = :callLogId")
    suspend fun deleteCallLog(callLogId: String)

    @Query("DELETE FROM call_logs WHERE callerId = :userId OR receiverId = :userId")
    suspend fun clearCallLogs(userId: String)
}
