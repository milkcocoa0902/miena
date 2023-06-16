package com.milkcocoa.info.personalcard.utils

import java.security.MessageDigest

class Sha1(val data: ByteArray) {
    fun digest() = MessageDigest.getInstance("SHA-1").digest(data)

    fun digestInfo(): ByteArray{
        val old = byteArrayOf(0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a)
        val nl = byteArrayOf(0x05, 0x00)
        val seq = byteArrayOf().plus(0x30).plus((old.size + nl.size).toByte()).plus(old).plus(nl)
        val digest = byteArrayOf().plus(0x04).plus(digest().size.toByte()).plus(digest())

        return byteArrayOf().plus(0x30).plus((seq.size + digest.size).toByte()).plus(seq).plus(digest)
    }
}