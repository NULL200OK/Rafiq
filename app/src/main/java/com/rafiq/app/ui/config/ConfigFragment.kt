package com.rafiq.app.ui.config

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rafiq.app.MainActivity
import com.rafiq.app.R
import com.rafiq.app.config.ConfigManager
import com.rafiq.app.config.LlmConfig
import com.rafiq.app.databinding.FragmentConfigBinding
import com.rafiq.app.network.AuthExpiredException
import com.rafiq.app.network.ZaiClient
import kotlinx.coroutines.launch

class ConfigFragment : Fragment(R.layout.fragment_config) {

    private var _b: FragmentConfigBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentConfigBinding.bind(view)

        val reason = arguments?.getInt(ARG_REASON, 0) ?: 0
        if (reason != 0) {
            b.tvSubtitle.text = getString(reason)
            b.tvSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.rafiq_error))
        }

        ConfigManager.load(requireContext())?.let { saved ->
            b.etBaseUrl.setText(saved.baseUrl)
            b.etModel.setText(saved.model)
        }

        b.btnTest.setOnClickListener { testConnection() }
        b.btnSave.setOnClickListener { saveConfig() }
    }

    private fun resolveApiKey(): String? {
        val typed = b.etApiKey.text?.toString()?.trim()
        return typed?.takeIf { it.isNotEmpty() }
            ?: ConfigManager.load(requireContext())?.apiKey
    }

    private fun currentConfig() = resolveApiKey()?.let { key ->
        LlmConfig(
            apiKey = key,
            baseUrl = b.etBaseUrl.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: ConfigManager.DEFAULT_BASE_URL,
            model = b.etModel.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() } ?: ConfigManager.DEFAULT_MODEL
        )
    }

    private fun testConnection() {
        val config = currentConfig() ?: run {
            toast(R.string.error_empty_key); return
        }
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = ZaiClient.test(config)
            setLoading(false)
            result.fold(
                onSuccess = { toast(R.string.connection_ok) },
                onFailure = { toast(if (it is AuthExpiredException) R.string.auth_invalid else R.string.connection_failed) }
            )
        }
    }

    private fun saveConfig() {
        val config = currentConfig() ?: run {
            toast(R.string.error_empty_key); return
        }
        ConfigManager.save(requireContext(), config.apiKey, config.baseUrl, config.model)
        (activity as? MainActivity)?.showChat()
    }

    private fun setLoading(loading: Boolean) {
        b.btnTest.isEnabled = !loading
        b.btnSave.isEnabled = !loading
        b.btnTest.text = getString(if (loading) R.string.testing else R.string.test_connection)
    }

    private fun toast(@StringRes res: Int) =
        Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        private const val ARG_REASON = "reason"
        fun newInstance(@StringRes reason: Int? = null) = ConfigFragment().apply {
            arguments = Bundle().apply { reason?.let { putInt(ARG_REASON, it) } }
        }
    }
}