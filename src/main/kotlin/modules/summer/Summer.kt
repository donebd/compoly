package modules.summer

import api.Vk
import chatIds
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class Summer : modules.Module {
    override val callingType = 0
    override val millis = arrayOf(8 * 60 * 60L)
    override val name = "Дней до лета"
    override var lastCalling = 0L

    private val myFormat = SimpleDateFormat("dd MM yyyy")
    private val summerBegins = myFormat.parse("01 06 2020")

    override fun call() {
        val days = getDifferenceDays(Date(), summerBegins)
        if (days > 0) {
            Vk().send("☀Дней до начала лета: $days", chatIds)
        }
    }

    private fun getDifferenceDays(d1: Date, d2: Date): Long {
        val diff: Long = d2.getTime() - d1.getTime()
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    }
}

fun main() {
    Summer().call()
}