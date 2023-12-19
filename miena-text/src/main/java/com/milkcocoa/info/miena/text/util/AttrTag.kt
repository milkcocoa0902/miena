package com.milkcocoa.info.miena.text.util

/**
 * Tag
 * @author keita
 * @since 2023/12/19 01:14
 */

/**
 *
 */
enum class AttrTag(val id: Int) {
    Header(33),
    Name(34),
    Address(35),
    Birthday(36),
    Sex(37);

    companion object{
        fun valueOf(id: Int) = values().find { it.id == id }
    }
}