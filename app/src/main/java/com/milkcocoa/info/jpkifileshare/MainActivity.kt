package com.milkcocoa.info.jpkifileshare

import android.app.ProgressDialog
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import com.milkcocoa.info.jpkifileshare.R
import com.milkcocoa.info.personalcard.ap.Jpki.Jpki
import com.milkcocoa.info.personalcard.utils.AdpuValidateException
import com.milkcocoa.info.personalcard.utils.NoVerifyCountRemainsException
import com.milkcocoa.info.personalcard.utils.Pin
import com.milkcocoa.info.personalcard.utils.Sha1
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RequiresApi(Build.VERSION_CODES.N)
class MainActivity: AppCompatActivity(){
    lateinit var jpki: Jpki
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
        setContentView(R.layout.activity_main)
        jpki = Jpki()

        selectFile = SelectFile(this)

        createDocument = CreateDocument(this)

        val progressDialog = ProgressDialog(this).also {
            it.setMessage("マイナンバーカードをかざしてください")
        }



        // 証明書を取得する
        findViewById<Button>(R.id.read_certificate).also { readCertificace->
            readCertificace.setOnClickListener {
                lifecycleScope.launch {

                    progressDialog.show()
                    NfcAdapter.ReaderCallback { tag->
                        lifecycleScope.launch {

                            kotlin.runCatching {
                                // JPKI APを選択して
                                jpki.prepare(tag = tag)

                                // 証明書を読み取る
                                val der = jpki.readCertificateAuth(tag = tag)

                                createDocument.launch("jpki.der")?.let { contentResolver.openOutputStream(it) }?.use { outputStream->
                                    outputStream.write(der.encoded)
                                    Toast.makeText(this@MainActivity, "証明書を保存しました", Toast.LENGTH_SHORT).show()
                                }
                            }.getOrElse {
                                when(it){
                                    is NoVerifyCountRemainsException ->{
                                        Toast.makeText(this@MainActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                    }
                                    is AdpuValidateException ->{
                                        Toast.makeText(this@MainActivity, "カードの読み取りに失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.also {
                                nfcCard.disableReaderMode(this@MainActivity)

                                progressDialog.dismiss()
                            }

                        }
                    }.let { nfcCallback->
                        nfcCard.enableReaderMode(this@MainActivity, nfcCallback, NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
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
                            jpki.prepare(tag = tag)
                            val remains = jpki.verifyCountRemains(tag = tag)
                            kotlin.runCatching {
                                if(remains > 0){
                                    jpki.verifyAuth(tag = tag, pin = pin)

                                    val signature = this@MainActivity.contentResolver.openInputStream(uri!!)?.use {inputStream->
                                        jpki.computeSignatureAuth(tag = tag, data = inputStream.readBytes())
                                    }
                                    Log.i("SIGNATURE", signature.toString())
                                    createDocument.launch("signature.sig")?.let { contentResolver.openOutputStream(it) }?.use { outputStream->
                                        outputStream.write(signature)
                                        Toast.makeText(this@MainActivity, "署名を保存しました", Toast.LENGTH_SHORT).show()
                                    }
                                }else{
                                    Toast.makeText(this@MainActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                }
                            }.getOrElse {
                                when(it){
                                    is NoVerifyCountRemainsException ->{
                                        Toast.makeText(this@MainActivity, "カードがロックされています", Toast.LENGTH_SHORT).show()
                                    }
                                    is AdpuValidateException ->{
                                        Toast.makeText(this@MainActivity, "カードの読み取りに失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.also {

                                nfcCard.disableReaderMode(this@MainActivity)
                                progressDialog.dismiss()
                            }

                        }
                    }.let { nfcCallback->
                        nfcCard.enableReaderMode(this@MainActivity, nfcCallback, NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                    }
                }
            }

        }


        findViewById<Button>(R.id.verify).also { verifySignature->
            verifySignature.setOnClickListener {
                lifecycleScope.launch {
                    // 今回はローカルに保存しておいた証明書を使うパターン
                    Toast.makeText(this@MainActivity, "証明書を選択してください", Toast.LENGTH_SHORT).show()
                    val cert = selectFile.launch() ?: return@launch

                    Toast.makeText(this@MainActivity, "検証したいファイルを選択してください", Toast.LENGTH_SHORT).show()
                    val file = selectFile.launch() ?: return@launch

                    Toast.makeText(this@MainActivity, "検証するファイルの署名を選択してください", Toast.LENGTH_SHORT).show()
                    val sig = selectFile.launch() ?: return@launch

                    val certificateByteArray = contentResolver.openInputStream(cert)?.use { inputStream->inputStream.readBytes() } ?: return@launch

                    // 証明書を読み込む
                    val x509 = CertificateFactory.getInstance("X.509").generateCertificate(
                        ByteArrayInputStream(certificateByteArray)
                    ) as X509Certificate

                    val data = contentResolver.openInputStream(file)?.use { inputStream->Sha1(inputStream.readBytes()).data } ?: return@launch
                    val signature = contentResolver.openInputStream(sig)?.use { inputStream->inputStream.readBytes() } ?: return@launch


                    // 検証する
                    val verify = Signature.getInstance("SHA1withRSA").apply {
                        initVerify(x509.publicKey)
                        update(data)
                    }.verify(signature)

                    if(verify){
                        Toast.makeText(this@MainActivity, "選択したファイルと署名は、証明書によりただしく検証されました", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this@MainActivity, "選択したファイルと署名が一致しませんでした", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * request 4 digit of pin-code
     */
    private suspend fun getPinCode(): Pin?{
        return suspendCoroutine { continuation ->

            val editText = AppCompatEditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                isSingleLine = true
                maxLines = 1
            }
            AlertDialog.Builder(this)
                .setTitle("PINの入力")
                .setMessage("4桁のPINを入力してください")
                .setView(editText)
                .setPositiveButton("OK") { dialog, _ ->
                    // OKボタンを押したときの処理
                    dialog.dismiss()


                    editText.text?.let {
                        if(it.isDigitsOnly()){
                            continuation.resume(Pin(it.toString()))
                            return@setPositiveButton
                        }
                        continuation.resume(null)
                        return@setPositiveButton
                    } ?: kotlin.run {
                        continuation.resume(null)
                        return@setPositiveButton
                    }
                }
                .setNegativeButton("キャンセル") { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(null)
                }
                .create()
                .show()
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