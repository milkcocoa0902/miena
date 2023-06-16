package com.milkcocoa.info.personalcard.utils

import android.nfc.tech.IsoDep
import java.lang.Exception

class Adpu(val isoDep: IsoDep) {
    fun transceive(command: CommandAdpu, sw1: Byte = 0x90.toByte(), sw2: Byte = 0x00, validate: Boolean = true): ResponseAdpu{
        val response = isoDep.transceive(command.toAdpu())
        val responseAdpu = ResponseAdpu(rawData = response)
        if(validate) {
            if(responseAdpu.validate(sw1, sw2).not()){
                throw AdpuValidateException("ADPUコマンドの結果が異常です")
            }
        }

        return responseAdpu
    }
}