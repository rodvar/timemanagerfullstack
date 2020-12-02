package com.rodvar.timemanager.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private const val SYDNEY_PATTERN = "dd/MM/yyyy"
    private val TIMEZONE = TimeZone.getTimeZone("Australia/Sydney")
    private val calendar = Calendar.getInstance(TIMEZONE)
    private val simpleDateFormat = SimpleDateFormat(SYDNEY_PATTERN)

    fun now() = this.calendar.time

    fun tomorrow(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = now()
        calendar.add(Calendar.DATE, 1)
        return calendar.time
    }

    fun sameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1: Calendar = Calendar.getInstance()
        val cal2: Calendar = Calendar.getInstance()
        cal1.time = Date(timestamp1)
        cal2.time = Date(timestamp2)
        return cal1.get(Calendar.DAY_OF_YEAR) === cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) === cal2.get(Calendar.YEAR)
    }

    fun format(date: Date): String = this.simpleDateFormat.format(date)

    fun isToday(date: Date): Boolean {
        return DateUtils.isToday(date.time)
    }

    fun year(timestamp: Long): Int = Calendar.getInstance(TIMEZONE).let {
            it.time = Date(timestamp)
            it.get(Calendar.YEAR)
        }

    fun month(timestamp: Long): Int = Calendar.getInstance(TIMEZONE).let {
        it.time = Date(timestamp)
        it.get(Calendar.MONTH)
    }

    fun date(timestamp: Long): Int = Calendar.getInstance(TIMEZONE).let {
        it.time = Date(timestamp)
        it.get(Calendar.DAY_OF_MONTH)
    }

    fun toDate(year: Int, monthOfYear: Int, dayOfMonth: Int): Date {
        val newDate = Calendar.getInstance()
        newDate.set(year, monthOfYear, dayOfMonth)
        newDate.set(Calendar.HOUR, 0)
        newDate.set(Calendar.MINUTE, 0)
        newDate.set(Calendar.SECOND, 0)
        newDate.set(Calendar.MILLISECOND, 0)
        return newDate.time
    }

}