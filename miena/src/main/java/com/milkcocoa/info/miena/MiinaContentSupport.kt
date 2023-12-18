package com.milkcocoa.info.miena

import android.nfc.Tag
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.pin.ComplexPin
import com.milkcocoa.info.miena.pin.DigitPin
import com.milkcocoa.info.miena.pin.MyNumberPin
import com.milkcocoa.info.miena.pin.Pin
import java.security.cert.X509Certificate

/**
 * Miina
 * @author keita
 * @since 2023/12/17 13:53
 */

/**
 *
 */
interface MienaTextSupport<PIN: Pin>: Miena<PIN>{
    fun selectTextSupport(tag: Tag)
    fun selectTextSupportPin(tag: Tag)

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

interface MienaTextSupportNumber: MienaTextSupport<MyNumberPin>

interface MienaTextSupportBasicAttrs: MienaTextSupport<ComplexPin>

interface MienaTextSupportFull: MienaTextSupport<DigitPin>{
}