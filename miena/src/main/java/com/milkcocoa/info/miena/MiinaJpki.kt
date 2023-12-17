package com.milkcocoa.info.miena

import android.nfc.Tag
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
interface MiinaJpki<PIN : Pin> {
    fun selectJpki(tag: Tag)
    fun selectCertificatePublicKey(tag: Tag)
    fun readCertificatePublicKey(tag: Tag): X509Certificate
    fun computeSignature(tag: Tag, data: ByteArray): ByteArray
    fun selectCertificatePin(tag: Tag)
    fun selectCertificatePrivateKey(tag: Tag)
    fun verifyPin(tag: Tag?, pin: PIN)
    fun verifyCountRemains(tag: Tag): Int
}