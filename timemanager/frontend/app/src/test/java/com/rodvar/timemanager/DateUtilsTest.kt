package com.rodvar.timemanager

import com.rodvar.timemanager.utils.DateUtils
import org.junit.Assert
import org.junit.Test

class DateUtilsTest {

    @Test
    fun sameDayTest() {
        Assert.assertTrue(DateUtils.sameDay(DateUtils.now().time, DateUtils.now().time))
    }

    @Test
    fun notSameDayTest() {
        Assert.assertFalse(DateUtils.sameDay(DateUtils.now().time, DateUtils.tomorrow().time))
    }

    @Test
    fun dateParams() {
        DateUtils.toDate(2020, 11, 12).let { date ->
            Assert.assertEquals(2020, DateUtils.year(date.time))
            Assert.assertEquals(11, DateUtils.month(date.time))
            Assert.assertEquals(12, DateUtils.date(date.time))
        }
    }
}