package com.example.filrouge.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.filrouge.R

/**
 * Composable réutilisable pour afficher l'avatar d'un utilisateur.
 * Gère automatiquement :
 * - Les images Base64 (sauvegardées directement dans Firestore)
 * - Les URLs web HTTP/HTTPS (ex: Google photo)
 * - L'initiale du pseudo en repli
 * - L'icône par défaut si aucun pseudo
 */
@Composable
fun UserAvatar(
    photoUrl: String,
    displayName: String,
    modifier: Modifier = Modifier,
    initialFontSize: TextUnit = 20.sp,
    iconSize: Dp = 24.dp
) {
    val bitmap = remember(photoUrl) {
        if (photoUrl.isNotBlank() && !photoUrl.startsWith("http")) {
            try {
                val clean = photoUrl.substringAfter("base64,")
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.cd_profile_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else if (photoUrl.isNotBlank() && photoUrl.startsWith("http")) {
        AsyncImage(
            model = photoUrl,
            contentDescription = stringResource(R.string.cd_profile_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        val initial = displayName.trim().take(1).uppercase()
        if (initial.isNotEmpty()) {
            Text(
                text = initial,
                fontSize = initialFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = stringResource(R.string.cd_profile_photo),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

