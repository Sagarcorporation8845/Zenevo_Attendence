package com.zenevo.core_ATS.security

import android.util.Log
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, SECURITY
}

enum class SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    PERMISSION_DENIED,
    ROLE_ACCESS_DENIED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_SUCCESS,
    PASSWORD_RESET_FAILED,
    SUSPICIOUS_ACTIVITY,
    RATE_LIMIT_EXCEEDED
}

data class SecurityLogEntry(
    val timestamp: Long,
    val userId: String?,
    val username: String?,
    val eventType: SecurityEventType,
    val details: String,
    val ipAddress: String?,
    val userAgent: String?,
    val severity: LogLevel,
    val sessionId: String?
)

data class RateLimitInfo(
    val count: AtomicInteger = AtomicInteger(0),
    val firstAttempt: Long = System.currentTimeMillis(),
    val lastAttempt: Long = System.currentTimeMillis()
)

class SecurityLogger private constructor() {
    
    companion object {
        private const val TAG = "SecurityLogger"
        private const val MAX_LOGS_IN_MEMORY = 1000
        private const val RATE_LIMIT_WINDOW_MS = 60000L // 1 minute
        private const val MAX_ATTEMPTS_PER_WINDOW = 5
        
        @Volatile
        private var INSTANCE: SecurityLogger? = null
        
        fun getInstance(): SecurityLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityLogger().also { INSTANCE = it }
            }
        }
    }
    
    private val logs = mutableListOf<SecurityLogEntry>()
    private val rateLimitMap = ConcurrentHashMap<String, RateLimitInfo>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun logSecurityEvent(
        eventType: SecurityEventType,
        userId: String? = null,
        username: String? = null,
        details: String,
        ipAddress: String? = null,
        userAgent: String? = null,
        severity: LogLevel = LogLevel.INFO,
        sessionId: String? = null
    ) {
        scope.launch {
            val key = "${username}_${eventType}_${ipAddress}"
            
            // Check rate limiting for permission denied events
            if (eventType == SecurityEventType.PERMISSION_DENIED || 
                eventType == SecurityEventType.ROLE_ACCESS_DENIED) {
                
                val rateLimitInfo = rateLimitMap.computeIfAbsent(key) { RateLimitInfo() }
                val currentTime = System.currentTimeMillis()
                
                // Reset counter if window has passed
                if (currentTime - rateLimitInfo.firstAttempt > RATE_LIMIT_WINDOW_MS) {
                    rateLimitMap[key] = RateLimitInfo()
                } else {
                    val currentCount = rateLimitInfo.count.incrementAndGet()
                    rateLimitInfo.lastAttempt = currentTime
                    
                    // If too many attempts, log only once and skip subsequent logs
                    if (currentCount > MAX_ATTEMPTS_PER_WINDOW) {
                        if (currentCount == MAX_ATTEMPTS_PER_WINDOW + 1) {
                            // Log rate limit exceeded event
                            addLogEntry(
                                SecurityLogEntry(
                                    timestamp = currentTime,
                                    userId = userId,
                                    username = username,
                                    eventType = SecurityEventType.RATE_LIMIT_EXCEEDED,
                                    details = "Rate limit exceeded for $eventType. User: $username, IP: $ipAddress",
                                    ipAddress = ipAddress,
                                    userAgent = userAgent,
                                    severity = LogLevel.WARNING,
                                    sessionId = sessionId
                                )
                            )
                        }
                        return@launch // Skip logging this event
                    }
                }
            }
            
            val logEntry = SecurityLogEntry(
                timestamp = System.currentTimeMillis(),
                userId = userId,
                username = username,
                eventType = eventType,
                details = details,
                ipAddress = ipAddress,
                userAgent = userAgent,
                severity = severity,
                sessionId = sessionId
            )
            
            addLogEntry(logEntry)
        }
    }
    
    private fun addLogEntry(entry: SecurityLogEntry) {
        synchronized(logs) {
            logs.add(entry)
            
            // Keep only the last MAX_LOGS_IN_MEMORY entries
            if (logs.size > MAX_LOGS_IN_MEMORY) {
                logs.removeAt(0)
            }
        }
        
        // Log to Android LogCat with appropriate level
        val logMessage = formatLogMessage(entry)
        when (entry.severity) {
            LogLevel.DEBUG -> Log.d(TAG, logMessage)
            LogLevel.INFO -> Log.i(TAG, logMessage)
            LogLevel.WARNING -> Log.w(TAG, logMessage)
            LogLevel.ERROR -> Log.e(TAG, logMessage)
            LogLevel.SECURITY -> Log.w(TAG, "SECURITY: $logMessage")
        }
    }
    
    private fun formatLogMessage(entry: SecurityLogEntry): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date(entry.timestamp))
        
        return "[$timestamp] ${entry.eventType.name} - User: ${entry.username ?: "Unknown"} " +
               "(${entry.userId ?: "N/A"}) - ${entry.details} - IP: ${entry.ipAddress ?: "N/A"}"
    }
    
    fun getRecentLogs(limit: Int = 50): List<SecurityLogEntry> {
        return synchronized(logs) {
            logs.takeLast(limit).reversed()
        }
    }
    
    fun getLogsByUser(username: String): List<SecurityLogEntry> {
        return synchronized(logs) {
            logs.filter { it.username == username }
        }
    }
    
    fun getLogsByEventType(eventType: SecurityEventType): List<SecurityLogEntry> {
        return synchronized(logs) {
            logs.filter { it.eventType == eventType }
        }
    }
    
    fun getLogsBySeverity(severity: LogLevel): List<SecurityLogEntry> {
        return synchronized(logs) {
            logs.filter { it.severity == severity }
        }
    }
    
    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
        rateLimitMap.clear()
    }
    
    fun getRateLimitStats(): Map<String, Int> {
        return rateLimitMap.mapValues { it.value.count.get() }
    }
}