package com.example.filrouge.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class AlarmInvitation(
    @DocumentId val id: String = "",
    val alarmId: String = "",
    val alarmName: String = "",
    val alarmHour: Int = 0,
    val alarmMinute: Int = 0,
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val status: String = "pending" // "pending", "accepted", "declined"
) {
    val formattedTime: String
        get() = "%d:%02d".format(alarmHour, alarmMinute)

    val displayName: String
        get() = alarmName.ifBlank { "Alarme" }
}
