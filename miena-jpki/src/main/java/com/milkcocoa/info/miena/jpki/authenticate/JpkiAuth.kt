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
        tag.isoDep().critical {isoDep->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x0A.toByte())
            )
            Adpu(isoDep).transceive(selectFileAdpu)
        }
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

    override fun selectCertificatePin(tag: Tag){
        tag.isoDep().critical { isoDep ->
            val adpu = Adpu(isoDep)
            val selectFile = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x18)
            )
            adpu.transceive(selectFile)
        }
    }

    override fun selectCertificatePrivateKey(tag: Tag){
        tag.isoDep().critical {isoDep->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x17)
            )
            Adpu(isoDep).transceive(selectFileAdpu)
        }
    }


    // 認証用PINのロックを解除する
    override fun verifyPin(tag: Tag?, pin: DigitPin){
        requireNotNull(tag)

        tag.isoDep().critical {isoDep->
            val verifyAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0x20,
                P1 = 0x00,
                P2 = 0x80.toByte(),
                Lc = byteArrayOf(pin.length().toByte()),
                DF = pin.toByteArray()
            )
            Adpu(isoDep).transceive(verifyAdpu)
        }
    }


    override fun verifyCountRemains(tag: Tag): Int{
        return tag.isoDep().critical {isoDep->
            val verifyAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0x20,
                P1 = 0x00,
                P2 = 0x80.toByte()
            )
            val response = Adpu(isoDep).transceive(verifyAdpu, validate = false)


            // ロックまでの残回数を問い合わせるとき、コマンドの終端が変化する
            // 終端が[0x63, 0x6?]になり、?が残回数
            if(response.validate(sw1 = 0x63, sw2 = 0xC3.toByte())){
                return@critical 3
            }else if(response.validate(sw1 = 0x63, sw2 = 0xC2.toByte())){
                return@critical 2
            }else if (response.validate(sw1 = 0x63, sw2 = 0xC1.toByte())){
                return@critical 1
            }else if(response.validate(sw1 = 0x63, sw2 = 0xC0.toByte())){
                throw NoVerifyCountRemainsException("カードがロックされています")
            }else{
                throw NoVerifyCountRemainsException("カードがロックされています")
            }
        }
    }
}