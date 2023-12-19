package com.milkcocoa.info.miena.jpki.authenticate

import android.nfc.Tag
import android.util.Log
import java.io.ByteArrayInputStream
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.jpki.Jpki
import com.milkcocoa.info.miena.pin.DigitPin
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep
import com.milkcocoa.info.miena.util.Sha1
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.contracts.contract

class JpkiAuth : Jpki<DigitPin>(){
    override fun selectCertificatePublicKey(tag: Tag){
        selectEF(tag, byteArrayOf(0x00, 0x0A))
    }


    // 認証用の鍵を読み取る
    override fun readCertificatePublicKey(tag: Tag): X509Certificate{
        return tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)

            val preloadAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = byteArrayOf(0x04)
            )
            val preloadResponse = adpu.transceive(preloadAdpu)
            val preloadAsn1 = preloadResponse.asn1FrameIterator().next().frameSize



            val readCertificateAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = preloadAsn1.toByteArray()
            )
            val certificateByteArray = adpu.transceive(readCertificateAdpu)

            // X.509形式に変換
            CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certificateByteArray.data)) as X509Certificate
        }
    }

    /**
     * @brief : Calculate signature using certificate
     * @param tag[Tag] : NFC Tag object
     * @param data[ByteArray] : Calculation target file's data
     * @return [ByteArray] : Signature
     */
    override fun computeSignature(tag: Tag, data: ByteArray): ByteArray{
        return tag.isoDep(timeout = 5000).critical {isoDep->

            val digestInfo = Sha1(data = data).digestInfo()

            val computeDigitalSignature = CommandAdpu(
                CLA = 0x80.toByte(),
                INS = 0x2A,
                P1 = 0x00,
                P2 = 0x80.toByte(),
                Lc = byteArrayOf(digestInfo.size.toByte()),
                DF = digestInfo,
                Le = byteArrayOf(0x00)
            )
            val response = Adpu(isoDep).transceive(computeDigitalSignature)

            return@critical response.data
        }
    }

    override fun selectPin(tag: Tag){
        selectEF(tag, byteArrayOf(0x00, 0x18))
    }

    override fun selectCertificatePrivateKey(tag: Tag){
        selectEF(tag, byteArrayOf(0x00, 0x17))
    }
}