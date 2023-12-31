# 注意！！！！
⚠ 券面入力補助、パスワードを勘違いして間違えまくっている間にロックされて確認しきれなかったので、自己責任で！！！！！  
⚠ ネットに転がっている情報を参考に、仕様書などなにもない状態で作っているので、一部正確でないものが含まれている可能性があります。  

## Miena



マイナンバーカードをAndroidで扱うためのライブラリです。  
読み方は ミーナ (マイナンバー -> マイナ(my-num) -> ミーナ)  

✅ JPKI APの使用  
✅ 利用者証明用電子証明書の読み取り  
✅ 利用者証明用電子証明書を用いた署名生成  
✅ 署名用電子証明書の読み取り  
✅ 署名用電子証明書を用いた署名生成  
✅ カードロックまでの残回数の確認  
✅ 券面入力補助事項の取得  
✅ カード基本情報（Versionなど）の取得

## miena
Mienaのコア部分です。ADPUに関する部分とか、いろいろ。  
`common`とも言う

## miena-jpki
JPKI用のモジュールです。  
現状は usecaseでまとまっているものではなく、各操作（ファイル指定だったり）が別個になっているものです。  

## miena-text
券面入力事項支援のモジュールです。  
miena-jpki同様、ひとまずは操作のサポートです。  
動作確認もしきれていない。あと、取得しているデータが他のカードでも同じようにとれるのかわからない（break point置きながら頑張った）

## 使い方
例えば利用者証明用電子証明書で署名を生成するとき、マイナンバーカードの操作は以下の手順を踏む必要があります。  

1. JPKI APを選択する
2. 利用者証明用電子証明書のPINファイルを選択する
3. PINを照合する
4. 電子証明書の秘密鍵を選択する
5. 署名対象のデータを投げつける
6. 署名のデータを受け取る


現状のMienaでは、これら各工程の操作をサポートするライブラリとなっています。  
そのため、どのファイルを選択して、、、、みたいな順番を把握している必要はあります。  

コードで書くとざっくり以下の感じ  

```kotlin

NfcAdapter.ReaderCallback {tag->
    lifecycleScope.launch {
        runCatching {
            val jpki = JpkiAuth()
            
            // 1. まず、JPKI APを選択
            jpki.selectAP(tag = tag)

            // 2. 続いて、PINファイルを選択
            jpki.selectPin(tag = tag)
            
            // 3. 残回数を確認しておく
            // ロックされているとこのタイミングで NoVerifyCountRemainsExceptionが送出される
            val remains = jpki.verifyCountRemains(tag = tag)

            // 4. PINを照合する
            // pinはDigitPin(4桁)
            jpki.verifyPin(tag = tag, pin = pin)

            // 5. 秘密鍵を選択する
            jpki.selectCertificatePrivateKey(tag = tag)

            // 6. 秘密鍵で署名する
            // uri : 署名対象のファイル
            val signature = contentResolver.openInputStream(uri!!)?.use {inputStream->
                jpki.computeSignature(tag = tag, data = inputStream.readBytes())
            }
            createDocument.launch("signature.sig")?.let { contentResolver.openOutputStream(it) }?.use { outputStream->
                outputStream.write(signature)
                Log.i(TAG, "署名を保存しました")
            }
        }.getOrElse {
            when(it){
                is NoVerifyCountRemainsException ->{
                    Log.i(TAG, "カードがロックされています")
                }
                is AdpuValidateException ->{
                    Log.i(TAG, "カードとの通信に失敗しました")
                }
            }
        }.also {
            nfcCard.disableReaderMode(this)
        }
    }
}.let { nfcCallback->
    nfcCard.enableReaderMode(this, nfcCallback, NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
}
```
