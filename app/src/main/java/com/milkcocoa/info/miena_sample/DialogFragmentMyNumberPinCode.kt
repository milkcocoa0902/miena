package com.milkcocoa.info.miena_sample

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputEditText

/**
 * DialogFragmentPinCode
 * @author keita
 * @since 2023/12/17 16:55
 */

/**
 *
 */

class DialogFragmentMyNumberPinCode(private val listener: DialogFragmentMyNumberPinCodeListener): DialogFragment() {
    interface DialogFragmentMyNumberPinCodeListener{
        fun onCancel()
        fun onComplete(pin: String)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return layoutInflater.inflate(R.layout.dialog_fragment_input_mynumber_pincode, null, false)
            .apply {
                findViewById<Button>(R.id.ok).setOnClickListener {
                    dismiss()
                    listener.onComplete(findViewById<TextInputEditText>(R.id.pin_code).text?.toString() ?: "")
                }
                findViewById<Button>(R.id.cancel).setOnClickListener {
                    dismiss()
                    listener.onCancel()
                }
            }
            .run {
                return AlertDialog.Builder(requireContext()).setView(this).create()
        }
    }

    fun show(fm: FragmentManager){
        if(dialog?.isShowing != true){
            show(fm, DialogFragment::class.simpleName)
        }
    }
}