package com.milkcocoa.info.miena.entity

/**
 * CardSignature
 * @author keita
 * @since 2023/12/20 23:28
 */

/**
 *
 */
data class CardSignature(
    val myNumberDigest: String,
    val attrsDigest: String,
    val signature: String
)