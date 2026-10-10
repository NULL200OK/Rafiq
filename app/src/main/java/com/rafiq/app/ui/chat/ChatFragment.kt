package com.rafiq.app.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rafiq.app.R
import com.rafiq.app.voice.SpeechRecognitionManager
import com.rafiq.app.voice.TextToSpeechManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var ttsManager: TextToSpeechManager

    // ===== منتقي الصور (يدعم جميع الامتدادات) =====
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                // صلاحية قراءة دائمة
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { /* ignore */ }

                // أضف الرسالة إلى القائمة
                val msg = ChatMessage(imageUri = it.toString(), isUser = true)
                messages.add(msg)
                adapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    // ===== صلاحية الميكروفون =====
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechManager.startListening()
        else Toast.makeText(requireContext(), "نحتاج صلاحية الميكروفون", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerChat)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ChatAdapter(messages) { imageUri ->
            // ✅ عند النقر على الصورة، افتحها بالحجم الكامل
            FullImageDialog.newInstance(imageUri)
                .show(parentFragmentManager, "FullImageDialog")
        }
        recyclerView.adapter = adapter

        // ===== Voice Managers =====
        speechManager = SpeechRecognitionManager(
            context = requireContext(),
            onResult = { text ->
                val msg = ChatMessage(text = text, isUser = true)
                messages.add(msg)
                adapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
                // مثال على الرد (استبدله بمنطق Rafiq الحقيقي)
                ttsManager.speak(text)
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )

        ttsManager = TextToSpeechManager(
            context = requireContext(),
            onError = { err -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show() }
        )

        // ===== أزرار =====
        val micBtn: FloatingActionButton? = view.findViewById(R.id.micButton)
        val imageBtn: FloatingActionButton? = view.findViewById(R.id.imageButton)

        micBtn?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) speechManager.startListening()
            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        imageBtn?.setOnClickListener { openImagePicker() }
    }

    // ===== منتقي الصور: يدعم كل الامتدادات =====
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            // ✅ إضافة جميع أنواع الصور الشائعة والنادرة
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "image/png", "image/jpeg", "image/jpg", "image/gif",
                    "image/webp", "image/bmp", "image/svg+xml",
                    "image/heic", "image/heif", "image/tiff", "image/avif"
                )
            )
        }
        imagePickerLauncher.launch(intent)
    }

    override fun onDestroyView() {
        speechManager.destroy()
        ttsManager.shutdown()
        super.onDestroyView()
    }
}
