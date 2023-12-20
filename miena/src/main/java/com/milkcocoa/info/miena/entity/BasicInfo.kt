package com.milkcocoa.info.miena.entity

/**
 * BasicInfo
 * @author keita
 * @since 2023/12/20 23:21
 */

/**
 *
 */
data class BasicInfo(
    val apInfo: String,
    val version: Int,
    val extAdpu: Int,
    val vendor: Int,
    val option: Int,
    val keyId: String,
)
