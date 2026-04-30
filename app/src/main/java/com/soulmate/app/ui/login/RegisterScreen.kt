package com.soulmate.app.ui.login

import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soulmate.app.R
import com.soulmate.app.ui.theme.customGradient
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.authSuccess.collectLatest {
            onRegisterSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collectLatest { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
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
                .clip(RoundedCornerShape(bottomStart = 110.dp, bottomEnd = 0.dp))
                .background(customGradient)
                .padding(top = 45.dp, end = 52.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Join With Us !",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign Up",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(top = 160.dp, start = 24.dp, end = 24.dp, bottom = 20.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            elevation = 8.dp,
            backgroundColor = Color.White // Cố định nền Card màu trắng
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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    textStyle = TextStyle(color = Color.Black), // Cố định màu chữ đen
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF3fd6a7),
                        unfocusedBorderColor = Color.LightGray,
                        backgroundColor = Color.White,
                        textColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    textStyle = TextStyle(color = Color.Black),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF3fd6a7),
                        unfocusedBorderColor = Color.LightGray,
                        backgroundColor = Color.White,
                        textColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    textStyle = TextStyle(color = Color.Black),
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    textStyle = TextStyle(color = Color.Black),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acceptTerms,
                        onCheckedChange = { acceptTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2A7B9B),
                            uncheckedColor = Color.Gray
                        ),
                        enabled = !isLoading
                    )
                    Text(
                        text = "I accept the policy and terms",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Button(
                    onClick = {
                        if (acceptTerms) {
                            viewModel.register(name, email, password, confirmPassword)
                        } else {
                            Toast.makeText(context, "Vui lòng chấp nhận điều khoản", Toast.LENGTH_SHORT).show()
                        }
                    },
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
                        text = if (isLoading) "Creating account..." else "Sign up",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text(text = "Already have an account? ", fontSize = 14.sp, color = Color.DarkGray)
                    Text(
                        text = "Log in",
                        color = Color(0xFF2A7B9B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToLogin() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Signup Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    Text(
                        text = " Or sign up with ",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Social Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    RegisterSocialIcon(iconRes = R.drawable.google)
                    Spacer(modifier = Modifier.width(16.dp))
                    RegisterSocialIcon(iconRes = R.drawable.twitter)
                    Spacer(modifier = Modifier.width(16.dp))
                    RegisterSocialIcon(iconRes = R.drawable.facebook)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RegisterSocialIcon(iconRes: Int) {
    Card(
        modifier = Modifier
            .size(45.dp),
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
