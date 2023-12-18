package com.milkcocoa.info.miena.text.support.scope

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.MienaTextSupportBasicAttrs
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.pin.ComplexPin
import com.milkcocoa.info.miena.text.support.TextSupport
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
class TextSupportBasicAttrs: TextSupport<ComplexPin>(), MienaTextSupportBasicAttrs {
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
    override fun selectTextSupportPin(tag: Tag) {
        tag.isoDep().critical { isoDep ->
            val adpu = Adpu(isoDep)
            val selectFile = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x15)
            )
            adpu.transceive(selectFile)
        }
    }

    override fun selectBasicAttrs(tag: Tag) {
        tag.isoDep().critical {isoDep->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x02.toByte())
            )
            Adpu(isoDep).transceive(selectFileAdpu)
        }
    }
}