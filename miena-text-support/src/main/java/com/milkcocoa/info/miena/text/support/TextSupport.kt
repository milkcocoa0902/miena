package com.milkcocoa.info.miena.text.support

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.MienaTextSupport
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.exception.IllegalTagException
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.pin.Pin
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep
import com.milkcocoa.info.miena.util.Sha1
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * TextSupport
 * @author keita
 * @since 2023/12/18 20:52
 */

/**
 *
 */
abstract class TextSupport<PIN: Pin>: MienaTextSupport<PIN> {
    override fun selectBasicInfo(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x05))
    }

    override fun readBasicInfo(tag: Tag) {
        tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)
            val preloadAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = 0x04.toByteArray()
            )

            val preloadAdpuResult = adpu.transceive(preloadAdpu)

            val readBasicInfoAdpu =  CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = preloadAdpuResult.asn1FrameIterator().next().frameSize.toByteArray()
            )

            val readPersonalNumberResult = adpu.transceive(readBasicInfoAdpu)
            readPersonalNumberResult.asn1FrameIterator().forEach {
                Log.i("BASIC", String(it.value ?: byteArrayOf()))
            }
        }
    }

    override fun selectCertificate(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x04))
    }

    override fun readCertificate(tag: Tag) {
        tag.isoDep().critical {isoDep->
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
                P2 = 0x04,
                Le = preloadAsn1.toByteArray()
            )
            val certificateByteArray = adpu.transceive(readCertificateAdpu)

            // X.509形式に変換
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(
                ByteArrayInputStream(certificateByteArray.data)
            ) as X509Certificate

            Log.i("CERT", cert.toString())
        }
    }

    override fun selectSignature(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x03))
    }

    override fun readSignature(tag: Tag) {
        tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)

            val preloadAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = byteArrayOf(0x05)
            )
            val preloadResponse = adpu.transceive(preloadAdpu)
            val preloadAsn1 = preloadResponse.asn1FrameIterator().next().frameSize

            val readCertificateAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x05,
                Le = preloadAsn1.toByteArray()
            )
            val certificateByteArray = adpu.transceive(readCertificateAdpu)

            certificateByteArray.asn1FrameIterator().forEach {
                Log.i("TAG", it.tag.toString())
                it.value ?: return@forEach
                Log.i("digest", "${it.value?.toList()}")
            }
        }
    }


    final override fun selectAP(tag: Tag) {
        tag.isoDep().critical { isoDep ->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x04,
                P2 = 0x0C,
                Lc = byteArrayOf(0x0A),
                DF = byteArrayOf(
                    0xD3.toByte(),
                    0x92.toByte(),
                    0x10.toByte(),
                    0x00,
                    0x31,
                    0x00,
                    0x01,
                    0x01,
                    0x04,
                    0x08
                )
            )
            Adpu(isoDep).transceive(selectFileAdpu)
        }
    }
}