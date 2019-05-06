package ninja.seibert.filegur.utils

import org.joda.time.DateTime

fun DateTime.toLocalDateTimeString(): String {
    return this.toLocalDateTime().toString("yyyy-MM-dd HH:mm:ss")
}