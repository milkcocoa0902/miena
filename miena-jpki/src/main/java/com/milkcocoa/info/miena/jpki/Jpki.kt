package com.milkcocoa.info.miena.jpki

import android.nfc.Tag
import com.milkcocoa.info.miena.MiinaJpki
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.pin.Pin
import com.milkcocoa.info.miena.pin.StrPin
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep

/**
 * Jpki
 * @author keita
 * @since 2023/12/17 13:50
 */

/**
 *
 */
abstract class Jpki<PIN: Pin> : MiinaJpki<PIN> {

    /**
     * @brief : send command to card for transition into JPKI mode
     * @param tag[Tag]: NFC card object
     */
    override fun selectJpki(tag: Tag) {
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
                    0xF0.toByte(),
                    0x00,
                    0x26,
                    0x01,
                    0x00,
                    0x00,
                    0x00,
                    0x01
                )
            )
            Adpu(isoDep).transceive(selectFileAdpu)
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
            for(cnt in 15.downTo(1)){
                if(response.validate(sw1 = 0x63, sw2 = (0xC0 + cnt).toByte())){
                    return@critical cnt
                }
            }
            throw NoVerifyCountRemainsException("カードがロックされています")
        }
    }
}