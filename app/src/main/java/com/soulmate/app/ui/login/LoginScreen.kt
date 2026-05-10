package com.soulmate.app.ui.login

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.soulmate.app.R
import com.soulmate.app.ui.theme.customGradient
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.authSuccess.collectLatest {
            onLoginSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collectLatest { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("17181834176-7gd70ksot6egk819h9j46fbqoroeerdq.apps.googleusercontent.com")
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Log.e("GoogleLogin", "Error while selecting the account: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(139.dp)
                .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 110.dp))
                .background(customGradient)
                .padding(top = 45.dp, start = 32.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Welcome Back !",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Log In",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(top = 180.dp, start = 24.dp, end = 24.dp, bottom = 40.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            elevation = 8.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Color(0xFF2A7B9B)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF3fd6a7),
                        unfocusedBorderColor = Color.LightGray,
                        backgroundColor = Color.White,
                        textColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF3fd6a7),
                        unfocusedBorderColor = Color.LightGray,
                        backgroundColor = Color.White,
                        textColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF2A7B9B),
                                uncheckedColor = Color.Gray
                            ),
                            enabled = !isLoading
                        )
                        Text(text = "Remember me", fontSize = 14.sp, color = Color.DarkGray)
                    }
                    Text(
                        text = "Forgot password?",
                        color = Color(0xFF2A7B9B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(enabled = !isLoading) { /* Forgot password logic */ }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(customGradient),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    elevation = null,
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (isLoading) "Logging in..." else "Log in",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text(text = "Don't have an account? ", fontSize = 14.sp, color = Color.DarkGray)
                    Text(
                        text = "Sign up",
                        color = Color(0xFF2A7B9B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToRegister() }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Social Login Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    Text(
                        text = " Or login with ",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Icons với ảnh thực tế
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SocialIcon(
                        iconRes = R.drawable.google,
                        onClick = { launcher.launch(googleSignInClient.signInIntent) })
                    Spacer(modifier = Modifier.width(16.dp))
                    SocialIcon(iconRes = R.drawable.twitter, onClick = {})
                    Spacer(modifier = Modifier.width(16.dp))
                    SocialIcon(iconRes = R.drawable.facebook, onClick = {})
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SocialIcon(iconRes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(45.dp)
            .clickable{onClick()},
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        backgroundColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
