package com.milkcocoa.info.miena.util

import com.hierynomus.asn1.ASN1InputStream
import com.hierynomus.asn1.encodingrules.der.DERDecoder
import java.io.ByteArrayInputStream
import java.lang.Exception

object Extension {
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

    fun ByteArray.asn1FrameIterator(): Iterator<ASN1Frame> {
        return object: Iterator<ASN1Frame> {
            private val decoder = DERDecoder()
            private val byteArrayInputStream = ByteArrayInputStream(this@asn1FrameIterator)
            private val asn1InputStream = ASN1InputStream(decoder, byteArrayInputStream)

            override fun hasNext(): Boolean = byteArrayInputStream.available() > 0

            override fun next(): ASN1Frame {
                if (!hasNext()) throw NoSuchElementException()
                val tag = decoder.readTag(asn1InputStream)
                val length = decoder.readLength(asn1InputStream)
                val position = this@asn1FrameIterator.size - byteArrayInputStream.available()
                val frameSize = length + position
                val value: ByteArray? = try {
                    decoder.readValue(length, asn1InputStream)
                } catch (e: Exception) {
                    null
                }
                return ASN1Frame(tag.tag, length, frameSize, value)
            }
        }
    }
}

