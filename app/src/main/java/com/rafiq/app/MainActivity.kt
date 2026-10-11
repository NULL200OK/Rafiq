package com.rafiq.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rafiq.app.config.ConfigManager
import com.rafiq.app.databinding.ActivityMainBinding
import com.rafiq.app.prefs.RafiqPrefs
import com.rafiq.app.ui.chat.ChatFragment
import com.rafiq.app.ui.config.ConfigFragment
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // ✅ أول شيء: تحقق من وجود تقرير انهيار سابق
            if (showCrashIfExists()) return
            maybeShowWelcome()
            navigate()
        }
    }

    /** إذا وُجد تقرير انهيار من جلسة سابقة، اعرضه للمستخدم قبل المتابعة */
    private fun showCrashIfExists(): Boolean {
        val file = File(filesDir, "rafiq_crash.txt")
        if (!file.exists()) return false

        val log = try { file.readText() } catch (_: Exception) { "" }
        file.delete()  // احذفه بعد القراءة حتى لا يتكرر

        if (log.isBlank()) return false

        AlertDialog.Builder(this)
            .setTitle("⚠️ تقرير الانهيار السابق")
            .setMessage(log)
            .setPositiveButton("نسخ") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("rafiq_crash", log))
                Toast.makeText(this, "✅ تم النسخ — الصقه في المحادثة", Toast.LENGTH_LONG).show()
                continueAfterCrash()
            }
            .setNeutralButton("مشاركة") { _, _ ->
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Rafiq Crash Report")
                    putExtra(Intent.EXTRA_TEXT, log)
                }
                startActivity(Intent.createChooser(i, "مشاركة تقرير الانهيار"))
                continueAfterCrash()
            }
            .setNegativeButton("تجاهل") { _, _ ->
                continueAfterCrash()
            }
            .setCancelable(false)
            .show()
        return true
    }

    private fun continueAfterCrash() {
        maybeShowWelcome()
        navigate()
    }

    /** رسالة ترحيب أول تشغيل */
    private fun maybeShowWelcome() {
        if (RafiqPrefs.welcomeShown(this)) return
        RafiqPrefs.setWelcomeShown(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.welcome_title)
            .setMessage(R.string.welcome_message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun navigate() {
        if (ConfigManager.isConfigured(this)) showChat() else openConfig(null)
    }

    fun openConfig(reasonRes: Int?) {
        replace(ConfigFragment.newInstance(reasonRes))
    }

    fun showChat() {
        replace(ChatFragment())
    }

    private fun replace(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
