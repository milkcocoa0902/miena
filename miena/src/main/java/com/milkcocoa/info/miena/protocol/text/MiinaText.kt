package com.milkcocoa.info.miena.protocol.text

import android.nfc.Tag
import com.milkcocoa.info.miena.entity.BasicAttrs
import com.milkcocoa.info.miena.entity.BasicInfo
import com.milkcocoa.info.miena.entity.CardSignature
import com.milkcocoa.info.miena.entity.PersonalNumber
import com.milkcocoa.info.miena.pin.Pin
import com.milkcocoa.info.miena.protocol.Miena

/**
 * Miina
 * @author keita
 * @since 2023/12/17 13:53
 */

/**
 *
 */
interface MienaText<PIN: Pin>: Miena<PIN> {
    fun selectCertificate(tag: Tag)
    fun readCertificate(tag: Tag)

    fun selectSignature(tag: Tag)

    fun readSignature(tag: Tag): CardSignature

    fun selectBasicInfo(tag: Tag)

    fun readBasicInfo(tag: Tag): BasicInfo

    fun selectPersonalNumber(tag: Tag)
    fun selectBasicAttrs(tag: Tag)
    fun readPersonalNumber(tag: Tag): PersonalNumber
    fun readBasicAttrs(tag: Tag): BasicAttrs

}

