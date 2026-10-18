package com.example.filrouge.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.time.LocalTime

@IgnoreExtraProperties
data class Alarm(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val participantIds: List<String> = emptyList()
) {
    @get:Exclude
    val time: String get() = "%d:%02d".format(hour, minute)

    @get:Exclude
    val subtitle: String
        get() = if (LocalTime.of(hour, minute).isAfter(LocalTime.now())) "aujourd'hui" else "demain"
}