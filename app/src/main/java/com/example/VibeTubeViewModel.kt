package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiClient
import com.example.data.local.VibeTubeDatabase
import com.example.data.model.CallLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CommentEntity
import com.example.data.model.DirectConversationEntity
import com.example.data.model.DirectMessageEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.UserEntity
import com.example.data.model.VideoEntity
import com.example.data.repository.VibeTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class Screen {
    HOME,
    SHORTS,
    EXPLORE,
    CREATE,
    AI,
    PROFILE,
    WATCH,
    NOTIFICATIONS,
    AUTH,
    MESSAGES,
    CHAT_DETAIL,
    ACTIVE_CALL
}

data class ActiveCallState(
    val callId: String,
    val otherUser: UserEntity,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isFrontCamera: Boolean = true,
    val durationSeconds: Int = 0
)

enum class HomeFeedTab {
    FOR_YOU,
    FOLLOWING
}

class VibeTubeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VibeTubeDatabase.getDatabase(application, viewModelScope)
    private val repository = VibeTubeRepository(database.vibeTubeDao())
    private val geminiClient = GeminiClient()

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Home feed tab selection
    private val _homeTab = MutableStateFlow(HomeFeedTab.FOR_YOU)
    val homeTab: StateFlow<HomeFeedTab> = _homeTab.asStateFlow()

    // Current User
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active User ID helper
    private val currentUserId: String
        get() = currentUser.value?.id ?: "user_me"

    // Feeds
    @OptIn(ExperimentalCoroutinesApi::class)
    val allVideos: StateFlow<List<VideoEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getAllVideos(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val followingVideos: StateFlow<List<VideoEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getFollowingFeed(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeFeedVideos: StateFlow<List<VideoEntity>> = combine(allVideos, followingVideos, homeTab) { all, following, tab ->
        if (tab == HomeFeedTab.FOLLOWING) following else all
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val shorts: StateFlow<List<VideoEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getShorts(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Video for Full Watch Player
    private val _selectedVideo = MutableStateFlow<VideoEntity?>(null)
    val selectedVideo: StateFlow<VideoEntity?> = _selectedVideo.asStateFlow()

    // Active Creator Profile (when inspecting someone's profile)
    private val _viewedCreator = MutableStateFlow<UserEntity?>(null)
    val viewedCreator: StateFlow<UserEntity?> = _viewedCreator.asStateFlow()

    // Explore / Search State
    private val _exploreCategory = MutableStateFlow("All")
    val exploreCategory: StateFlow<String> = _exploreCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val exploreVideos: StateFlow<List<VideoEntity>> = combine(
        repository.currentUser,
        _exploreCategory,
        _searchQuery
    ) { user, category, query ->
        Triple(user?.id ?: "user_me", category, query)
    }.flatMapLatest { (uid, category, query) ->
        if (query.isNotBlank()) {
            repository.searchVideos(uid, query)
        } else if (category != "All") {
            repository.getVideosByCategory(uid, category)
        } else {
            repository.getAllVideos(uid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val creatorsList: StateFlow<List<UserEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getAllCreators(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User's own uploads & saved bookmarks
    @OptIn(ExperimentalCoroutinesApi::class)
    val userSavedVideos: StateFlow<List<VideoEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getSavedVideos(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userUploads: StateFlow<List<VideoEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getVideosByCreator(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Interaction Sets (Liked, Saved, Followed)
    val likedVideoIds: StateFlow<Set<String>> = repository.currentUser.flatMapLatest { user ->
        database.vibeTubeDao().getUserInteractions(user?.id ?: "user_me", "LIKE").map { list ->
            list.map { it.targetId }.toSet()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val savedVideoIds: StateFlow<Set<String>> = repository.currentUser.flatMapLatest { user ->
        database.vibeTubeDao().getUserInteractions(user?.id ?: "user_me", "SAVE").map { list ->
            list.map { it.targetId }.toSet()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val followedCreatorIds: StateFlow<Set<String>> = repository.currentUser.flatMapLatest { user ->
        database.vibeTubeDao().getUserInteractions(user?.id ?: "user_me", "FOLLOW").map { list ->
            list.map { it.targetId }.toSet()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Notifications
    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getNotifications(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadNotifsCount: StateFlow<Int> = repository.currentUser.flatMapLatest { user ->
        repository.getUnreadNotificationsCount(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // AI Assistant Chat Messages
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getChatMessages(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // Navigation Controls
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setHomeTab(tab: HomeFeedTab) {
        _homeTab.value = tab
    }

    fun setExploreCategory(category: String) {
        _exploreCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openWatchScreen(video: VideoEntity) {
        _selectedVideo.value = video
        _currentScreen.value = Screen.WATCH
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementViews(video.id)
        }
    }

    fun openCreatorProfile(creator: UserEntity) {
        _viewedCreator.value = creator
        _currentScreen.value = Screen.PROFILE
    }

    // Interactive Actions
    fun toggleLike(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleLike(currentUserId, videoId)
        }
    }

    fun toggleSave(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleSave(currentUserId, videoId)
        }
    }

    fun toggleFollow(creatorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFollow(currentUserId, creatorId)
        }
    }

    fun addComment(videoId: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addComment(videoId, text)
        }
    }

    fun getCommentsForVideo(videoId: String) = repository.getCommentsForVideo(videoId)

    // Upload New Video
    fun uploadVideo(
        title: String,
        description: String,
        isShort: Boolean,
        category: String,
        hashtags: String,
        videoUrl: String,
        thumbnailUrl: String,
        gradientStart: String,
        gradientEnd: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val created = repository.uploadVideo(
                title = title,
                description = description,
                isShort = isShort,
                category = category,
                hashtags = hashtags,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                gradientStart = gradientStart,
                gradientEnd = gradientEnd
            )
            if (created != null) {
                viewModelScope.launch(Dispatchers.Main) {
                    onSuccess()
                    _currentScreen.value = if (isShort) Screen.SHORTS else Screen.HOME
                }
            }
        }
    }

    // Creator Deleting Video
    fun deleteVideo(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVideo(videoId)
            if (_selectedVideo.value?.id == videoId) {
                _selectedVideo.value = null
                _currentScreen.value = Screen.HOME
            }
        }
    }

    // Safety: Reporting & Blocking
    fun reportVideo(videoId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reportVideo(videoId)
            if (_selectedVideo.value?.id == videoId) {
                _selectedVideo.value = null
                _currentScreen.value = Screen.HOME
            }
        }
    }

    fun blockUser(creatorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.blockUser(currentUserId, creatorId)
            _currentScreen.value = Screen.HOME
        }
    }

    // AI Chat Interaction
    fun askAi(prompt: String) {
        if (prompt.isBlank()) return
        val uid = currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            // Save user message immediately
            repository.saveChatMessage(uid, "user", prompt.trim())
            _isAiTyping.value = true

            // Fetch catalog for grounding
            val catalog = repository.getAllVideosDirect()
            val history = chatMessages.value.map { it.sender to it.text }

            val response = geminiClient.chat(prompt.trim(), catalog, history)

            val recommendedIdsString = if (response.recommendedVideos.isNotEmpty()) {
                response.recommendedVideos.joinToString(",") { it.id }
            } else null

            repository.saveChatMessage(
                userId = uid,
                sender = "ai",
                text = response.replyText,
                recommendedVideoIds = recommendedIdsString
            )
            _isAiTyping.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChatHistory(currentUserId)
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllNotificationsRead(currentUserId)
        }
    }

    // Auth & Profile
    fun updateProfile(displayName: String, username: String, bio: String, avatarUrl: String, isPrivate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(displayName, username, bio, avatarUrl, isPrivate)
        }
    }

    fun login(emailOrUser: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.login(emailOrUser, pass)
            viewModelScope.launch(Dispatchers.Main) {
                if (res.isSuccess) {
                    _currentScreen.value = Screen.HOME
                    onResult(true, null)
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Login failed")
                }
            }
        }
    }

    fun signUp(username: String, name: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.signUp(username, name, email, pass)
            viewModelScope.launch(Dispatchers.Main) {
                if (res.isSuccess) {
                    _currentScreen.value = Screen.HOME
                    onResult(true, null)
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Sign up failed")
                }
            }
        }
    }

    fun switchUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.switchUser(userId)
            _currentScreen.value = Screen.HOME
        }
    }

    // ==================== FRIENDS, MESSAGING & CALLS STATE ====================

    // Conversations
    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<DirectConversationEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getConversations(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directConversations: StateFlow<List<DirectConversationEntity>> = conversations.map { list ->
        list.filter { !it.isGroup }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupConversations: StateFlow<List<DirectConversationEntity>> = conversations.map { list ->
        list.filter { it.isGroup }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Call Logs
    @OptIn(ExperimentalCoroutinesApi::class)
    val callLogs: StateFlow<List<CallLogEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getCallLogs(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Followed Friends & Suggested
    @OptIn(ExperimentalCoroutinesApi::class)
    val followedFriends: StateFlow<List<UserEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getFollowedUsers(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val suggestedFriends: StateFlow<List<UserEntity>> = repository.currentUser.flatMapLatest { user ->
        repository.getSuggestedUsers(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total Unread Messages Count
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalUnreadMessagesCount: StateFlow<Int> = repository.currentUser.flatMapLatest { user ->
        repository.getTotalUnreadMessagesCount(user?.id ?: "user_me")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Active Conversation & Messages
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeConversation: StateFlow<DirectConversationEntity?> = _activeConversationId.flatMapLatest { id ->
        if (id != null) repository.getConversationById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<DirectMessageEntity>> = _activeConversationId.flatMapLatest { id ->
        if (id != null) repository.getDirectMessages(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Video to share via chat
    private val _videoToShare = MutableStateFlow<VideoEntity?>(null)
    val videoToShare: StateFlow<VideoEntity?> = _videoToShare.asStateFlow()

    // Active Call
    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

    private var callTimerJob: Job? = null

    // ==================== MESSAGING ACTIONS ====================

    fun openInbox() {
        _currentScreen.value = Screen.MESSAGES
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        _currentScreen.value = Screen.CHAT_DETAIL
        viewModelScope.launch(Dispatchers.IO) {
            repository.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    fun openDirectChatWith(otherUser: UserEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val conv = repository.createOrGetDirectConversation(user, otherUser)
            viewModelScope.launch(Dispatchers.Main) {
                _activeConversationId.value = conv.id
                _currentScreen.value = Screen.CHAT_DETAIL
            }
            repository.markMessagesAsRead(conv.id, user.id)
        }
    }

    fun sendDirectMessage(
        text: String,
        sharedVideoId: String? = null,
        sharedImageUrl: String? = null
    ) {
        val convId = _activeConversationId.value ?: return
        val user = currentUser.value ?: return
        if (text.isBlank() && sharedVideoId == null && sharedImageUrl == null) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(
                sender = user,
                conversationId = convId,
                text = text,
                sharedVideoId = sharedVideoId,
                sharedImageUrl = sharedImageUrl
            )
        }
    }

    fun deleteDirectMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(messageId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
                _currentScreen.value = Screen.MESSAGES
            }
        }
    }

    fun createGroup(
        groupName: String,
        groupAvatar: String?,
        selectedMembers: List<UserEntity>
    ) {
        val user = currentUser.value ?: return
        if (groupName.isBlank() || selectedMembers.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val group = repository.createGroupConversation(
                currentUser = user,
                groupName = groupName,
                groupAvatar = groupAvatar,
                selectedMembers = selectedMembers
            )
            viewModelScope.launch(Dispatchers.Main) {
                _activeConversationId.value = group.id
                _currentScreen.value = Screen.CHAT_DETAIL
            }
        }
    }

    fun leaveGroup(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.leaveGroup(conversationId, currentUserId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
                _currentScreen.value = Screen.MESSAGES
            }
        }
    }

    fun addMemberToGroup(conversationId: String, newMember: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMemberToGroup(conversationId, newMember.id)
        }
    }

    fun removeMemberFromGroup(conversationId: String, memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeMemberFromGroup(conversationId, memberId)
        }
    }

    // Video Sharing Dialog
    fun setVideoToShare(video: VideoEntity?) {
        _videoToShare.value = video
    }

    fun shareVideoToConversation(video: VideoEntity, conversationId: String, note: String = "") {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val caption = if (note.isNotBlank()) "$note\n${video.title}" else video.title
            repository.sendMessage(
                sender = user,
                conversationId = conversationId,
                text = caption,
                sharedVideoId = video.id
            )
            viewModelScope.launch(Dispatchers.Main) {
                _videoToShare.value = null
                openConversation(conversationId)
            }
        }
    }

    // Unfollow / Remove Friend
    fun removeFriend(targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeOrUnfollowUser(currentUserId, targetUserId)
        }
    }

    // Update Message & Call Privacy
    fun updatePrivacySettings(whoCanMessage: String, whoCanCall: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePrivacySettings(currentUserId, whoCanMessage, whoCanCall)
        }
    }

    // ==================== VOICE & VIDEO CALLS ====================

    fun startCall(otherUser: UserEntity, isVideo: Boolean) {
        val callId = "call_${UUID.randomUUID().toString().take(8)}"
        _activeCall.value = ActiveCallState(
            callId = callId,
            otherUser = otherUser,
            isVideo = isVideo,
            isIncoming = false,
            isConnected = false,
            durationSeconds = 0
        )
        _currentScreen.value = Screen.ACTIVE_CALL

        // Simulate connecting after 2.5 seconds
        viewModelScope.launch {
            delay(2500)
            if (_activeCall.value?.callId == callId && !_activeCall.value!!.isConnected) {
                _activeCall.value = _activeCall.value?.copy(isConnected = true)
                startCallTimer()
            }
        }
    }

    fun simulateIncomingCall(fromUser: UserEntity, isVideo: Boolean) {
        val callId = "call_${UUID.randomUUID().toString().take(8)}"
        _activeCall.value = ActiveCallState(
            callId = callId,
            otherUser = fromUser,
            isVideo = isVideo,
            isIncoming = true,
            isConnected = false,
            durationSeconds = 0
        )
        _currentScreen.value = Screen.ACTIVE_CALL
    }

    fun acceptCall() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isConnected = true, isIncoming = false)
        startCallTimer()
    }

    fun declineCall() {
        val current = _activeCall.value ?: return
        val caller = current.otherUser
        val me = currentUser.value ?: return
        callTimerJob?.cancel()

        viewModelScope.launch(Dispatchers.IO) {
            repository.logCall(
                caller = caller,
                receiver = me,
                isVideo = current.isVideo,
                status = "DECLINED",
                durationSeconds = 0
            )
        }
        _activeCall.value = null
        _currentScreen.value = Screen.MESSAGES
    }

    fun endCall() {
        val current = _activeCall.value ?: return
        val other = current.otherUser
        val me = currentUser.value ?: return
        val duration = current.durationSeconds
        val wasConnected = current.isConnected
        callTimerJob?.cancel()

        viewModelScope.launch(Dispatchers.IO) {
            val status = if (wasConnected) {
                "COMPLETED"
            } else if (current.isIncoming) {
                "MISSED"
            } else {
                "OUTGOING"
            }
            repository.logCall(
                caller = if (current.isIncoming) other else me,
                receiver = if (current.isIncoming) me else other,
                isVideo = current.isVideo,
                status = status,
                durationSeconds = duration
            )
        }
        _activeCall.value = null
        _currentScreen.value = Screen.MESSAGES
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    it.copy(durationSeconds = it.durationSeconds + 1)
                }
            }
        }
    }

    fun toggleCallMute() {
        _activeCall.value = _activeCall.value?.let {
            it.copy(isMuted = !it.isMuted)
        }
    }

    fun toggleCallCamera() {
        _activeCall.value = _activeCall.value?.let {
            it.copy(isCameraOff = !it.isCameraOff)
        }
    }

    fun switchCallCamera() {
        _activeCall.value = _activeCall.value?.let {
            it.copy(isFrontCamera = !it.isFrontCamera)
        }
    }

    fun deleteCallLog(callId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCallLog(callId)
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCallLogs(currentUserId)
        }
    }

    // ==================== SIDE IN MESSAGE WITH AI ====================

    fun generateSmartRepliesForChat(
        lastMessage: String,
        onResult: (List<String>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val replies = geminiClient.generateSmartReplies(lastMessage)
            viewModelScope.launch(Dispatchers.Main) {
                onResult(replies)
            }
        }
    }

    fun rewriteChatMessage(
        text: String,
        tone: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rewritten = geminiClient.rewriteMessage(text, tone)
            viewModelScope.launch(Dispatchers.Main) {
                onResult(rewritten)
            }
        }
    }

    fun summarizeChatMessages(
        messagesList: List<String>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = geminiClient.summarizeConversation(messagesList)
            viewModelScope.launch(Dispatchers.Main) {
                onResult(summary)
            }
        }
    }

    fun summarizeChatMessagesWithMode(
        messagesList: List<String>,
        mode: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = geminiClient.summarizeConversationWithMode(messagesList, mode)
            viewModelScope.launch(Dispatchers.Main) {
                onResult(summary)
            }
        }
    }

    fun askAiInSideChat(
        query: String,
        conversationContext: List<Pair<String, String>>,
        onResult: (com.example.data.ai.AiResponse) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val catalog = repository.getAllVideosDirect()
            val response = geminiClient.chat(query, catalog, conversationContext)
            viewModelScope.launch(Dispatchers.Main) {
                onResult(response)
            }
        }
    }
}

