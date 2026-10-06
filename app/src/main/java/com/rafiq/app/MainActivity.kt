package com.rafiq.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rafiq.app.config.ConfigManager
import com.rafiq.app.databinding.ActivityMainBinding
import com.rafiq.app.prefs.RafiqPrefs
import com.rafiq.app.ui.chat.ChatFragment
import com.rafiq.app.ui.config.ConfigFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            maybeShowWelcome()
            navigate()
        }
    }

    /** رسالة ترحيب أول تشغيل — تتضمن اسم المصمم بالعربية والإنجليزية */
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