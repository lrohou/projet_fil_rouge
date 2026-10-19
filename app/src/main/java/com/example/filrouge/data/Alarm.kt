package com.example.filrouge.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.time.LocalDate
import java.time.LocalTime

@IgnoreExtraProperties
data class Alarm(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val name: String = "",
    //jours de la semaine : 1=lundi .. 7=dimanche
    val days: List<Int> = emptyList(),
    val soundName: String = "aura_tone",
    val vibrate: Boolean = false,
    val participantIds: List<String> = emptyList()
) {
    @get:Exclude
    val time: String get() = "%d:%02d".format(hour, minute)

    @get:Exclude
    val subtitle: String
        get() {
            if (days.isNotEmpty()) {
                val names = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                return days.sorted().joinToString(", ") { names[it - 1] }
            }
            return if (LocalTime.of(hour, minute).isAfter(LocalTime.now())) "aujourd'hui" else "demain"
        }

    @get:Exclude
    val displayName: String get() = name.ifBlank { "Alarme" }
}