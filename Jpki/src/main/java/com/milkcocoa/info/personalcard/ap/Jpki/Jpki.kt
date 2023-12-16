package com.milkcocoa.info.personalcard.ap.Jpki

import android.content.Context
import android.net.Uri
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.milkcocoa.info.personalcard.utils.*
import com.milkcocoa.info.personalcard.utils.Extension.Companion.hexByteArray
import java.io.ByteArrayInputStream
import com.milkcocoa.info.personalcard.utils.Extension.Companion.toByteArray
import java.io.FileInputStream
import java.lang.Exception
import java.security.AlgorithmConstraints
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class Jpki {
    private fun<T> IsoDep.critical(action: (IsoDep)->T): T{
        connect()
        val result = action(this)
        close()
        return result
    }


    /**
     * @brief : send command to card for transition into JPKI mode
     * @param tag[Tag]: NFC card object
     * @return [Boolean] true if success else failure
     */
private fun selectJpki(tag: Tag): Boolean{
    return IsoDep.get(tag).critical {isoDep->
        val selectFileAdpu = CommandAdpu(
            CLA = 0x00,
            INS = 0xA4.toByte(),
            P1 = 0x04,
            P2 = 0x0C,
            Lc = byteArrayOf(0x0A),
            DF = byteArrayOf(0xD3.toByte(), 0x92.toByte(), 0xF0.toByte(), 0x00, 0x26, 0x01, 0x00, 0x00, 0x00, 0x01))
        return@critical Adpu(isoDep).transceive(selectFileAdpu).validate()
    }
}


    private fun selectCertificateAuth(tag: Tag): Boolean{
        return IsoDep.get(tag).critical {isoDep->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x0A.toByte()))
            return@critical Adpu(isoDep).transceive(selectFileAdpu).validate()
        }
    }

    /**
     * @brief : send command to card for transition into JPKI mode
     * @param tag[Tag]: NFC card object
     *
     */
    fun prepare(tag: Tag): Boolean{
        return selectJpki(tag)
    }




    // 認証用の鍵を読み取る
    fun readCertificateAuth(tag: Tag): X509Certificate{
        selectCertificateAuth(tag)

        val response = IsoDep.get(tag).critical {isoDep->
            val adpu = Adpu(isoDep)

            val preloadAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = byteArrayOf(0x04))
            val preloadResponse = adpu.transceive(preloadAdpu)
            val preloadAsn1 = preloadResponse.asn1FrameIterator().next().frameSize

            Log.d("JPKI", preloadAsn1.toString())


            val readCertificateAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = preloadAsn1.toByteArray())
            val certificateByteArray = adpu.transceive(readCertificateAdpu)

            // X.509形式に変換
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certificateByteArray.data)) as X509Certificate
            Log.d("JPKI", cert.toString())
            return@critical cert
        }

        return response
    }

    /**
     * @brief : Calculate signature using certificate
     * @param tag[Tag] : NFC Tag object
     * @param data[ByteArray] : Calculation target file's data
     * @return [ByteArray] : Signature
     */
    fun computeSignatureAuth(tag: Tag, data: ByteArray): ByteArray{
        return IsoDep.get(tag).critical { isoDep ->
            isoDep.timeout = 5000

            val digestInfo = Sha1(data = data).digestInfo()
            val adpu = Adpu(isoDep)

            val selectFile = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x17))
            adpu.transceive(selectFile)


            val computeDigitalSignature = CommandAdpu(
                CLA = 0x80.toByte(),
                INS = 0x2A,
                P1 = 0x00,
                P2 = 0x80.toByte(),
                Lc = byteArrayOf(digestInfo.size.toByte()),
                DF = digestInfo,
                Le = byteArrayOf(0x00))
            val response = adpu.transceive(computeDigitalSignature)

            return@critical response.data
        }
    }


    // 認証用PINのロックを解除する
fun verifyAuth(tag: Tag?, pin: Pin){
    requireNotNull(tag)

    IsoDep.get(tag).critical {isoDep->
        val adpu = Adpu(isoDep)
        val selectFile = CommandAdpu(CLA = 0x00, INS = 0xA4.toByte(), P1 = 0x02, P2 = 0x0C, Lc = byteArrayOf(0x02), DF = byteArrayOf(0x00, 0x18))
        adpu.transceive(selectFile)


        val verifyAdpu = CommandAdpu(CLA = 0x00, INS = 0x20, P1 = 0x00, P2 = 0x80.toByte(), Lc = byteArrayOf(0x04), DF = pin.toByteArray())
        val response = adpu.transceive(verifyAdpu)

        Log.i("JPKI", response.rawData.contentToString())
    }
}


    fun verifyCountRemains(tag: Tag): Int{
        return IsoDep.get(tag).critical { isoDep ->
            val selectFile = CommandAdpu(CLA = 0x00, INS = 0xA4.toByte(), P1 = 0x02, P2 = 0x0C, Lc = byteArrayOf(0x02), DF = byteArrayOf(0x00, 0x18))
            val selectFileResponse = isoDep.transceive(selectFile.toAdpu())
            Log.i("JPKI", selectFileResponse.toString())


            val verifyAdpu = CommandAdpu(CLA = 0x00, INS = 0x20, P1 = 0x00, P2 = 0x80.toByte())
            val response = Adpu(isoDep).transceive(verifyAdpu, validate = false)

            Log.i("JPKI", response.toString())

            // ロックまでの残回数を問い合わせるとき、コマンドの終端が変化する
            // 終端が[0x63, 0x6?]になり、?が残回数
            if(response.validate(sw1 = 0x63, sw2 = 0xC3.toByte())){
                return@critical 3
            }else if(response.validate(sw1 = 0x63, sw2 = 0xC2.toByte())){
                return@critical 2
            }else if (response.validate(sw1 = 0x63, sw2 = 0xC1.toByte())){
                return@critical 1
            }else if(response.validate(sw1 = 0x63, sw2 = 0xC0.toByte())){
                return@critical 0
            }else{
                throw NoVerifyCountRemainsException("カードがロックされています")
            }
        }
    }
}