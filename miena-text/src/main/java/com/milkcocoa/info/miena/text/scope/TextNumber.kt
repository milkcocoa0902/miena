package com.milkcocoa.info.miena.text.scope

import android.nfc.Tag
import com.milkcocoa.info.miena.protocol.text.MienaTextNumber
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.exception.IllegalTagException
import com.milkcocoa.info.miena.pin.MyNumberPin
import com.milkcocoa.info.miena.text.Text
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep

/**
 * TextSupportNumber
 * @author keita
 * @since 2023/12/18 20:49
 */

/**
 *
 */
class TextNumber: Text<MyNumberPin>(), MienaTextNumber {
    /**
     * cannot access basic attrs
     */
    override fun readBasicAttrs(tag: Tag): BasicAttrs {
        TODO("Not yet implemented")
    }

    /**
     * cannot access basic attrs
     */
    override fun selectBasicAttrs(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun readPersonalNumber(tag: Tag): PersonalNumber {
        return tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)
            val readBasicAttrAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = 0x00.toByteArray()
            )
            val readPersonalNumberResult = adpu.transceive(readBasicAttrAdpu)
            val maybePersonalNumber = readPersonalNumberResult.asn1FrameIterator().next()
            maybePersonalNumber.tag.takeIf { it == 16 } ?: throw IllegalTagException("マイナンバーじゃない")

            return@critical PersonalNumber(String(maybePersonalNumber.value!!))
        }
    }

    override fun selectPin(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x14))
    }


    override fun selectPersonalNumber(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x01))
    }
}