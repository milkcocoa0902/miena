package com.milkcocoa.info.miena.protocol

import android.nfc.Tag
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.pin.Pin
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep

/**
 * Miena
 * @author keita
 * @since 2023/12/19 02:24
 */

/**
 *
 */
interface Miena<PIN: Pin> {
    fun selectAP(tag: Tag)
    fun selectPin(tag: Tag)

    fun selectEF(tag: Tag, addr: ByteArray){
        if(addr.size != 2) throw IllegalArgumentException("")

        tag.isoDep().critical { isoDep ->
            val adpu = Adpu(isoDep)
            val selectFile = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = addr
            )
            adpu.transceive(selectFile)
        }
    }

    fun verifyPin(tag: Tag?, pin: PIN){
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
    fun verifyCountRemains(tag: Tag): Int{
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
            for(cnt in 1.until(15)){
                if(response.validate(sw1 = 0x63, sw2 = (0xC0 + cnt).toByte())){
                    return@critical cnt
                }
            }
            throw NoVerifyCountRemainsException("カードがロックされています")
        }
    }
}