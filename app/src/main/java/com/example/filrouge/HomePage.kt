package com.example.filrouge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filrouge.ui.theme.FilRougeTheme

private val LightBlue = Color(0xFF90CAFF)
private val CardBlue = Color(0xFFD0EBFF)
private val DarkBlue = Color(0xFF0066B2)

class HomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilRougeTheme {
                Page()
            }
        }
    }
}

@Composable
fun Page() {
    Scaffold(
        bottomBar = { BottomNavBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkBlue, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkBlue, RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //box pour nouvelle alarme
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LightBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nouvelle\nAlarme",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 32.sp
                    )

                    //icone +
                    IconButton(
                        onClick = { /* Action au clic */ },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.plus),
                            contentDescription = "Ajouter une alarme",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alarmes en cours",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            //cartes pour alarmes
            AlarmCard(time = "07:00", subtitle = "aujourd’hui", initialChecked = true)
            Spacer(modifier = Modifier.height(17.dp))
            AlarmCard(time = "08:30", subtitle = "demain", initialChecked = false)
            Spacer(modifier = Modifier.height(17.dp))
            AlarmCard(time = "15:45", subtitle = "demain", initialChecked = true)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AlarmCard(time: String, subtitle: String, initialChecked: Boolean) {
    var isChecked by remember { mutableStateOf(initialChecked) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //case de l'heure
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Text(
                        text = time,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Black
                )
            }

            //switch
            Switch(
                checked = isChecked,
                onCheckedChange = { isChecked = it }
            )
        }
    }
}

@Composable
fun BottomNavBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LightBlue
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.group),
                    contentDescription = "Groupe",
                    modifier = Modifier.size(32.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.clock),
                    contentDescription = "Alarmes",
                    modifier = Modifier.size(36.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = "Paramètres",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PagePreview() {
    FilRougeTheme {
        Page()
    }
}