package com.milkcocoa.info.miena.util

data class ASN1Frame(
    val tag: Int,
    val length: Int,
    val frameSize: Int,
    val value: ByteArray? = null
)