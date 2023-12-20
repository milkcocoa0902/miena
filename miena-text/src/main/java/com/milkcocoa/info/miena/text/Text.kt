package com.milkcocoa.info.miena.text

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.protocol.text.MienaText
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.entity.BasicInfo
import com.milkcocoa.info.miena.entity.CardSignature
import com.milkcocoa.info.miena.pin.Pin
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.Extension.toHexString
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep
import kotlin.math.sign

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

    override fun readBasicInfo(tag: Tag): BasicInfo {
        return tag.isoDep().critical {isoDep->
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

            var apInfo = ""
            var version = -1
            var extAdpu = -1
            var vendor = -1
            var option = -1
            var keyId = ""
            val readPersonalNumberResult = adpu.transceive(readBasicInfoAdpu)
            readPersonalNumberResult.asn1FrameIterator().forEach {
                when(it.tag){
                    65 ->{
                        it.value?.toHexString()?.apply { apInfo = this }
                        it.value?.getOrNull(0)?.toInt()?.apply { version = this }
                        it.value?.getOrNull(1)?.toInt()?.apply { extAdpu = this }
                        it.value?.getOrNull(2)?.toInt()?.apply { vendor = this }
                        it.value?.getOrNull(3)?.toInt()?.apply { option = this }
                    }
                    66 ->{
                        it.value?.toHexString()?.apply { keyId = this }
                    }
                    else ->{
                        // ??????
                        // Log.i("Padding?", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                }
            }

            return@critical BasicInfo(
                apInfo = apInfo,
                version = version,
                extAdpu = extAdpu,
                vendor = vendor,
                option = option,
                keyId = keyId
            )
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

    override fun readSignature(tag: Tag) : CardSignature{
        return tag.isoDep().critical {isoDep->
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

            var mynumDigest = ""
            var attrsDigest = ""
            var sig = ""
            certificateByteArray.asn1FrameIterator().forEach {
                when(it.tag){
                    49 ->{
                        it.value?.toHexString()?.apply { mynumDigest = this }
                    }
                    50 ->{
                        it.value?.toHexString()?.apply { attrsDigest = this }
                    }
                    51 ->{
                        it.value?.toHexString()?.apply { sig = this }
                    }
                    else ->{
                        // ??????
                        // Log.i("Padding?", it.value?.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) } ?: "")
                    }
                }
            }

            return@critical CardSignature(
                myNumberDigest = mynumDigest,
                attrsDigest = attrsDigest,
                signature = sig
            )
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