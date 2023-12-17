package com.milkcocoa.info.miena.util

import android.nfc.Tag
import android.nfc.tech.IsoDep

/**
 * IsoDepUtil
 * @author keita
 * @since 2023/12/17 13:28
 */

/**
 *
 */
object IsoDepUtil {
 fun Tag.isoDep(timeout: Int = 1000) = IsoDep.get(this).apply { this.timeout = timeout }
 fun <T> IsoDep.critical(action: (IsoDep) -> T): T {
  return kotlin.runCatching {
   connect()
   val result: T = action(this)
   close()
   result
  }.getOrThrow().also {
   close()
  }
 }
}