package com.milkcocoa.info.miena_sample

import android.app.ProgressDialog
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.milkcocoa.info.miena.exception.AdpuValidateException
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.pin.DigitPin
import com.milkcocoa.info.miena.text.support.scope.TextSupportFull
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RequiresApi(Build.VERSION_CODES.N)
class TextSupportBasicAttrsActivity: AppCompatActivity(){
    val textSuppprtBasicAttr by lazy { TextSupportFull() }
    val nfcCard: NfcAdapter by lazy { NfcAdapter.getDefaultAdapter(applicationContext) }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_support)

        val progressDialog = ProgressDialog(this).also {
            it.setMessage("マイナンバーカードをかざしてください")
        }



        // 証明書を取得する
        // 利用者認証とは異なり、署名用の公開鍵はロックがかかっている
        findViewById<Button>(R.id.read_attrs).also { readCertificace->
            readCertificace.setOnClickListener {
                lifecycleScope.launch {

                    val pin = getPinCode()
                    pin ?: return@launch
                    Log.i("PIN", pin.toString())

                    progressDialog.show()
                    NfcAdapter.ReaderCallback { tag->
                        lifecycleScope.launch {
                            kotlin.runCatching {
                                textSuppprtBasicAttr.selectTextSupport(tag = tag)
                                textSuppprtBasicAttr.selectTextSupportPin(tag = tag)
                                val remains = textSuppprtBasicAttr.verifyCountRemains(tag = tag)
                                Log.i("REMAINS", remains.toString())
                                if(remains > 0){
                                    // 4. PINを照合する
                                    textSuppprtBasicAttr.verifyPin(tag = tag, pin = pin)

                                    // 5. 公開鍵を選択する
                                    textSuppprtBasicAttr.selectBasicAttrs(tag = tag)
                                    textSuppprtBasicAttr.readBasicAttrs(tag = tag)
                                    textSuppprtBasicAttr.selectPersonalNumber(tag = tag)
                                    textSuppprtBasicAttr.readPersonalNumber(tag = tag)
                                }
                            }.getOrElse {
                                when(it){
                                    is NoVerifyCountRemainsException ->{
                                        Toast.makeText(this@TextSupportBasicAttrsActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                    }
                                    is AdpuValidateException ->{
                                        Toast.makeText(this@TextSupportBasicAttrsActivity, "カードの読み取りに失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.also {
                                nfcCard.disableReaderMode(this@TextSupportBasicAttrsActivity)

                                progressDialog.dismiss()
                            }

                        }
                    }.let { nfcCallback->
                        nfcCard.enableReaderMode(this@TextSupportBasicAttrsActivity, nfcCallback, NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                    }
                }
            }
        }
    }

    /**
     * request 4 digit of pin-code
     */
    private suspend fun getPinCode(): DigitPin?{
        return suspendCoroutine { continuation ->
            DialogFragmentPinCode(
                object : DialogFragmentPinCode.DialogFragmentPinCodeListener{
                    override fun onCancel() {
                        continuation.resume(null)
                    }

                    override fun onComplete(pin: String) {
                        if(pin.isNotBlank()){
                            continuation.resume(DigitPin(pin))
                        }else{
                            continuation.resume(null)
                        }
                    }
                }
            ).show(supportFragmentManager)
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

        nfcCard.disableReaderMode(this)
    }
}