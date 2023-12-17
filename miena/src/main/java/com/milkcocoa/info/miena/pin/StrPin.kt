package com.milkcocoa.info.miena.pin

import com.milkcocoa.info.miena.pin.Pin

/**
 * DigitPin
 * @author keita
 * @since 2023/12/17 13:13
 */

/**
 *
 */
class StrPin(code: String): Pin(code = code, min = 6, max = 16)