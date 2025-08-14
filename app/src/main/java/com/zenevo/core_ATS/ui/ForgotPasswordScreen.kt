package com.zenevo.core_ATS.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.zenevo.core_ATS.security.PasswordResetManager
import com.zenevo.core_ATS.security.OTPVerificationResult
import com.zenevo.core_ATS.security.PasswordResetResult
import com.zenevo.core_ATS.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(ForgotPasswordStep.EMAIL) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val passwordResetManager = PasswordResetManager.getInstance(context)
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BaseWhite)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = when (currentStep) {
                    ForgotPasswordStep.EMAIL -> 0.33f
                    ForgotPasswordStep.OTP -> 0.66f
                    ForgotPasswordStep.NEW_PASSWORD -> 1.0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                color = BrandPrimary,
                trackColor = Gray200
            )
            
            // Step indicator
            Text(
                text = when (currentStep) {
                    ForgotPasswordStep.EMAIL -> "Step 1: Enter Email"
                    ForgotPasswordStep.OTP -> "Step 2: Verify OTP"
                    ForgotPasswordStep.NEW_PASSWORD -> "Step 3: Set New Password"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            when (currentStep) {
                ForgotPasswordStep.EMAIL -> {
                    EmailStep(
                        email = email,
                        onEmailChange = { email = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onRequestReset = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                val result = passwordResetManager.requestPasswordReset(email)
                                
                                when (result) {
                                    is PasswordResetResult.Success -> {
                                        successMessage = result.message
                                        currentStep = ForgotPasswordStep.OTP
                                    }
                                    is PasswordResetResult.Error -> {
                                        errorMessage = result.message
                                    }
                                    is PasswordResetResult.Blocked -> {
                                        errorMessage = result.message
                                    }
                                }
                                
                                isLoading = false
                            }
                        }
                    )
                }
                
                ForgotPasswordStep.OTP -> {
                    OTPStep(
                        email = email,
                        otp = otp,
                        onOtpChange = { otp = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onVerifyOTP = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                val result = passwordResetManager.verifyOTP(email, otp)
                                
                                when (result) {
                                    is OTPVerificationResult.Success -> {
                                        currentStep = ForgotPasswordStep.NEW_PASSWORD
                                    }
                                    is OTPVerificationResult.Error -> {
                                        errorMessage = result.message
                                    }
                                    is OTPVerificationResult.Blocked -> {
                                        errorMessage = result.message
                                    }
                                }
                                
                                isLoading = false
                            }
                        },
                        onResendOTP = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                val result = passwordResetManager.requestPasswordReset(email)
                                
                                when (result) {
                                    is PasswordResetResult.Success -> {
                                        successMessage = "New OTP sent to your email"
                                    }
                                    is PasswordResetResult.Error -> {
                                        errorMessage = result.message
                                    }
                                    is PasswordResetResult.Blocked -> {
                                        errorMessage = result.message
                                    }
                                }
                                
                                isLoading = false
                            }
                        }
                    )
                }
                
                ForgotPasswordStep.NEW_PASSWORD -> {
                    NewPasswordStep(
                        newPassword = newPassword,
                        onNewPasswordChange = { newPassword = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onSetNewPassword = {
                            scope.launch {
                                if (newPassword != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    return@launch
                                }
                                
                                if (newPassword.length < 8) {
                                    errorMessage = "Password must be at least 8 characters long"
                                    return@launch
                                }
                                
                                isLoading = true
                                errorMessage = null
                                
                                // Here you would typically call your API to update the password
                                // For now, we'll simulate success
                                kotlinx.coroutines.delay(1000)
                                
                                successMessage = "Password updated successfully!"
                                isLoading = false
                                
                                // Navigate back to login after a delay
                                kotlinx.coroutines.delay(2000)
                                navController.navigateUp()
                            }
                        }
                    )
                }
            }
            
            // Success message
            successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessBg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Success500
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            color = SuccessText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onRequestReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email",
            modifier = Modifier.size(64.dp),
            tint = BrandPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Enter your email address to receive a password reset OTP",
            fontSize = 16.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Email address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Gray500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Gray100,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedLabelColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )
        
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DangerBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = DangerText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        color = DangerText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestReset,
            enabled = email.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                disabledContainerColor = BrandPrimary.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send OTP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OTPStep(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onVerifyOTP: () -> Unit,
    onResendOTP: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "OTP",
            modifier = Modifier.size(64.dp),
            tint = BrandPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Enter the 6-digit OTP sent to\n$email",
            fontSize = 16.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter 6-digit OTP") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "OTP Icon", tint = Gray500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Gray100,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedLabelColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )
        
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DangerBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = DangerText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        color = DangerText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onVerifyOTP,
            enabled = otp.length == 6 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                disabledContainerColor = BrandPrimary.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Verify OTP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onResendOTP,
            enabled = !isLoading
        ) {
            Text("Resend OTP", color = BrandPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NewPasswordStep(
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onSetNewPassword: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = "New Password",
            modifier = Modifier.size(64.dp),
            tint = BrandPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Set your new password",
            fontSize = 16.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("New Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = Gray500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Gray100,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedLabelColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Confirm New Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon", tint = Gray500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Gray100,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedLabelColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )
        
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DangerBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = DangerText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        color = DangerText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSetNewPassword,
            enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                disabledContainerColor = BrandPrimary.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Set New Password", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

enum class ForgotPasswordStep {
    EMAIL, OTP, NEW_PASSWORD
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    CoreTheme {
        ForgotPasswordScreen(rememberNavController())
    }
}