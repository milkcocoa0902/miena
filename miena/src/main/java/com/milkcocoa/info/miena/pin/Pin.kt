package com.milkcocoa.info.miena.pin

import java.security.InvalidParameterException

abstract class Pin(val code: String, min: Int, max: Int){
    init{
        if(code.length < min || max < code.length){
            if(min == max){
                throw InvalidParameterException("PINは${min}桁で指定してください")
            }else{
                throw InvalidParameterException("PINは${min}桁~${max}桁で指定してください")
            }
        }
    }

    fun length() = code.length
    fun toByteArray() = code.toByteArray()
}