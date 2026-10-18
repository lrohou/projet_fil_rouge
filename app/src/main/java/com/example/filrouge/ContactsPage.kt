package com.example.filrouge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modèle de données pour un contact
data class Contact(
    val name: String,
    val alarmCount: Int
)

// Contenu de la page Contacts
@Composable
fun ContactsPageContent(
    isDarkTheme: Boolean = false
) {
    // Liste fictive de contacts
    val contacts = listOf(
        Contact("Gabriel", 1),
        Contact("Loan", 2),
        Contact("User 1", 1),
        Contact("User 2", 2)
    )

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Titre de la page
        Text(
            text = "Contacts",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Liste des contacts
        contacts.forEach { contact ->
            ContactCard(contact = contact, isDarkTheme = isDarkTheme)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton "Nouveau contact"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nouveau\ncontact",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    // Icône + dans un cercle bleu foncé
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Carte individuelle d'un contact
@Composable
fun ContactCard(
    contact: Contact,
    isDarkTheme: Boolean = false
) {
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color.Black
    val avatarBg = if (isDarkTheme) Color(0xFF2C3E50) else Color(0xFFE0E0E0)
    val avatarIcon = if (isDarkTheme) R.drawable.ic_person else R.drawable.ic_person_black

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône de profil dans un cercle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = avatarIcon),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nom et nombre de réveils actifs
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "${contact.alarmCount} réveil actif",
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            }

            // Icône horloge +
            Image(
                painter = painterResource(id = R.drawable.plus),
                contentDescription = "Ajouter alarme",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
