package com.milkcocoa.info.miena_sample

import android.app.ProgressDialog
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.milkcocoa.info.miena.exception.AdpuValidateException
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.jpki.sign.JpkiSign
import com.milkcocoa.info.miena.pin.StrPin
import com.milkcocoa.info.miena.util.Sha1
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RequiresApi(Build.VERSION_CODES.N)
class JpkiSignActivity: AppCompatActivity(){
    val jpki by lazy { JpkiSign() }
    val nfcCard: NfcAdapter by lazy { NfcAdapter.getDefaultAdapter(applicationContext) }

    class SelectFile(val activity: AppCompatActivity, val mime: String = "*/*") {
        var continuation: CancellableContinuation<Uri?>? = null
        val selectFileLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()){result->
            continuation?.resume(result)
        }

        suspend fun launch(): Uri?{
            return suspendCancellableCoroutine {continuation->
                this.continuation = continuation
                selectFileLauncher.launch(mime)
                continuation.invokeOnCancellation {
                    this.continuation = null
                }
            }
        }
    }

    class CreateDocument(val activity: AppCompatActivity) {
        var continuation: CancellableContinuation<Uri?>? = null
        val createDocumentLauncher = activity.registerForActivityResult(ActivityResultContracts.CreateDocument()){uri->
            continuation?.resume(uri)
        }

        suspend fun launch(name: String): Uri?{
            return suspendCancellableCoroutine {continuation->
                this.continuation = continuation
                createDocumentLauncher.launch(name)
                continuation.invokeOnCancellation {
                    this.continuation = null
                }
            }
        }
    }


    lateinit var selectFile: SelectFile
    lateinit var createDocument: CreateDocument


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jpki)

        selectFile = SelectFile(this)

        createDocument = CreateDocument(this)

        val progressDialog = ProgressDialog(this).also {
            it.setMessage("マイナンバーカードをかざしてください")
        }



        // 証明書を取得する
        // 利用者認証とは異なり、署名用の公開鍵はロックがかかっている
        findViewById<Button>(R.id.read_certificate).also { readCertificace->
            readCertificace.setOnClickListener {
                lifecycleScope.launch {

                    val pin = getPinCode()
                    pin ?: return@launch

                    progressDialog.show()
                    NfcAdapter.ReaderCallback { tag->
                        lifecycleScope.launch {
                            kotlin.runCatching {
                                // 1. JPKI APを選択する
                                jpki.selectJpki(tag = tag)

                                // 2. PINを選択する
                                jpki.selectCertificatePin(tag = tag)

                                // 3. 残数を確認しておく
                                val remains = jpki.verifyCountRemains(tag = tag)
                                if(remains > 0){
                                    // 4. PINを照合する
                                    jpki.verifyPin(tag = tag, pin = pin)

                                    // 5. 公開鍵を選択する
                                    jpki.selectCertificatePublicKey(tag = tag)

                                    // 6. 公開鍵を読み取る
                                    val der = jpki.readCertificatePublicKey(tag = tag)

                                    createDocument.launch("jpki.der")?.let { contentResolver.openOutputStream(it) }?.use { outputStream->
                                        outputStream.write(der.encoded)
                                        Toast.makeText(this@JpkiSignActivity, "証明書を保存しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.getOrElse {
                                when(it){
                                    is NoVerifyCountRemainsException ->{
                                        Toast.makeText(this@JpkiSignActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                    }
                                    is AdpuValidateException ->{
                                        Toast.makeText(this@JpkiSignActivity, "カードの読み取りに失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.also {
                                nfcCard.disableReaderMode(this@JpkiSignActivity)

                                progressDialog.dismiss()
                            }

                        }
                    }.let { nfcCallback->
                        nfcCard.enableReaderMode(this@JpkiSignActivity, nfcCallback, NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                    }
                }
            }
        }



        findViewById<Button>(R.id.sign).also { computeSignature->
            var uri: Uri? = null
            computeSignature.setOnClickListener {
                uri = null

                lifecycleScope.launch {
                    uri = selectFile.launch()

                    if(uri == null){
                        return@launch
                    }
                    val pin = getPinCode()
                    pin ?: return@launch

                    progressDialog.show()


                    NfcAdapter.ReaderCallback {tag->
                        lifecycleScope.launch {

                            kotlin.runCatching {
                                // 1. JPKI APを選択する
                                jpki.selectJpki(tag = tag)

                                // 2. PINを選択する
                                jpki.selectCertificatePin(tag = tag)

                                // 3. 残回数を確認しておく
                                val remains = jpki.verifyCountRemains(tag = tag)
                                if(remains > 0){
                                    // 4. PINを照合する
                                    jpki.verifyPin(tag = tag, pin = pin)

                                    // 5. 秘密鍵を選択する
                                    jpki.selectCertificatePrivateKey(tag = tag)

                                    // 6. 署名を計算する
                                    val signature = this@JpkiSignActivity.contentResolver.openInputStream(uri!!)?.use {inputStream->
                                        jpki.computeSignature(tag = tag, data = inputStream.readBytes())
                                    }
                                    createDocument.launch("signature.sig")?.let { contentResolver.openOutputStream(it) }?.use { outputStream->
                                        outputStream.write(signature)
                                        Toast.makeText(this@JpkiSignActivity, "署名を保存しました", Toast.LENGTH_SHORT).show()
                                    }
                                }else{
                                    Toast.makeText(this@JpkiSignActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                }
                            }.getOrElse {
                                when(it){
                                    is NoVerifyCountRemainsException ->{
                                        Toast.makeText(this@JpkiSignActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                    }
                                    is AdpuValidateException ->{
                                        Toast.makeText(this@JpkiSignActivity, "カードの読み取りに失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.also {

                                nfcCard.disableReaderMode(this@JpkiSignActivity)
                                progressDialog.dismiss()
                            }

                        }
                    }.let { nfcCallback->
                        nfcCard.enableReaderMode(this@JpkiSignActivity, nfcCallback, NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                    }
                }
            }

        }


        findViewById<Button>(R.id.verify).also { verifySignature->
            verifySignature.setOnClickListener {
                lifecycleScope.launch {
                    // 今回はローカルに保存しておいた証明書を使うパターン
                    Toast.makeText(this@JpkiSignActivity, "証明書を選択してください", Toast.LENGTH_SHORT).show()
                    val cert = selectFile.launch() ?: return@launch

                    Toast.makeText(this@JpkiSignActivity, "検証したいファイルを選択してください", Toast.LENGTH_SHORT).show()
                    val file = selectFile.launch() ?: return@launch

                    Toast.makeText(this@JpkiSignActivity, "検証するファイルの署名を選択してください", Toast.LENGTH_SHORT).show()
                    val sig = selectFile.launch() ?: return@launch

                    val certificateByteArray = contentResolver.openInputStream(cert)?.use { inputStream->inputStream.readBytes() } ?: return@launch

                    // 証明書を読み込む
                    val x509 = CertificateFactory.getInstance("X.509").generateCertificate(
                        ByteArrayInputStream(certificateByteArray)
                    ) as X509Certificate

                    val data = contentResolver.openInputStream(file)?.use { inputStream->
                        Sha1(
                            inputStream.readBytes()
                        ).data } ?: return@launch
                    val signature = contentResolver.openInputStream(sig)?.use { inputStream->inputStream.readBytes() } ?: return@launch


                    // 検証する
                    val verify = Signature.getInstance("SHA1withRSA").apply {
                        initVerify(x509.publicKey)
                        update(data)
                    }.verify(signature)

                    if(verify){
                        Toast.makeText(this@JpkiSignActivity, "選択したファイルと署名は、証明書によりただしく検証されました", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this@JpkiSignActivity, "選択したファイルと署名が一致しませんでした", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * request 4 digit of pin-code
     */
    private suspend fun getPinCode(): StrPin?{
        return suspendCoroutine { continuation ->
            DialogFragmentLongPinCode(
                object : DialogFragmentLongPinCode.DialogFragmentLongPinCodeListener{
                    override fun onCancel() {
                        continuation.resume(null)
                    }

                    override fun onComplete(pin: String) {
                        if(pin.isNotBlank()){
                            continuation.resume(StrPin(pin))
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