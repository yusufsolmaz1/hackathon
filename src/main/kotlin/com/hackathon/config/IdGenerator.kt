package com.hackathon.config

import java.security.SecureRandom

private val random = SecureRandom()
private val hexChars = "0123456789abcdef".toCharArray()

private fun randomHex(len: Int): String {
    val bytes = ByteArray((len + 1) / 2)
    random.nextBytes(bytes)
    val sb = StringBuilder(len)
    for (b in bytes) {
        sb.append(hexChars[(b.toInt() ushr 4) and 0x0F])
        sb.append(hexChars[b.toInt() and 0x0F])
    }
    return sb.substring(0, len)
}

fun newUserId() = "usr_" + randomHex(8)
fun newProductId() = "prd_" + randomHex(6)
fun newCollectionId() = "col_" + randomHex(8)
fun newFriendEdgeId() = "frn_" + randomHex(8)
fun newCartItemId() = "crt_" + randomHex(8)
fun newOrderId() = "ord_" + randomHex(8)
fun newNotificationId() = "ntf_" + randomHex(8)
fun newToken() = randomHex(48)

/** ord_xxx için human-readable order_number üretir. */
fun newOrderNumber(): String {
    val now = java.time.LocalDate.now()
    val datePart = "%04d%02d%02d".format(now.year, now.monthValue, now.dayOfMonth)
    val seq = (random.nextInt(900) + 100) // 100-999
    return "TS-$datePart-$seq"
}
