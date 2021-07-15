package com.subranil_saha.placebook.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PhotoOptionsDialogFragment : DialogFragment() {
    interface PhotoOptionsDialogListener {
        fun onCaptureClick()
        fun onPickClick()
    }

    private lateinit var listener: PhotoOptionsDialogListener
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        listener = activity as PhotoOptionsDialogListener
        var captureSelectInx = -1
        var pickSelectIndex = -1
        val options = ArrayList<String>()
        val context = activity as Context

        if (canCapture(context)) {
            options.add("Camera")
            captureSelectInx = 0
        }
        if (canPick(context)) {
            options.add("Gallery")
            pickSelectIndex = if (captureSelectInx == 0) 1 else 0
        }
        return AlertDialog.Builder(context)
            .setTitle("Photo Option")
            .setItems(options.toTypedArray<CharSequence>()) { _,
                                                              which ->
                if (which == captureSelectInx) {
                    listener.onCaptureClick()
                } else if (which == pickSelectIndex) {
                    listener.onPickClick()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        fun canPick(context: Context): Boolean {
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            return (pickIntent.resolveActivity(context.packageManager) != null)
        }

        fun canCapture(context: Context): Boolean {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            return (captureIntent.resolveActivity(context.packageManager) != null)
        }

        fun newInstance(context: Context) = if (canPick(context) || canCapture(context)) {
            PhotoOptionsDialogFragment()
        } else {
            null
        }
    }
}