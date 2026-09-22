package com.example.filrouge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filrouge.ui.theme.FilRougeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilRougeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Apply innerPadding so content does not overlap system bars
                    LoginForm(
                        onClick = { /* Handle login click */ },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usernameState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()

    Column(
        modifier = modifier.fillMaxSize().padding(60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mon réveil social", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(48.dp))
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo de l'app",
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.size(48.dp))

        TextField(
            state = usernameState,
            lineLimits = TextFieldLineLimits.SingleLine,
            placeholder = { Text("Enter Email Address") }
        )

        Spacer(Modifier.size(8.dp))

        TextField(
            state = passwordState,
            lineLimits = TextFieldLineLimits.SingleLine,
            placeholder = { Text("Enter Password") }
        )
        Text("Forgot your password?", fontSize = 10.sp, color = Color(100,100,100),textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.size(16.dp))

        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Sign In")
        }

        Spacer(Modifier.size(72.dp))

        Text(buildAnnotatedString {
            append("No account yet? ")
            withLink(
                LinkAnnotation.Url(
                    "https://youtu.be/dQw4w9WgXcQ?si=-5jJOXGnXRi6VKp_",
                    TextLinkStyles(style = SpanStyle(color = Color.Blue))
                )
            ) {
                append("Click here ")
            }
            append("to sign up.")
        },
            fontSize = 14.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun LoginFormPreview() {
    FilRougeTheme {
        LoginForm(onClick = {})
    }
}