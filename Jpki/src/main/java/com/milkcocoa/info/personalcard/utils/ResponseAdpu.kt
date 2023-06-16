package com.milkcocoa.info.personalcard.utils

import com.hierynomus.asn1.ASN1InputStream
import com.hierynomus.asn1.encodingrules.der.DERDecoder
import java.io.ByteArrayInputStream
import java.lang.Exception

class ResponseAdpu(val rawData: ByteArray) {
    lateinit var data: ByteArray
    var SW1: Byte = 0x00
    var SW2: Byte = 0x00

    fun validate(sw1: Byte = 0x90.toByte(), sw2: Byte = 0x00): Boolean{
        val sw = rawData.takeLast(2)
        if(sw.get(0) != sw1 && sw.get(1) != sw2){
            return false
        }

        data = rawData.dropLast(2).toByteArray()
        SW1 = rawData.takeLast(2).first()
        SW2 = rawData.last()

        return true
    }

    fun asn1FrameIterator() = rawData.asn1FrameIterator()



    class ASN1Frame(
        val tag: Int,
        val length: Int,
        val frameSize: Int,
        val value: ByteArray? = null
    )

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