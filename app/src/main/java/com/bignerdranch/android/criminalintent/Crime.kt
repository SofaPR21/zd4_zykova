package com.bignerdranch.android.criminalintent

import java.util.Date
import java.util.UUID

class Crime {
    val id: UUID = UUID.randomUUID()
    var title: String = ""
    var date: Date = Date(System.currentTimeMillis())
    var isSolved: Boolean = false
    var suspect: String = ""
    var suspectPhoneNumber: String = ""
}
