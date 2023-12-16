package com.milkcocoa.info.personalcard.utils

data class CommandAdpu(
    val CLA: Byte,
    val INS: Byte,
    val P1: Byte,
    val P2: Byte,
    val Lc: ByteArray = byteArrayOf(),
    val DF: ByteArray = byteArrayOf(),
    val Le: ByteArray = byteArrayOf()){
    fun toAdpu() = byteArrayOf(CLA, INS, P1, P2).plus(Lc).plus(DF).plus(Le)
}