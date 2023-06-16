package com.milkcocoa.info.personalcard.utils

import java.security.InvalidParameterException

class Pin(val code: String){
    init{
        if(code.length != 4){
            throw InvalidParameterException("PINは4桁で指定してください")
        }
    }

    fun toByteArray() = code.toByteArray()
}