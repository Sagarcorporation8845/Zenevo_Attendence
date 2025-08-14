package com.zenevo.core_ATS.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

data class PasswordResetAttempt(
    val email: String,
    val otp: String,
    val attempts: AtomicInteger = AtomicInteger(0),
    val firstAttempt: Long = System.currentTimeMillis(),
    val lastAttempt: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val blockUntil: Long = 0L
)

class PasswordResetManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PasswordResetManager"
        private const val OTP_LENGTH = 6
        private const val OTP_EXPIRY_MINUTES = 10
        private const val MAX_ATTEMPTS_PER_DAY = 3
        private const val BLOCK_DURATION_5MIN = 5 * 60 * 1000L
        private const val BLOCK_DURATION_1HOUR = 60 * 60 * 1000L
        private const val BLOCK_DURATION_1DAY = 24 * 60 * 60 * 1000L
        
        @Volatile
        private var INSTANCE: PasswordResetManager? = null
        
        fun getInstance(context: Context): PasswordResetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PasswordResetManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val resetAttempts = ConcurrentHashMap<String, PasswordResetAttempt>()
    private val securityLogger = SecurityLogger.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Email configuration (replace with your actual SMTP settings)
    private val emailProperties = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", "smtp.gmail.com") // Replace with your SMTP server
        put("mail.smtp.port", "587")
    }
    
    private val emailSession = Session.getInstance(emailProperties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication("noreply@zenevoinnovations.com", "your-app-password") // Replace with actual credentials
        }
    })
    
    suspend fun requestPasswordReset(email: String): PasswordResetResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user is blocked
                val currentAttempt = resetAttempts[email]
                if (currentAttempt != null && currentAttempt.isBlocked) {
                    val timeRemaining = currentAttempt.blockUntil - System.currentTimeMillis()
                    if (timeRemaining > 0) {
                        val minutesRemaining = (timeRemaining / (60 * 1000)).toInt()
                        return@withContext PasswordResetResult.Blocked(
                            "Account temporarily blocked. Please try again in $minutesRemaining minutes."
                        )
                    } else {
                        // Unblock user
                        resetAttempts.remove(email)
                    }
                }
                
                // Check daily limit
                if (currentAttempt != null) {
                    val attemptsToday = currentAttempt.attempts.get()
                    if (attemptsToday >= MAX_ATTEMPTS_PER_DAY) {
                        val blockUntil = currentAttempt.firstAttempt + BLOCK_DURATION_1DAY
                        resetAttempts[email] = currentAttempt.copy(
                            isBlocked = true,
                            blockUntil = blockUntil
                        )
                        
                        securityLogger.logSecurityEvent(
                            eventType = SecurityEventType.PASSWORD_RESET_FAILED,
                            username = email,
                            details = "Daily password reset limit exceeded",
                            severity = LogLevel.WARNING
                        )
                        
                        return@withContext PasswordResetResult.Blocked(
                            "Daily password reset limit exceeded. Please try again tomorrow."
                        )
                    }
                }
                
                // Generate OTP
                val otp = generateOTP()
                val attempt = PasswordResetAttempt(
                    email = email,
                    otp = otp,
                    attempts = AtomicInteger(1),
                    firstAttempt = System.currentTimeMillis(),
                    lastAttempt = System.currentTimeMillis()
                )
                
                resetAttempts[email] = attempt
                
                // Send email
                val emailSent = sendPasswordResetEmail(email, otp)
                
                if (emailSent) {
                    securityLogger.logSecurityEvent(
                        eventType = SecurityEventType.PASSWORD_RESET_REQUESTED,
                        username = email,
                        details = "Password reset OTP sent successfully",
                        severity = LogLevel.INFO
                    )
                    
                    PasswordResetResult.Success("Password reset OTP sent to your email")
                } else {
                    securityLogger.logSecurityEvent(
                        eventType = SecurityEventType.PASSWORD_RESET_FAILED,
                        username = email,
                        details = "Failed to send password reset email",
                        severity = LogLevel.ERROR
                    )
                    
                    PasswordResetResult.Error("Failed to send password reset email. Please try again.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting password reset", e)
                securityLogger.logSecurityEvent(
                    eventType = SecurityEventType.PASSWORD_RESET_FAILED,
                    username = email,
                    details = "Exception during password reset: ${e.message}",
                    severity = LogLevel.ERROR
                )
                
                PasswordResetResult.Error("An error occurred. Please try again.")
            }
        }
    }
    
    suspend fun verifyOTP(email: String, otp: String): OTPVerificationResult {
        return withContext(Dispatchers.IO) {
            try {
                val attempt = resetAttempts[email] ?: return@withContext OTPVerificationResult.Error("No password reset request found")
                
                // Check if user is blocked
                if (attempt.isBlocked) {
                    val timeRemaining = attempt.blockUntil - System.currentTimeMillis()
                    if (timeRemaining > 0) {
                        val minutesRemaining = (timeRemaining / (60 * 1000)).toInt()
                        return@withContext OTPVerificationResult.Blocked(
                            "Account temporarily blocked. Please try again in $minutesRemaining minutes."
                        )
                    }
                }
                
                // Check OTP expiry
                val otpAge = System.currentTimeMillis() - attempt.firstAttempt
                if (otpAge > OTP_EXPIRY_MINUTES * 60 * 1000) {
                    securityLogger.logSecurityEvent(
                        eventType = SecurityEventType.PASSWORD_RESET_FAILED,
                        username = email,
                        details = "OTP expired",
                        severity = LogLevel.WARNING
                    )
                    return@withContext OTPVerificationResult.Error("OTP has expired. Please request a new one.")
                }
                
                // Verify OTP
                if (attempt.otp == otp) {
                    // Success - remove the attempt
                    resetAttempts.remove(email)
                    
                    securityLogger.logSecurityEvent(
                        eventType = SecurityEventType.PASSWORD_RESET_SUCCESS,
                        username = email,
                        details = "OTP verified successfully",
                        severity = LogLevel.INFO
                    )
                    
                    OTPVerificationResult.Success
                } else {
                    // Increment attempt counter
                    val currentAttempts = attempt.attempts.incrementAndGet()
                    attempt.lastAttempt = System.currentTimeMillis()
                    
                    // Apply progressive blocking
                    val blockDuration = when {
                        currentAttempts == 2 -> BLOCK_DURATION_5MIN
                        currentAttempts == 3 -> BLOCK_DURATION_1HOUR
                        currentAttempts >= 4 -> BLOCK_DURATION_1DAY
                        else -> 0L
                    }
                    
                    if (blockDuration > 0) {
                        resetAttempts[email] = attempt.copy(
                            isBlocked = true,
                            blockUntil = System.currentTimeMillis() + blockDuration
                        )
                        
                        val minutesRemaining = (blockDuration / (60 * 1000)).toInt()
                        val message = when {
                            currentAttempts == 2 -> "Too many failed attempts. Account blocked for 5 minutes."
                            currentAttempts == 3 -> "Account blocked for 1 hour due to multiple failed attempts."
                            else -> "Account blocked for 24 hours due to repeated failed attempts."
                        }
                        
                        securityLogger.logSecurityEvent(
                            eventType = SecurityEventType.PASSWORD_RESET_FAILED,
                            username = email,
                            details = "OTP verification failed - attempt $currentAttempts. $message",
                            severity = LogLevel.WARNING
                        )
                        
                        OTPVerificationResult.Blocked(message)
                    } else {
                        securityLogger.logSecurityEvent(
                            eventType = SecurityEventType.PASSWORD_RESET_FAILED,
                            username = email,
                            details = "OTP verification failed - attempt $currentAttempts",
                            severity = LogLevel.WARNING
                        )
                        
                        OTPVerificationResult.Error("Invalid OTP. ${3 - currentAttempts} attempts remaining.")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying OTP", e)
                OTPVerificationResult.Error("An error occurred. Please try again.")
            }
        }
    }
    
    private fun generateOTP(): String {
        val random = Random()
        return (1..OTP_LENGTH).map { random.nextInt(10) }.joinToString("")
    }
    
    private suspend fun sendPasswordResetEmail(email: String, otp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val message = MimeMessage(emailSession).apply {
                    setFrom(InternetAddress("noreply@zenevoinnovations.com"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                    subject = "Password Reset Request - Zenevo Innovations"
                    setText("""
                        Hello,
                        
                        You have requested a password reset for your account.
                        
                        Your OTP is: $otp
                        
                        This OTP will expire in $OTP_EXPIRY_MINUTES minutes.
                        
                        If you did not request this password reset, please ignore this email.
                        
                        Best regards,
                        Zenevo Innovations Team
                    """.trimIndent())
                }
                
                Transport.send(message)
                Log.i(TAG, "Password reset email sent to $email")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send password reset email to $email", e)
                false
            }
        }
    }
    
    fun getResetAttempts(): Map<String, PasswordResetAttempt> {
        return resetAttempts.toMap()
    }
    
    fun clearResetAttempts() {
        resetAttempts.clear()
    }
}

sealed class PasswordResetResult {
    data class Success(val message: String) : PasswordResetResult()
    data class Error(val message: String) : PasswordResetResult()
    data class Blocked(val message: String) : PasswordResetResult()
}

sealed class OTPVerificationResult {
    object Success : OTPVerificationResult()
    data class Error(val message: String) : OTPVerificationResult()
    data class Blocked(val message: String) : OTPVerificationResult()
}