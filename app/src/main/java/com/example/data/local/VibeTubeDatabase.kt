package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        VideoEntity::class,
        CommentEntity::class,
        UserInteractionEntity::class,
        NotificationEntity::class,
        ChatMessageEntity::class,
        BlockedUserEntity::class,
        DirectConversationEntity::class,
        DirectMessageEntity::class,
        CallLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VibeTubeDatabase : RoomDatabase() {

    abstract fun vibeTubeDao(): VibeTubeDao

    companion object {
        @Volatile
        private var INSTANCE: VibeTubeDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): VibeTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VibeTubeDatabase::class.java,
                    "vibetube_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.vibeTubeDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: VibeTubeDao) {
            val now = System.currentTimeMillis()

            // 1. Prepopulate Users
            val me = UserEntity(
                id = "user_me",
                username = "abdul_vibe",
                displayName = "Abdul Noonari",
                email = "abdul@vibetube.com",
                passwordHash = "password123",
                bio = "Digital creator & explorer on VibeTube 🚀 Building next-gen mobile video experiences.",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80",
                followersCount = 128,
                followingCount = 4,
                isCurrentUser = true,
                isPrivate = false
            )

            val creator1 = UserEntity(
                id = "user_maya",
                username = "mayatech",
                displayName = "Maya Lin",
                email = "maya@vibetube.com",
                bio = "Creative technologist & Android designer. High-energy micro-tutorials on AI & Code ✨",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&auto=format&fit=crop&q=80",
                followersCount = 14200,
                followingCount = 180,
                isCurrentUser = false
            )

            val creator2 = UserEntity(
                id = "user_julian",
                username = "julianbites",
                displayName = "Chef Julian",
                email = "julian@vibetube.com",
                bio = "Crispy recipes, street food culture & kitchen alchemy in 60 seconds 🍳",
                avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&auto=format&fit=crop&q=80",
                followersCount = 28500,
                followingCount = 92,
                isCurrentUser = false
            )

            val creator3 = UserEntity(
                id = "user_elena",
                username = "sciencepulse",
                displayName = "Dr. Elena Vance",
                email = "elena@vibetube.com",
                bio = "Astrophysicist decoding the cosmos, black holes, and quantum physics simply 🌌",
                avatarUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200&auto=format&fit=crop&q=80",
                followersCount = 52100,
                followingCount = 45,
                isCurrentUser = false
            )

            val creator4 = UserEntity(
                id = "user_kaito",
                username = "kaitovibes",
                displayName = "Kaito Sound",
                email = "kaito@vibetube.com",
                bio = "Analog synthesizer jams, chill beats & ambient soundscapes for deep focus 🎧",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                followersCount = 19400,
                followingCount = 110,
                isCurrentUser = false
            )

            val creator5 = UserEntity(
                id = "user_zara",
                username = "zarawander",
                displayName = "Zara Travel",
                email = "zara@vibetube.com",
                bio = "Solo backpacker exploring hidden gems, drone vistas & nomadic lifestyle 🏔️",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                followersCount = 34900,
                followingCount = 230,
                isCurrentUser = false
            )

            dao.insertUsers(listOf(me, creator1, creator2, creator3, creator4, creator5))

            // 2. Prepopulate Seed Videos (Shorts and Long Videos)
            val videos = listOf(
                // Short 1 (Tech)
                VideoEntity(
                    id = "video_short_1",
                    creatorId = creator1.id,
                    creatorUsername = creator1.username,
                    creatorName = creator1.displayName,
                    creatorAvatar = creator1.avatarUrl,
                    title = "How Neural Networks Actually 'See' Images in 30 Seconds! 🧠",
                    description = "A visual breakdown of convolutional filters and how AI detects edges, patterns, and faces in real-time. #shorts #ai #tech #learn",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#7C3AED",
                    thumbnailGradientEnd = "#3B82F6",
                    isShort = true,
                    durationSeconds = 32,
                    viewsCount = 48200,
                    likesCount = 3910,
                    commentsCount = 142,
                    category = "Tech",
                    hashtags = "ai,tech,shorts,learn,deeplearning",
                    createdAt = now - 3600000 * 4
                ),
                // Short 2 (Food)
                VideoEntity(
                    id = "video_short_2",
                    creatorId = creator2.id,
                    creatorUsername = creator2.username,
                    creatorName = creator2.displayName,
                    creatorAvatar = creator2.avatarUrl,
                    title = "The Secret to the Crispiest Golden Smash Burger 🍔🔥",
                    description = "High heat cast iron, 80/20 chuck blend, wafer-thin onions, and that irresistible caramelized crust. Try this tonight! #food #shorts #cooking",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#EF4444",
                    thumbnailGradientEnd = "#F59E0B",
                    isShort = true,
                    durationSeconds = 48,
                    viewsCount = 92400,
                    likesCount = 8120,
                    commentsCount = 310,
                    category = "Food",
                    hashtags = "food,shorts,cooking,burger,delicious",
                    createdAt = now - 3600000 * 8
                ),
                // Short 3 (Science)
                VideoEntity(
                    id = "video_short_3",
                    creatorId = creator3.id,
                    creatorUsername = creator3.username,
                    creatorName = creator3.displayName,
                    creatorAvatar = creator3.avatarUrl,
                    title = "What Happens If You Fall Into a Supermassive Black Hole? 🌌",
                    description = "Spaghettification, gravitational time dilation, and the event horizon explained in simple terms! #science #astronomy #blackhole #shorts",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#4F46E5",
                    thumbnailGradientEnd = "#EC4899",
                    isShort = true,
                    durationSeconds = 54,
                    viewsCount = 135000,
                    likesCount = 14200,
                    commentsCount = 850,
                    category = "Science",
                    hashtags = "science,space,blackhole,shorts,physics",
                    createdAt = now - 3600000 * 12
                ),
                // Short 4 (Travel)
                VideoEntity(
                    id = "video_short_4",
                    creatorId = creator5.id,
                    creatorUsername = creator5.username,
                    creatorName = creator5.displayName,
                    creatorAvatar = creator5.avatarUrl,
                    title = "Hidden Alpine Waterfall in the Dolomites You Need to Visit 🏔️",
                    description = "Take the secret path past Val di Funes at sunrise. Pure tranquil magic and crystal mountain water. #travel #nature #shorts #italy",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#059669",
                    thumbnailGradientEnd = "#06B6D4",
                    isShort = true,
                    durationSeconds = 29,
                    viewsCount = 76000,
                    likesCount = 6800,
                    commentsCount = 195,
                    category = "Travel",
                    hashtags = "travel,dolomites,nature,shorts,mountains",
                    createdAt = now - 3600000 * 18
                ),
                // Long Video 1 (Tech)
                VideoEntity(
                    id = "video_long_1",
                    creatorId = creator1.id,
                    creatorUsername = creator1.username,
                    creatorName = creator1.displayName,
                    creatorAvatar = creator1.avatarUrl,
                    title = "Building Modern Jetpack Compose Apps: Complete Architecture Guide",
                    description = "In this full creator session, we dive into Clean MVVM architecture, state hoisting, unidirectional data flow, and modern Room database persistence. Perfect for Android engineers leveling up their skills in 2026.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#6366F1",
                    thumbnailGradientEnd = "#8B5CF6",
                    isShort = false,
                    durationSeconds = 596, // ~10 mins
                    viewsCount = 18450,
                    likesCount = 1620,
                    commentsCount = 88,
                    category = "Tech",
                    hashtags = "android,jetpackcompose,kotlin,coding,software",
                    createdAt = now - 3600000 * 24
                ),
                // Long Video 2 (Science)
                VideoEntity(
                    id = "video_long_2",
                    creatorId = creator3.id,
                    creatorUsername = creator3.username,
                    creatorName = creator3.displayName,
                    creatorAvatar = creator3.avatarUrl,
                    title = "The James Webb Space Telescope: Revealing the Cosmic Dawn",
                    description = "How deep infrared imaging captured the earliest galaxies born just 300 million years after the Big Bang. A deep dive into redshift, gravitational lensing, and cosmic reionization.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#1E1B4B",
                    thumbnailGradientEnd = "#4338CA",
                    isShort = false,
                    durationSeconds = 654,
                    viewsCount = 39800,
                    likesCount = 4210,
                    commentsCount = 230,
                    category = "Science",
                    hashtags = "space,astronomy,jwst,cosmos,science",
                    createdAt = now - 3600000 * 36
                ),
                // Long Video 3 (Music)
                VideoEntity(
                    id = "video_long_3",
                    creatorId = creator4.id,
                    creatorUsername = creator4.username,
                    creatorName = creator4.displayName,
                    creatorAvatar = creator4.avatarUrl,
                    title = "Midnight Synthwave & Cyberpunk Ambient Session (Live Hardware Jam)",
                    description = "Analog patch exploration with Juno-106, Moog Sub 37, and tape delay pedals. Immersive stereo mix designed for late-night coding, focus sessions, and relaxing.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#EC4899",
                    thumbnailGradientEnd = "#8B5CF6",
                    isShort = false,
                    durationSeconds = 734,
                    viewsCount = 54100,
                    likesCount = 5890,
                    commentsCount = 312,
                    category = "Music",
                    hashtags = "synthwave,lofi,music,focus,ambient",
                    createdAt = now - 3600000 * 48
                ),
                // Long Video 4 (Travel)
                VideoEntity(
                    id = "video_long_4",
                    creatorId = creator5.id,
                    creatorUsername = creator5.username,
                    creatorName = creator5.displayName,
                    creatorAvatar = creator5.avatarUrl,
                    title = "Kyoto in Autumn: A Cinematic Journey Through Ancient Shrines & Bamboo Groves",
                    description = "Exploring Gion, Fushimi Inari at twilight, and peaceful temple gardens ablaze in vibrant maple foliage. Shot in 4K with binaural natural audio.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    thumbnailUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&auto=format&fit=crop&q=80",
                    thumbnailGradientStart = "#F59E0B",
                    thumbnailGradientEnd = "#EF4444",
                    isShort = false,
                    durationSeconds = 480,
                    viewsCount = 28700,
                    likesCount = 3100,
                    commentsCount = 145,
                    category = "Travel",
                    hashtags = "japan,kyoto,travel,cinematic,culture",
                    createdAt = now - 3600000 * 60
                )
            )

            dao.insertVideos(videos)

            // 3. Initial Follows for Current User
            dao.insertInteraction(UserInteractionEntity("FOLLOW_user_me_user_maya", me.id, creator1.id, "FOLLOW"))
            dao.insertInteraction(UserInteractionEntity("FOLLOW_user_me_user_elena", me.id, creator3.id, "FOLLOW"))

            // 4. Initial Likes & Saves
            dao.insertInteraction(UserInteractionEntity("LIKE_user_me_video_short_1", me.id, "video_short_1", "LIKE"))
            dao.insertInteraction(UserInteractionEntity("SAVE_user_me_video_long_1", me.id, "video_long_1", "SAVE"))

            // 5. Prepopulate Sample Comments
            val comments = listOf(
                CommentEntity(
                    id = "cmt_1",
                    videoId = "video_short_1",
                    userId = creator3.id,
                    username = creator3.username,
                    userAvatar = creator3.avatarUrl,
                    text = "Fantastic explanation! The visual overlay makes convolution so intuitive.",
                    likesCount = 24,
                    timestamp = now - 3600000 * 2
                ),
                CommentEntity(
                    id = "cmt_2",
                    videoId = "video_short_1",
                    userId = me.id,
                    username = me.username,
                    userAvatar = me.avatarUrl,
                    text = "Love this Maya! Can you do a follow-up on transformer attention next?",
                    likesCount = 12,
                    timestamp = now - 3600000 * 1
                ),
                CommentEntity(
                    id = "cmt_3",
                    videoId = "video_short_3",
                    userId = creator1.id,
                    username = creator1.username,
                    userAvatar = creator1.avatarUrl,
                    text = "The time dilation concept still blows my mind every single time! 🚀",
                    likesCount = 45,
                    timestamp = now - 3600000 * 5
                ),
                CommentEntity(
                    id = "cmt_4",
                    videoId = "video_long_1",
                    userId = me.id,
                    username = me.username,
                    userAvatar = me.avatarUrl,
                    text = "Bookmarked! The section on unidirectional data flow solved my state issue.",
                    likesCount = 8,
                    timestamp = now - 3600000 * 10
                )
            )
            dao.insertComments(comments)

            // 6. Prepopulate Notifications for Current User
            val notifications = listOf(
                NotificationEntity(
                    id = "notif_1",
                    userId = me.id,
                    actorName = creator1.displayName,
                    actorAvatar = creator1.avatarUrl,
                    title = "New Follower",
                    message = "Maya Lin started following you on VibeTube!",
                    type = "FOLLOW",
                    timestamp = now - 1800000,
                    isRead = false
                ),
                NotificationEntity(
                    id = "notif_2",
                    userId = me.id,
                    actorName = creator2.displayName,
                    actorAvatar = creator2.avatarUrl,
                    title = "Liked Your Comment",
                    message = "Chef Julian liked your comment: 'Love this recipe!'",
                    type = "LIKE",
                    targetVideoId = "video_short_2",
                    timestamp = now - 7200000,
                    isRead = false
                ),
                NotificationEntity(
                    id = "notif_3",
                    userId = me.id,
                    actorName = "VibeTube Team",
                    actorAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=80",
                    title = "Welcome to VibeTube!",
                    message = "Discover trending Shorts, watch creator masterclasses, or ask VibeAI for ideas anytime.",
                    type = "SYSTEM",
                    timestamp = now - 86400000,
                    isRead = true
                )
            )
            dao.insertNotifications(notifications)

            // 7. Initial Welcome AI Chat Message
            dao.insertChatMessage(
                ChatMessageEntity(
                    id = "chat_welcome",
                    userId = me.id,
                    sender = "ai",
                    text = "Hey Abdul! 👋 I'm VibeAI, your creative co-pilot on VibeTube.\n\nI can help you find videos from our catalog (try 'Find me beginner science videos'), brainstorm titles and hashtags for your next upload, summarize video topics, or guide you through the app. What are you looking to explore today?",
                    recommendedVideoIds = "video_short_1,video_long_2",
                    timestamp = now - 3600000
                )
            )

            // 8. Mutual Follow: Maya Lin follows Abdul back so they are Friends!
            dao.insertInteraction(UserInteractionEntity("FOLLOW_user_maya_user_me", creator1.id, me.id, "FOLLOW"))
            dao.insertInteraction(UserInteractionEntity("FOLLOW_user_elena_user_me", creator3.id, me.id, "FOLLOW"))

            // 9. Initial Direct Conversations
            val convMaya = DirectConversationEntity(
                id = "conv_user_me_user_maya",
                isGroup = false,
                groupName = null,
                groupAvatar = null,
                adminUserId = null,
                participantIds = "user_me,user_maya",
                lastMessageText = "Check out this AI Neural Network short! 🔥",
                lastMessageSenderId = "user_maya",
                lastMessageTimestamp = now - 900000,
                unreadCount = 1
            )

            val convElena = DirectConversationEntity(
                id = "conv_user_me_user_elena",
                isGroup = false,
                groupName = null,
                groupAvatar = null,
                adminUserId = null,
                participantIds = "user_me,user_elena",
                lastMessageText = "The James Webb paper just went live! 🌌",
                lastMessageSenderId = "user_elena",
                lastMessageTimestamp = now - 7200000,
                unreadCount = 0
            )

            val convGroup = DirectConversationEntity(
                id = "conv_group_creators",
                isGroup = true,
                groupName = "🚀 Android Creators Lounge",
                groupAvatar = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200&auto=format&fit=crop&q=80",
                adminUserId = "user_me",
                participantIds = "user_me,user_maya,user_julian,user_elena",
                lastMessageText = "Welcome to the group chat everyone! 🎉",
                lastMessageSenderId = "user_me",
                lastMessageTimestamp = now - 14400000,
                unreadCount = 0
            )

            dao.insertConversation(convMaya)
            dao.insertConversation(convElena)
            dao.insertConversation(convGroup)

            // 10. Initial Direct Messages
            val messages = listOf(
                DirectMessageEntity(
                    id = "msg_m1",
                    conversationId = convMaya.id,
                    senderId = "user_maya",
                    senderName = "Maya Lin",
                    senderAvatar = creator1.avatarUrl,
                    text = "Hey Abdul! Saw you followed me. Loved your feedback on Compose architecture!",
                    timestamp = now - 3600000 * 2,
                    isRead = true
                ),
                DirectMessageEntity(
                    id = "msg_m2",
                    conversationId = convMaya.id,
                    senderId = me.id,
                    senderName = me.displayName,
                    senderAvatar = me.avatarUrl,
                    text = "Thanks Maya! Your explanation on convolutional filters was incredible 🙌",
                    timestamp = now - 3600000,
                    isRead = true
                ),
                DirectMessageEntity(
                    id = "msg_m3",
                    conversationId = convMaya.id,
                    senderId = "user_maya",
                    senderName = "Maya Lin",
                    senderAvatar = creator1.avatarUrl,
                    text = "Check out this AI Neural Network short! 🔥",
                    sharedVideoId = "video_short_1",
                    timestamp = now - 900000,
                    isRead = false
                ),
                DirectMessageEntity(
                    id = "msg_e1",
                    conversationId = convElena.id,
                    senderId = "user_elena",
                    senderName = "Dr. Elena Vance",
                    senderAvatar = creator3.avatarUrl,
                    text = "The James Webb paper just went live! 🌌",
                    sharedVideoId = "video_long_2",
                    timestamp = now - 7200000,
                    isRead = true
                ),
                DirectMessageEntity(
                    id = "msg_g1",
                    conversationId = convGroup.id,
                    senderId = me.id,
                    senderName = me.displayName,
                    senderAvatar = me.avatarUrl,
                    text = "Welcome to the group chat everyone! 🎉",
                    timestamp = now - 14400000,
                    isRead = true
                ),
                DirectMessageEntity(
                    id = "msg_g2",
                    conversationId = convGroup.id,
                    senderId = "user_julian",
                    senderName = "Chef Julian",
                    senderAvatar = creator2.avatarUrl,
                    text = "Stoked to be here! Cooking video dropping tomorrow 🍔",
                    timestamp = now - 12000000,
                    isRead = true
                )
            )
            for (msg in messages) {
                dao.insertDirectMessage(msg)
            }

            // 11. Initial Call Logs
            val callLogs = listOf(
                CallLogEntity(
                    id = "call_1",
                    callerId = "user_maya",
                    callerName = "Maya Lin",
                    callerAvatar = creator1.avatarUrl,
                    receiverId = me.id,
                    receiverName = me.displayName,
                    receiverAvatar = me.avatarUrl,
                    isVideo = true,
                    status = "COMPLETED",
                    durationSeconds = 184, // 3m 4s
                    timestamp = now - 86400000
                ),
                CallLogEntity(
                    id = "call_2",
                    callerId = "user_elena",
                    callerName = "Dr. Elena Vance",
                    callerAvatar = creator3.avatarUrl,
                    receiverId = me.id,
                    receiverName = me.displayName,
                    receiverAvatar = me.avatarUrl,
                    isVideo = false,
                    status = "MISSED",
                    durationSeconds = 0,
                    timestamp = now - 3600000 * 5
                ),
                CallLogEntity(
                    id = "call_3",
                    callerId = me.id,
                    callerName = me.displayName,
                    callerAvatar = me.avatarUrl,
                    receiverId = "user_maya",
                    receiverName = "Maya Lin",
                    receiverAvatar = creator1.avatarUrl,
                    isVideo = false,
                    status = "OUTGOING",
                    durationSeconds = 72,
                    timestamp = now - 3600000 * 20
                )
            )
            for (call in callLogs) {
                dao.insertCallLog(call)
            }
        }
    }
}
