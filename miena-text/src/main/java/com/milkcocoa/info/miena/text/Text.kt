package com.milkcocoa.info.miena.text

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.protocol.text.MienaText
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.pin.Pin
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep

/**
 * TextSupport
 * @author keita
 * @since 2023/12/18 20:52
 */

/**
 *
 */
abstract class Text<PIN: Pin>: MienaText<PIN> {
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
                Le = 0x05.toByteArray()
            )

            val preloadAdpuResult = adpu.transceive(preloadAdpu)

            val readBasicInfoAdpu =  CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x05,
                Le = preloadAdpuResult.asn1FrameIterator().next().frameSize.toByteArray()
            )

            val readPersonalNumberResult = adpu.transceive(readBasicInfoAdpu)
            readPersonalNumberResult.asn1FrameIterator().forEach {
                when(it.tag){
                    65 ->{
                        Log.i("APINFO", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                        Log.i("VERSION", it.value?.getOrNull(0)?.let { "%02X".format(it) } ?: "")
                        Log.i("ExtADPU", it.value?.getOrNull(1)?.let { "%02X".format(it) } ?: "")
                        Log.i("Vendor", it.value?.getOrNull(2)?.let { "%02X".format(it) } ?: "")
                        Log.i("Option", it.value?.getOrNull(3)?.let { "%02X".format(it) } ?: "")
                    }
                    66 ->{
                        Log.i("Key ID", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                    else ->{
                        // ??????
                        // Log.i("Padding?", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                }
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
                Le = byteArrayOf(0x00)
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

            // 何フォーマットかわからないが、一応読み込めている。
            // 読み込めているデータ、もしかしたら間違っているかもしれないが、
            // フォーマットがわからないのでどうしようもない
            certificateByteArray.asn1FrameIterator().forEach {
                Log.i("FRAME", "${it.tag} : ${it.value?.let { String(it) }}")
            }
        }
    }

    override fun selectSignature(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x03))
    }

    override fun readSignature(tag: Tag) {
        tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)

            // 5byte読みの根拠がわからん。
            // デバッグしてたら自分のカードだと5byteを境になんかあったのでひとまず
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
                when(it.tag){
                    49 ->{
                        Log.i("MyNumberDigest", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                    50 ->{
                        Log.i("AttrsDigest", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                    51 ->{
                        Log.i("Signature", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                    else ->{
                        // ??????
                        // Log.i("Padding?", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                }
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