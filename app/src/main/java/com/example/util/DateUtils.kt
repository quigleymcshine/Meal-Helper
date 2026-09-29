package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    val DAYS_OF_WEEK = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday"
    )

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("MMM d", Locale.US)
    private val dayNameFormat = SimpleDateFormat("EEEE", Locale.US)

    fun getWeekStartDate(weekOffset: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return dbDateFormat.format(cal.time)
    }

    fun getWeekDisplayRange(weekOffset: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = displayFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = displayFormat.format(cal.time)

        return "$start – $end"
    }

    fun getTodayDayName(): String {
        return dayNameFormat.format(Calendar.getInstance().time)
    }
}
