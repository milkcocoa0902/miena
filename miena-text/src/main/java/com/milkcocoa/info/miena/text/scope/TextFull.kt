package com.milkcocoa.info.miena.text.scope

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.protocol.text.MienaTextFull
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.exception.IllegalTagException
import com.milkcocoa.info.miena.pin.DigitPin
import com.milkcocoa.info.miena.text.Text
import com.milkcocoa.info.miena.text.util.AttrTag
import com.milkcocoa.info.miena.util.Extension.asn1FrameIterator
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep

/**
 * TextSupportFull
 * @author keita
 * @since 2023/12/18 20:49
 */

/**
 *
 */
class TextFull: Text<DigitPin>(), MienaTextFull {
    override fun selectPin(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x11))
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

    override fun readBasicAttrs(tag: Tag) : BasicAttrs{
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
                when(AttrTag.valueOf(it.tag)){
                    AttrTag.Header -> Log.i("HEADER", it.value?.toList()?.toString() ?: "")
                    AttrTag.Name -> it.value?.apply { name = String(this) }
                    AttrTag.Address -> it.value?.apply { address = String(this) }
                    AttrTag.Birthday ->  it.value?.apply { birth = String(this) }
                    AttrTag.Sex -> it.value?.apply {
                        sex = String(this).let {
                            if(it == "1") "男性"
                            else if(it == "2") "女性"
                            else if(it == "9") "その他"
                            else "不明"
                        }
                    }
                    else -> Unit
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


    override fun selectBasicAttrs(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x02))
    }

    override fun selectPersonalNumber(tag: Tag) {
        selectEF(tag, byteArrayOf(0x00, 0x01))
    }
}