package com.rafiq.app.ui.chat

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.rafiq.app.R

class FullImageDialog : DialogFragment() {

    companion object {
        private const val ARG_URI = "arg_uri"
        fun newInstance(uri: String): FullImageDialog {
            val d = FullImageDialog()
            d.arguments = Bundle().apply { putString(ARG_URI, uri) }
            return d
        }
    }

    private var imageUri: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_full_image, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageUri = arguments?.getString(ARG_URI)

        val imageView: ImageView = view.findViewById(R.id.fullImageView)
        Glide.with(this)
            .load(imageUri)
            .into(imageView)

        // ✅ النقر على الصورة يغلقها (بديل إضافي لزر الرجوع)
        imageView.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setDimAmount(0f)
            // ✅ إخفاء شريط الحالة لملء الشاشة
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }
}
