package com.milkcocoa.info.miena.text.scope

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.MienaTextBasicAttrs
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.pin.ComplexPin
import com.milkcocoa.info.miena.text.Text
import com.milkcocoa.info.miena.util.Extension.asn1FrameIterator
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep

/**
 * TextSupportBasicAttrs
 * @author keita
 * @since 2023/12/18 20:48
 */

/**
 *
 */
class TextBasicAttrs: Text<ComplexPin>(), MienaTextBasicAttrs {

    /**
     * basic attrs can not read mynumber
     */
    override fun readPersonalNumber(tag: Tag): PersonalNumber {
        TODO("Not yet implemented")
    }

    /**
     * basic attrs can not read my-number
     */
    override fun selectPersonalNumber(tag: Tag) {
        TODO("Not yet implemented")
    }

    override fun readBasicAttrs(tag: Tag): BasicAttrs {
        return tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)

            val preloadAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = byteArrayOf(0x03)
            )
            val preloadResponse = adpu.transceive(preloadAdpu)
            preloadResponse.validate()


            val readBasicAttrAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x03.toByte(), // 先頭の領域を読み飛ばす
                Le = preloadResponse.asn1FrameIterator().next().frameSize.toByteArray()
            )
            val readBasicAttrResult = adpu.transceive(readBasicAttrAdpu)



            var name: String = ""
            var address: String = ""
            var birth: String = ""
            var sex: String = ""
            readBasicAttrResult.rawData.asn1FrameIterator().forEach {
                when(it.tag){
                    33 -> Log.i("HEADER", it.value?.toList()?.toString() ?: "")
                    34 -> {
                        it.value?.apply { name = String(this) }
                    }
                    35 ->  {
                        it.value?.apply { address = String(this) }
                    }
                    36 ->  {
                        it.value?.apply { birth = String(this) }
                    }
                    37 ->  {
                        it.value?.apply { sex = String(this) }
                    }
                }
            }
            return@critical BasicAttrs(
                address = address,
                name = name,
                sex = sex,
                birthday = birth
            )
        }
    }

    override fun selectPin(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x15))
    }


    override fun selectBasicAttrs(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x02))
    }
}