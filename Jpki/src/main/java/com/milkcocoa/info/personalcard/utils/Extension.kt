package com.milkcocoa.info.personalcard.utils

import com.hierynomus.asn1.ASN1InputStream
import com.hierynomus.asn1.encodingrules.der.DERDecoder
import java.io.ByteArrayInputStream

class Extension {
    companion object{

        fun String.hexByteArray(): ByteArray = chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        fun Int.toByteArray(): ByteArray {
            val value = this.toInt()

            val byteArray = ByteArray(3)


            val bytes = ByteArray(3)
            bytes[0] = ((value ushr 16) and 0xFF).toByte()
            bytes[1] = ((value ushr 8) and 0xFF).toByte()
            bytes[2] = (value and 0xFF).toByte()
            return bytes
        }
    }
}

class AdpuValidateException(override val message: String?): RuntimeException(message)
class NoVerifyCountRemainsException(override val message: String?): RuntimeException(message)