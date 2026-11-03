package com.example.filrouge.alarms

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Modèle de données pour une invitation à une alarme partagée dans Firestore.
 */
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
    val status: String = "pending" // statuts possibles: "pending", "accepted", "declined"
) {
    /** Heure de l'alarme invitée au format HH:mm */
    val formattedTime: String
        get() = "%d:%02d".format(alarmHour, alarmMinute)

    /** Nom d'affichage de l'alarme invitée */
    val displayName: String
        get() = alarmName.ifBlank { "Alarme" }
}
