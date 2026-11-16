package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.data.model.ProgressView
import com.trazo.android.ui.progress.formatKgText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressContractTest {
    @Test fun `the progress payload of the server is read as sent`() {
        val json = """
        {"summary":{"totalSessions":3,"sessionsLast30Days":3,"sessionsThisWeek":1,"weeklyGoal":4,"streakWeeks":2,"totalSets":30,"totalVolumeKg":5400},
         "weekStarts":["2026-11-09","2026-11-16"],
         "muscles":[{"muscle":"Chest","setsByWeek":[4.5,6]}],
         "exercises":[{"exerciseId":"bench","name":"Press banca","sessions":2,"lastDate":"2026-11-16","bestWeightKg":65,"bestEstimatedOneRepMaxKg":82.3,
           "recordDate":"2026-11-16","newRecord":true,
           "points":[{"date":"2026-11-09","topWeightKg":60,"repsAtTop":8,"estimatedOneRepMaxKg":76,"volumeKg":1440,"sets":3},
                     {"date":"2026-11-16","topWeightKg":65,"repsAtTop":8,"estimatedOneRepMaxKg":null,"volumeKg":1560,"sets":3}]}]}
        """
        val p = Gson().fromJson(json, ProgressView::class.java)
        assertEquals(2, p.summary.streakWeeks)
        assertEquals(6.0, p.muscles[0].setsByWeek[1], 0.0)
        assertTrue(p.exercises[0].newRecord)
        assertNull(p.exercises[0].points[1].estimatedOneRepMaxKg)
    }

    @Test fun `kilograms drop a useless decimal`() {
        assertEquals("65", formatKgText(65.0))
        assertEquals("–", formatKgText(null))
    }
}
