package com.rafiq.app.ui.chat

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rafiq.app.MainActivity
import com.rafiq.app.R
import com.rafiq.app.brain.MemoryManager
import com.rafiq.app.databinding.FragmentChatBinding
import com.rafiq.app.face.FaceAnalyzer
import com.rafiq.app.face.FaceData
import com.rafiq.app.face.FaceStore
import com.rafiq.app.mode.RafiqMode
import com.rafiq.app.morning.MorningScheduler
import com.rafiq.app.morning.MorningStore
import com.rafiq.app.network.NetworkMonitor
import com.rafiq.app.prefs.RafiqPrefs
import com.rafiq.app.voice.SpeechRecognitionManager
import com.rafiq.app.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var _b: FragmentChatBinding? = null
    private val b get() = _b!!
    private val vm: ChatViewModel by viewModels()
    private val adapter = ChatAdapter()
    private var networkWatcher: NetworkMonitor.Watcher? = null

    private lateinit var tts: TextToSpeechManager
    private var stt: SpeechRecognitionManager? = null
    private val missingVoiceLangs = mutableSetOf<String>()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput()
        else toast(R.string.mic_permission_denied)
    }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handlePicked(it) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentChatBinding.bind(view)

        setupChat()
        setupVoice()
        setupFace()
        setupMemory()
        setupMode()
        applyModeUi()
        loadFace()
        observeState()
        maybeShowHint()
    }

    private fun setupChat() {
        val mode = RafiqMode.current(requireContext())
        adapter.textScale = if (mode == RafiqMode.SENIOR) 1.35f else 1f

        b.recycler.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        b.recycler.adapter = adapter

        b.typingRow.startAnimation(AlphaAnimation(0.25f, 1f).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        })

        val sendAction = {
            val text = b.etInput.text?.toString().orEmpty()
            if (vm.send(text)) b.etInput.setText("")
        }
        b.btnSend.setOnClickListener { sendAction() }
        b.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendAction(); true } else false
        }
        b.btnSettings.setOnClickListener { showSettingsDialog() }
        adapter.onLongSpeak = { content ->
            if (vm.speakEnabled.value) { tts.stop(); tts.speak(content) }
        }
    }

    private fun showSettingsDialog() {
        val ctx = requireContext()
        val enabled = MorningStore.isEnabled(ctx)
        val (h, m) = MorningStore.time(ctx)
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", h, m)

        val items = arrayOf(
            if (enabled) getString(R.string.morning_state_on, timeStr)
            else getString(R.string.morning_state_off),
            getString(R.string.morning_set_time),
            getString(R.string.clear_chat),
            getString(R.string.settings_config),
            getString(R.string.about_title)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.settings_menu)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> toggleMorning()
                    1 -> { if (!MorningStore.isEnabled(ctx)) { MorningStore.setEnabled(ctx, true); MorningScheduler.schedule(ctx) }; pickMorningTime() }
                    2 -> confirmClearChat()
                    3 -> (activity as? MainActivity)?.openConfig(null)
                    4 -> showAbout()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toggleMorning() {
        val ctx = requireContext()
        if (MorningStore.isEnabled(ctx)) {
            MorningStore.setEnabled(ctx, false)
            MorningScheduler.cancel(ctx)
            toast(R.string.morning_disabled)
        } else {
            MorningStore.setEnabled(ctx, true)
            MorningScheduler.schedule(ctx)
            maybeRequestNotifPermission()
            maybeRequestExactAlarm()
            toast(R.string.morning_enabled)
        }
    }

    private fun maybeRequestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun maybeRequestExactAlarm() {
        if (Build.VERSION.SDK_INT < 31) return
        val am = context?.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        if (am.canScheduleExactAlarms()) return
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.exact_alarm_hint)
            .setPositiveButton(R.string.ok) { _, _ ->
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${requireContext().packageName}")
                        )
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pickMorningTime() {
        val ctx = requireContext()
        val (h, m) = MorningStore.time(ctx)
        TimePickerDialog(ctx, { _, hour, minute ->
            MorningStore.setTime(ctx, hour, minute)
            MorningScheduler.schedule(ctx)
            toast(R.string.morning_time_set)
        }, h, m, true).show()
    }

    private fun confirmClearChat() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.clear_chat_confirm)
            .setPositiveButton(R.string.ok) { _, _ ->
                vm.clearChat()
                toast(R.string.chat_cleared)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_body)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun setupVoice() {
        tts = TextToSpeechManager(requireContext())
        tts.onLanguageMissing = { lang ->
            if (missingVoiceLangs.add(lang)) toast(R.string.tts_lang_missing)
        }
        tts.onSpeakingChanged = { speaking -> _b?.faceView?.setSpeaking(speaking) }
        tts.onWord = { _b?.faceView?.pulseMouth() }
        applyVoiceProfile()

        b.btnSpeaker.setOnClickListener { vm.toggleSpeak() }
        b.btnMic.setOnClickListener { onMicPressed() }
    }

    private fun applyVoiceProfile() {
        when (RafiqMode.current(requireContext())) {
            RafiqMode.SENIOR -> tts.setProfile(0.8f, 1.0f)
            RafiqMode.KID -> tts.setProfile(1.0f, 1.15f)
            RafiqMode.NORMAL -> tts.setProfile(1.0f, 1.05f)
        }
    }

    private fun onMicPressed() {
        when {
            stt?.isListening == true -> stt?.cancel()
            else -> {
                tts.stop()
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) startVoiceInput()
                else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceInput() {
        val lang = vm.trainerLang.value.ifBlank { null }
        val recognizer = stt ?: SpeechRecognitionManager(requireContext().applicationContext).also {
            stt = it
            it.onPartialResult = { partial ->
                _b?.let { bb -> bb.etInput.setText(partial); bb.etInput.setSelection(partial.length) }
            }
            it.onFinalResult = { text ->
                _b?.let { bb -> bb.etInput.setText(""); vm.send(text) }
            }
            it.onStateChange = { listening -> updateMicUI(listening) }
            it.onError = { code -> handleSttError(code) }
        }
        recognizer.startListening(lang)
    }

    private fun handleSttError(code: Int) {
        when (code) {
            SpeechRecognitionManager.ERR_UNAVAILABLE -> toast(R.string.voice_unavailable)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> toast(R.string.mic_permission_denied)
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Unit
            else -> toast(R.string.voice_error)
        }
    }

    private fun updateMicUI(listening: Boolean) {
        _b ?: return
        b.btnMic.backgroundTintList = ColorStateList.valueOf(
            if (listening) 0xFFE53935.toInt() else 0xFFECEFF1.toInt()
        )
        b.tvVoiceState.visibility = if (listening) View.VISIBLE else View.GONE
        b.etInput.hint = getString(if (listening) R.string.listening_short else R.string.chat_input_hint)
        b.faceView.setListening(listening)
    }

    private fun setupFace() {
        b.btnFaceEdit.setOnClickListener { showFaceMenu() }
    }

    private fun loadFace() {
        val stored = FaceStore.load(requireContext())
        if (stored != null) b.faceView.setFace(stored.first, stored.second)
        else b.faceView.setFace(FaceStore.defaultFace(requireContext()), FaceData.NONE)
    }

    private fun showFaceMenu() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.face_menu)
            .setItems(
                arrayOf(getString(R.string.face_opt_change), getString(R.string.face_opt_reset))
            ) { _, which ->
                when (which) {
                    0 -> pickImage.launch("image/*")
                    1 -> { FaceStore.clear(requireContext()); loadFace() }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handlePicked(uri: Uri) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val prepared = withContext(Dispatchers.IO) {
                FaceAnalyzer.decode(appContext, uri)?.let { FaceAnalyzer.prepare(it) }
            }
            if (prepared == null) { toast(R.string.face_error); return@launch }
            withContext(Dispatchers.IO) { FaceStore.save(appContext, prepared.first, prepared.second) }
            _b ?: return@launch
            b.faceView.setFace(prepared.first, prepared.second)
            toast(if (prepared.second.hasFace) R.string.face_saved else R.string.face_no_face)
        }
    }

    private fun setupMemory() {
        b.btnMemory.setOnClickListener { showMemoryDialog() }
    }

    private fun showMemoryDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = vm.memories.first()
            if (list.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.mem_title)
                    .setMessage(R.string.mem_empty)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return@launch
            }
            val items = ArrayList<String>(list.size + 1).apply {
                add(getString(R.string.mem_clear_all))
                list.forEach { "${MemoryManager.categoryEmoji(it.category)} ${it.content}".also(::add) }
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.mem_title)
                .setItems(items.toTypedArray()) { _, which ->
                    when {
                        which == 0 -> confirmAction { vm.clearMemories(); toast(R.string.mem_cleared) }
                        else -> {
                            val memory = list[which - 1]
                            confirmAction { vm.deleteMemory(memory.id) }
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun confirmAction(action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.mem_delete_confirm)
            .setPositiveButton(R.string.ok) { _, _ -> action() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupMode() {
        b.btnMode.setOnClickListener { showModeMenu() }
        b.trainerBanner.setOnClickListener { showTrainerMenu() }
    }

    private fun showModeMenu() {
        val items = arrayOf(
            getString(R.string.mode_normal),
            getString(R.string.mode_kid),
            getString(R.string.mode_senior),
            getString(R.string.mode_trainer)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mode_menu)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> changeMode(RafiqMode.NORMAL)
                    1 -> changeMode(RafiqMode.KID)
                    2 -> changeMode(RafiqMode.SENIOR)
                    3 -> showTrainerMenu()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showTrainerMenu() {
        val items = arrayOf(
            getString(R.string.trainer_fr),
            getString(R.string.trainer_en),
            getString(R.string.trainer_ar),
            getString(R.string.trainer_off)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.trainer_title)
            .setItems(items) { _, which ->
                vm.setTrainer(
                    when (which) {
                        0 -> "fr"; 1 -> "en"; 2 -> "ar"; else -> ""
                    }
                )
                toast(R.string.mode_changed)
                activity?.recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun changeMode(mode: RafiqMode) {
        if (vm.mode.value == mode) return
        vm.setMode(mode)
        toast(R.string.mode_changed)
        activity?.recreate()
    }

    private fun applyModeUi() {
        val mode = RafiqMode.current(requireContext())
        b.btnMode.text = mode.emoji

        val trainer = vm.trainerLang.value
        if (trainer.isNotBlank()) {
            b.trainerBanner.visibility = View.VISIBLE
            b.trainerBanner.setText(trainerBannerRes(trainer))
        } else b.trainerBanner.visibility = View.GONE

        if (mode == RafiqMode.SENIOR) {
            b.etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            b.btnMic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            b.btnSpeaker.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            b.typingRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            b.offlineBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            b.tvVoiceState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            b.trainerBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            b.faceContainer.layoutParams = b.faceContainer.layoutParams.apply {
                height = (180 * resources.displayMetrics.density).toInt()
            }
            b.faceContainer.requestLayout()
        }
    }

    private fun trainerBannerRes(lang: String): Int = when (lang) {
        "fr" -> R.string.trainer_banner_fr
        "en" -> R.string.trainer_banner_en
        else -> R.string.trainer_banner_ar
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.messages.collect { adapter.submit(it); scrollDown() } }
                launch { vm.liveText.collect { live -> adapter.showLive(live); if (live != null) scrollDown() } }
                launch {
                    vm.isTyping.collect { typing ->
                        b.typingRow.visibility = if (typing) View.VISIBLE else View.GONE
                        if (typing) scrollDown()
                    }
                }
                launch { vm.offlineMode.collect { applyConnectivityUI(it) } }
                launch { vm.emotion.collect { b.faceView.setEmotion(it) } }
                launch { vm.mode.collect { m -> b.btnMode.text = m.emoji } }
                launch {
                    vm.trainerLang.collect { lang ->
                        b.trainerBanner.visibility = if (lang.isBlank()) View.GONE else View.VISIBLE
                        if (lang.isNotBlank()) b.trainerBanner.setText(trainerBannerRes(lang))
                    }
                }
                launch {
                    vm.speakEvents.collect { sentence ->
                        if (vm.speakEnabled.value) tts.speak(sentence)
                    }
                }
                launch {
                    vm.speakEnabled.collect { enabled ->
                        tts.setEnabled(enabled)
                        b.btnSpeaker.text = if (enabled) "🔊" else "🔇"
                    }
                }
                launch {
                    vm.events.collect { event ->
                        when (event) {
                            ChatViewModel.EVENT_AUTH_EXPIRED -> {
                                Toast.makeText(requireContext(), R.string.auth_invalid, Toast.LENGTH_LONG).show()
                                (activity as? MainActivity)?.openConfig(R.string.config_expired)
                            }
                            ChatViewModel.EVENT_NETWORK_FAIL ->
                                Toast.makeText(requireContext(), R.string.fallback_offline, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun maybeShowHint() {
        if (!RafiqPrefs.hintShown(requireContext())) {
            toast(R.string.long_press_hint)
            RafiqPrefs.setHintShown(requireContext())
        }
    }

    private fun applyConnectivityUI(offline: Boolean) {
        b.offlineBanner.visibility = if (offline) View.VISIBLE else View.GONE
        b.tvStatus.setText(if (offline) R.string.status_offline else R.string.status_online)
        b.statusDot.backgroundTintList =
            ColorStateList.valueOf(if (offline) 0xFFBDBDBD.toInt() else 0xFF66BB6A.toInt())
    }

    private fun scrollDown() {
        b.recycler.post {
            if (adapter.itemCount > 0) b.recycler.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun toast(res: Int) =
        Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()

    override fun onStart() {
        super.onStart()
        vm.refreshConnectivity()
        networkWatcher = NetworkMonitor.observe(requireContext().applicationContext) { online ->
            vm.setOnline(online)
        }
    }

    override fun onStop() {
        super.onStop()
        networkWatcher?.stop(); networkWatcher = null
        stt?.cancel()
        tts.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.shutdown()
        stt?.cancel()
        _b = null
    }
}