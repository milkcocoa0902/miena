package com.milkcocoa.info.miena

import android.nfc.Tag
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.pin.ComplexPin
import com.milkcocoa.info.miena.pin.DigitPin
import com.milkcocoa.info.miena.pin.MyNumberPin
import com.milkcocoa.info.miena.pin.Pin

/**
 * Miina
 * @author keita
 * @since 2023/12/17 13:53
 */

/**
 *
 */
interface MienaText<PIN: Pin>: Miena<PIN>{
    fun selectCertificate(tag: Tag)
    fun readCertificate(tag: Tag)

    fun selectSignature(tag: Tag)

    fun readSignature(tag: Tag)

    fun selectBasicInfo(tag: Tag)

    fun readBasicInfo(tag: Tag)

    fun selectPersonalNumber(tag: Tag)
    fun selectBasicAttrs(tag: Tag)
    fun readPersonalNumber(tag: Tag): PersonalNumber
    fun readBasicAttrs(tag: Tag): BasicAttrs

}

interface MienaTextNumber: MienaText<MyNumberPin>

interface MienaTextBasicAttrs: MienaText<ComplexPin>

interface MienaTextFull: MienaText<DigitPin>{
}