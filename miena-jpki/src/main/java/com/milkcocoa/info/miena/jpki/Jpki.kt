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
    override fun selectAP(tag: Tag) {
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
}