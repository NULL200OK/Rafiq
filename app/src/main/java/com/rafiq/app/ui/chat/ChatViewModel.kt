package com.rafiq.app.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rafiq.app.R
import com.rafiq.app.brain.EmotionParser
import com.rafiq.app.brain.MemoryManager
import com.rafiq.app.brain.MemoryTagParser
import com.rafiq.app.config.ConfigManager
import com.rafiq.app.data.local.MessageEntity
import com.rafiq.app.data.local.RafiqDatabase
import com.rafiq.app.data.local.ResponseMemoryEntity
import com.rafiq.app.data.local.UserMemoryEntity
import com.rafiq.app.face.Emotion
import com.rafiq.app.mode.ModePrompts
import com.rafiq.app.mode.RafiqMode
import com.rafiq.app.mode.SafetyFilter
import com.rafiq.app.mode.TrainerBank
import com.rafiq.app.morning.MorningStore
import com.rafiq.app.network.NetworkMonitor
import com.rafiq.app.network.StreamEvent
import com.rafiq.app.network.ZaiClient
import com.rafiq.app.offline.OfflineEngine
import com.rafiq.app.prefs.RafiqPrefs
import com.rafiq.app.voice.SentenceSplitter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val EVENT_AUTH_EXPIRED = 1
        const val EVENT_NETWORK_FAIL = 2
        private const val CONTEXT_MESSAGES = 21
        private const val RESULT_OK = 0
        private const val RESULT_FAIL = 1
        private const val RESULT_AUTH = 2
        private const val TAG = "ChatViewModel"
        private const val WELCOME_BACK_GAP = 30 * 60 * 1000L
        private const val MAX_MEMORIES = 60
    }

    private val chatDao = RafiqDatabase.getInstance(app).chatDao()
    private val memoryDao = RafiqDatabase.getInstance(app).memoryDao()
    private val userMemoryDao = RafiqDatabase.getInstance(app).userMemoryDao()

    val messages: StateFlow<List<MessageEntity>> = chatDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _emotion = MutableStateFlow(Emotion.CALM)
    val emotion: StateFlow<Emotion> = _emotion.asStateFlow()

    val memories: StateFlow<List<UserMemoryEntity>> = userMemoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteMemory(id: Long) = viewModelScope.launch { userMemoryDao.delete(id) }
    fun clearMemories() = viewModelScope.launch { userMemoryDao.clearAll() }

    private val _mode = MutableStateFlow(RafiqMode.current(app))
    val mode: StateFlow<RafiqMode> = _mode.asStateFlow()

    private val _trainerLang = MutableStateFlow(RafiqPrefs.trainerLang(app))
    val trainerLang: StateFlow<String> = _trainerLang.asStateFlow()

    fun setMode(value: RafiqMode) {
        RafiqPrefs.setMode(getApplication(), value.id)
        _mode.value = value
    }

    fun setTrainer(lang: String) {
        RafiqPrefs.setTrainerLang(getApplication(), lang)
        _trainerLang.value = lang
    }

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    private val _liveText = MutableStateFlow<String?>(null)
    val liveText: StateFlow<String?> = _liveText.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events: SharedFlow<Int> = _events.asSharedFlow()

    private val _speakEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val speakEvents: SharedFlow<String> = _speakEvents.asSharedFlow()

    private val _speakEnabled = MutableStateFlow(RafiqPrefs.speakEnabled(app))
    val speakEnabled: StateFlow<Boolean> = _speakEnabled.asStateFlow()

    fun toggleSpeak() {
        val newValue = !_speakEnabled.value
        _speakEnabled.value = newValue
        RafiqPrefs.setSpeakEnabled(getApplication(), newValue)
    }

    init {
        viewModelScope.launch {
            val streak = MorningStore.checkIn(app)
            if (chatDao.count() == 0) {
                insert(MessageEntity.ROLE_RAFIQ, app.getString(R.string.rafiq_hello), Emotion.HAPPY)
                delay(800)
                _speakEvents.emit(app.getString(R.string.rafiq_hello))
            } else {
                maybeWelcomeBack(app, streak)
            }
            refreshConnectivity()
        }
    }

    private suspend fun maybeWelcomeBack(app: Application, streak: Int) {
        val last = chatDao.lastMessage() ?: return
        if (System.currentTimeMillis() - last.createdAt < WELCOME_BACK_GAP) return
        val name = MemoryManager.userName(userMemoryDao.recent(MAX_MEMORIES))
        var text = if (name.isBlank()) app.getString(R.string.welcome_back)
                   else app.getString(R.string.welcome_back_name, name)
        if (streak >= 3) text += "\n" + app.getString(R.string.morning_streak, streak)
        insert(MessageEntity.ROLE_RAFIQ, text, Emotion.HAPPY)
        delay(600)
        _speakEvents.emit(text)
    }

    fun refreshConnectivity() = setOnline(NetworkMonitor.isOnline(getApplication()))

    fun setOnline(online: Boolean) {
        _offlineMode.value = !(online && ConfigManager.isConfigured(getApplication()))
    }

    fun clearChat() = viewModelScope.launch {
        chatDao.clearAll()
        val hello = getApplication<Application>().getString(R.string.rafiq_hello)
        insert(MessageEntity.ROLE_RAFIQ, hello, Emotion.HAPPY)
        delay(500)
        _speakEvents.emit(hello)
    }

    fun send(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        if (_isTyping.value || _liveText.value != null) return false

        if (_mode.value == RafiqMode.KID && SafetyFilter.isDirty(text)) {
            viewModelScope.launch {
                insert(MessageEntity.ROLE_USER, text)
                val reply = getApplication<Application>().getString(R.string.kid_blocked)
                answerOffline(text, forcedReply = reply, forcedEmotion = Emotion.CARING)
            }
            return true
        }

        viewModelScope.launch {
            insert(MessageEntity.ROLE_USER, text)
            _emotion.value = Emotion.THINKING
            val app = getApplication<Application>()
            val online = NetworkMonitor.isOnline(app) && ConfigManager.isConfigured(app)
            _offlineMode.value = !online

            if (online) {
                when (tryOnline(text)) {
                    RESULT_OK -> Unit
                    RESULT_AUTH -> { answerOffline(text); _events.emit(EVENT_AUTH_EXPIRED) }
                    else -> { _events.emit(EVENT_NETWORK_FAIL); answerOffline(text) }
                }
            } else {
                answerOffline(text)
            }
        }
        return true
    }

    private suspend fun tryOnline(userText: String): Int {
        val config = ConfigManager.load(getApplication()) ?: return RESULT_FAIL

        val history = chatDao.getRecent(CONTEXT_MESSAGES).reversed()
        val memSection = MemoryManager.promptSection(userMemoryDao.recent(30))
        val prompt = ModePrompts.system(_mode.value, _trainerLang.value.ifBlank { null })
        val apiMessages = ArrayList<Pair<String, String>>(history.size + 1).apply {
            add("system" to (prompt + (memSection ?: "")))
            history.forEach {
                add(if (it.role == MessageEntity.ROLE_USER) "user" to it.content else "assistant" to it.content)
            }
        }

        _isTyping.value = true
        val sb = StringBuilder()
        val splitter = SentenceSplitter()
        val eParser = EmotionParser()
        val mParser = MemoryTagParser()
        var result = RESULT_OK
        var gotFirst = false

        ZaiClient.streamChat(config, apiMessages).collect { event ->
            when (event) {
                is StreamEvent.Chunk -> {
                    if (!gotFirst) { _isTyping.value = false; gotFirst = true }
                    var clean = eParser.feed(event.text)
                    eParser.emotion?.let { _emotion.value = it }
                    clean = mParser.feed(clean)
                    if (clean.isNotEmpty()) {
                        sb.append(clean)
                        _liveText.value = sb.toString()
                        splitter.feed(clean).forEach { _speakEvents.emit(it) }
                    }
                }
                StreamEvent.AuthExpired -> result = RESULT_AUTH
                is StreamEvent.Failed -> { Log.e(TAG, "Stream failed", event.error); result = RESULT_FAIL }
                else -> Unit
            }
        }
        _isTyping.value = false

        val replyRaw = sb.toString().trim()
        return when {
            result == RESULT_AUTH -> RESULT_AUTH
            replyRaw.isNotEmpty() -> {
                var rest = eParser.flush()
                eParser.emotion?.let { _emotion.value = it }
                rest = mParser.feed(rest)
                rest += mParser.flush()
                if (rest.isNotBlank()) {
                    sb.append(rest)
                    splitter.feed(rest).forEach { _speakEvents.emit(it) }
                }
                splitter.flush()?.let { _speakEvents.emit(it) }

                val blocked = _mode.value == RafiqMode.KID &&
                        SafetyFilter.isDirty(sb.toString())
                val reply = if (blocked) getApplication<Application>().getString(R.string.kid_blocked)
                            else sb.toString().trim()
                if (blocked) _emotion.value = Emotion.CARING

                saveNewMemories(if (blocked) emptyList() else mParser.memories)
                commit(userText, reply, saveMemory = !blocked, emotion = _emotion.value)
                RESULT_OK
            }
            else -> { _emotion.value = Emotion.DEFAULT; RESULT_FAIL }
        }
    }

    private suspend fun saveNewMemories(tags: List<MemoryTagParser.MemoryTag>) {
        if (tags.isEmpty()) return
        val existing = userMemoryDao.recent(MAX_MEMORIES)
        val seen = HashSet<String>()
        for (t in tags) {
            val category = MemoryManager.normalizeCategory(t.category)
            val content = t.content
            if (!seen.add("$category|$content")) continue
            if (existing.any { it.category == category && it.content.equals(content, ignoreCase = true) }) continue
            userMemoryDao.insert(
                UserMemoryEntity(category = category, content = content, createdAt = System.currentTimeMillis())
            )
        }
        userMemoryDao.trim(MAX_MEMORIES)
    }

    private suspend fun answerOffline(
        question: String,
        forcedReply: String? = null,
        forcedEmotion: Emotion? = null
    ) {
        val app = getApplication<Application>()
        _isTyping.value = true
        delay(600)
        val memories = memoryDao.recent(200)

        val body = forcedReply ?: when {
            _mode.value == RafiqMode.KID ->
                OfflineEngine.reply(question, memories, kidMode = true)
            _trainerLang.value.isNotBlank() ->
                app.getString(R.string.trainer_offline_prefix) + "\n" +
                        TrainerBank.practice(_trainerLang.value)
            else -> OfflineEngine.reply(question, memories)
        }
        val tag = forcedEmotion ?: when {
            _mode.value == RafiqMode.KID -> Emotion.HAPPY
            _trainerLang.value.isNotBlank() -> Emotion.CURIOUS
            else -> OfflineEngine.emotionFor(question, memories)
        }
        _isTyping.value = false

        val eParser = EmotionParser()
        val clean = (eParser.feed("[${tag.tag}]$body") + eParser.flush()).trim()
        val emotion = eParser.emotion ?: tag
        _emotion.value = emotion

        _liveText.value = clean
        delay(200)
        commit(question, clean, saveMemory = false, emotion = emotion)
        _speakEvents.emit(clean)
    }

    private suspend fun commit(question: String, answer: String, saveMemory: Boolean, emotion: Emotion) {
        _liveText.value = null
        insert(MessageEntity.ROLE_RAFIQ, answer, emotion)
        if (saveMemory && question.length in 4..300 && answer.length in 1..800) {
            memoryDao.insert(
                ResponseMemoryEntity(question = question, answer = answer, createdAt = System.currentTimeMillis())
            )
            memoryDao.trim(200)
        }
    }

    private suspend fun insert(role: String, content: String, emotion: Emotion? = null) {
        chatDao.insert(
            MessageEntity(
                role = role, content = content,
                createdAt = System.currentTimeMillis(),
                emotion = emotion?.tag ?: ""
            )
        )
    }
}