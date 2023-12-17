package com.milkcocoa.info.miena.adpu

import com.hierynomus.asn1.ASN1InputStream
import com.hierynomus.asn1.encodingrules.der.DERDecoder
import com.milkcocoa.info.miena.util.ASN1Frame
import com.milkcocoa.info.miena.util.Extension.asn1FrameIterator
import java.io.ByteArrayInputStream
import java.lang.Exception

data class ResponseAdpu(val rawData: ByteArray) {
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


}