package com.milkcocoa.info.miena

import android.nfc.Tag
import com.milkcocoa.info.miena.pin.Pin

/**
 * Miena
 * @author keita
 * @since 2023/12/19 02:24
 */

/**
 *
 */
interface Miena<PIN: Pin> {
    fun verifyPin(tag: Tag?, pin: PIN)
    fun verifyCountRemains(tag: Tag): Int
}