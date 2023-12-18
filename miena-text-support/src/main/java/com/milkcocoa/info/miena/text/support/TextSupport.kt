package com.milkcocoa.info.miena.text.support

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.MienaTextSupport
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.pin.Pin
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
abstract class TextSupport<PIN: Pin>: MienaTextSupport<PIN> {
    override fun selectTextSupportPin(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun selectCertificate(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun readCertificate(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun selectSignature(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun readSignature(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun selectBasicInfo(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun readBasicInfo(tag: Tag) {
        TODO("Not yet implemented")
    }



    override fun selectTextSupport(tag: Tag) {
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
    override fun verifyCountRemains(tag: Tag): Int {
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

    override fun verifyPin(tag: Tag?, pin: PIN) {
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
}