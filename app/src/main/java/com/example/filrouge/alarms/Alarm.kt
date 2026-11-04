package com.example.filrouge.alarms

import android.content.Context
import com.example.filrouge.R
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.time.LocalTime

/**
 * Modèle de données représentant une alarme stockée dans Firebase.
 * Contient la configuration temporelle, les sons/vibrations, la liste des participants
 * et l'URL facultative d'un Webhook Discord pour les notifications de retard.
 */
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
    val participantIds: List<String> = emptyList(),
    val discordWebhookUrl: String = ""
) {
    /** Heure formatée au format HH:mm pour l'interface UI */
    @get:Exclude
    val time: String get() = "%d:%02d".format(hour, minute)

    /** Résumé d'affichage des jours de répétition ou statut de la prochaine échéance */
    @get:Exclude
    val subtitle: String
        get() {
            if (days.isNotEmpty()) {
                val names = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                return days.sorted().joinToString(", ") { names[it - 1] }
            }
            return if (LocalTime.of(hour, minute).isAfter(LocalTime.now())) "aujourd'hui" else "demain"
        }

    /** Résumé d'affichage avec Context pour récupérer les chaînes traduites depuis strings.xml */
    fun getSubtitle(context: Context): String {
        if (days.isNotEmpty()) {
            val names = listOf(
                context.getString(R.string.day_lun),
                context.getString(R.string.day_mar),
                context.getString(R.string.day_mer),
                context.getString(R.string.day_jeu),
                context.getString(R.string.day_ven),
                context.getString(R.string.day_sam),
                context.getString(R.string.day_dim)
            )
            return days.sorted().joinToString(", ") { names[it - 1] }
        }
        return if (LocalTime.of(hour, minute).isAfter(LocalTime.now())) {
            context.getString(R.string.today)
        } else {
            context.getString(R.string.tomorrow)
        }
    }

    @get:Exclude
    val displayName: String get() = name.ifBlank { "Alarme" }

    fun getDisplayName(context: Context): String = name.ifBlank { context.getString(R.string.default_alarm_name) }
}
