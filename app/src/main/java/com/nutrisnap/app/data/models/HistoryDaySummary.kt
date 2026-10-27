package com.nutrisnap.app.data.models

import java.time.LocalDate

data class HistoryDaySummary(
    val date: LocalDate,
    val foodCount: Int,
    val caloriesKcal: Double?,
    val proteinG: Double?
)
