package com.bignerdranch.android.criminalintent

import java.util.Date
import java.util.UUID

data class Crime(
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var details: String = "",
    var date: Date = Date(System.currentTimeMillis()),
    var isSolved: Boolean = false,
    var suspect: String = "",
    var phone: String = ""
)
