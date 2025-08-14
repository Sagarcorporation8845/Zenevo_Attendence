package com.zenevo.core_ATS.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class UserRole {
    EMPLOYEE,
    MANAGER,
    HR_MANAGER,
    ADMIN
}

enum class Permission {
    VIEW_DASHBOARD,
    MARK_ATTENDANCE,
    VIEW_HISTORY,
    VIEW_REPORTS,
    MANAGE_INVOICES,
    MANAGE_USERS,
    VIEW_SECURITY_LOGS,
    MANAGE_ROLES
}

data class User(
    val id: String,
    val username: String,
    val email: String,
    val role: UserRole,
    val permissions: Set<Permission>,
    val isActive: Boolean = true
)

data class AccessAttempt(
    val userId: String?,
    val username: String?,
    val permission: Permission?,
    val role: UserRole?,
    val timestamp: Long,
    val ipAddress: String?,
    val userAgent: String?,
    val success: Boolean,
    val reason: String?
)

class SecurityManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val MAX_ATTEMPTS_PER_MINUTE = 10
        private const val BLOCK_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        
        @Volatile
        private var INSTANCE: SecurityManager? = null
        
        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val securityLogger = SecurityLogger.getInstance()
    private val accessAttempts = ConcurrentHashMap<String, MutableList<AccessAttempt>>()
    private val blockedUsers = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Mock user data - in real app, this would come from a database
    private val mockUsers = mapOf(
        "sagar@zenevo.com" to User(
            id = "1",
            username = "Sagar",
            email = "sagar@zenevo.com",
            role = UserRole.ADMIN,
            permissions = setOf(
                Permission.VIEW_DASHBOARD,
                Permission.MARK_ATTENDANCE,
                Permission.VIEW_HISTORY,
                Permission.VIEW_REPORTS,
                Permission.MANAGE_INVOICES,
                Permission.MANAGE_USERS,
                Permission.VIEW_SECURITY_LOGS,
                Permission.MANAGE_ROLES
            )
        ),
        "rutvik@zenevo.com" to User(
            id = "2",
            username = "Rutvik Alhat",
            email = "rutvik@zenevo.com",
            role = UserRole.EMPLOYEE,
            permissions = setOf(
                Permission.VIEW_DASHBOARD,
                Permission.MARK_ATTENDANCE,
                Permission.VIEW_HISTORY
            )
        )
    )
    
    suspend fun checkPermission(
        username: String?,
        permission: Permission,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user is blocked
                if (username != null && isUserBlocked(username)) {
                    logAccessAttempt(
                        userId = null,
                        username = username,
                        permission = permission,
                        role = null,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        success = false,
                        reason = "User is temporarily blocked"
                    )
                    return@withContext false
                }
                
                // Rate limiting check
                if (username != null && isRateLimited(username)) {
                    blockUser(username)
                    logAccessAttempt(
                        userId = null,
                        username = username,
                        permission = permission,
                        role = null,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        success = false,
                        reason = "Rate limit exceeded"
                    )
                    return@withContext false
                }
                
                // Get user
                val user = mockUsers[username]
                if (user == null || !user.isActive) {
                    logAccessAttempt(
                        userId = null,
                        username = username,
                        permission = permission,
                        role = null,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        success = false,
                        reason = "User not found or inactive"
                    )
                    return@withContext false
                }
                
                // Check permission
                val hasPermission = user.permissions.contains(permission)
                
                logAccessAttempt(
                    userId = user.id,
                    username = username,
                    permission = permission,
                    role = user.role,
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = hasPermission,
                    reason = if (hasPermission) null else "Permission denied"
                )
                
                if (!hasPermission) {
                    // Log specific security events
                    securityLogger.logSecurityEvent(
                        eventType = SecurityEventType.PERMISSION_DENIED,
                        userId = user.id,
                        username = username,
                        details = "Access denied for permission: $permission",
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        severity = LogLevel.WARNING
                    )
                }
                
                hasPermission
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permission", e)
                false
            }
        }
    }
    
    suspend fun checkRoleAccess(
        username: String?,
        requiredRole: UserRole,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user is blocked
                if (username != null && isUserBlocked(username)) {
                    logAccessAttempt(
                        userId = null,
                        username = username,
                        permission = null,
                        role = requiredRole,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        success = false,
                        reason = "User is temporarily blocked"
                    )
                    return@withContext false
                }
                
                // Get user
                val user = mockUsers[username]
                if (user == null || !user.isActive) {
                    logAccessAttempt(
                        userId = null,
                        username = username,
                        permission = null,
                        role = requiredRole,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        success = false,
                        reason = "User not found or inactive"
                    )
                    return@withContext false
                }
                
                // Check role hierarchy
                val hasRoleAccess = when (requiredRole) {
                    UserRole.EMPLOYEE -> true
                    UserRole.MANAGER -> user.role in listOf(UserRole.MANAGER, UserRole.HR_MANAGER, UserRole.ADMIN)
                    UserRole.HR_MANAGER -> user.role in listOf(UserRole.HR_MANAGER, UserRole.ADMIN)
                    UserRole.ADMIN -> user.role == UserRole.ADMIN
                }
                
                logAccessAttempt(
                    userId = user.id,
                    username = username,
                    permission = null,
                    role = requiredRole,
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = hasRoleAccess,
                    reason = if (hasRoleAccess) null else "Role access denied"
                )
                
                if (!hasRoleAccess) {
                    securityLogger.logSecurityEvent(
                        eventType = SecurityEventType.ROLE_ACCESS_DENIED,
                        userId = user.id,
                        username = username,
                        details = "Role: $requiredRole",
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        severity = LogLevel.WARNING
                    )
                }
                
                hasRoleAccess
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking role access", e)
                false
            }
        }
    }
    
    private fun logAccessAttempt(
        userId: String?,
        username: String?,
        permission: Permission?,
        role: UserRole?,
        ipAddress: String?,
        userAgent: String?,
        success: Boolean,
        reason: String?
    ) {
        val attempt = AccessAttempt(
            userId = userId,
            username = username,
            permission = permission,
            role = role,
            timestamp = System.currentTimeMillis(),
            ipAddress = ipAddress,
            userAgent = userAgent,
            success = success,
            reason = reason
        )
        
        val key = username ?: "anonymous"
        accessAttempts.computeIfAbsent(key) { mutableListOf() }.add(attempt)
        
        // Keep only recent attempts
        val recentAttempts = accessAttempts[key]?.filter { 
            System.currentTimeMillis() - it.timestamp < 60000L // Last minute
        } ?: emptyList()
        accessAttempts[key] = recentAttempts.toMutableList()
    }
    
    private fun isRateLimited(username: String): Boolean {
        val attempts = accessAttempts[username] ?: return false
        val recentAttempts = attempts.filter { 
            System.currentTimeMillis() - it.timestamp < 60000L // Last minute
        }
        return recentAttempts.size >= MAX_ATTEMPTS_PER_MINUTE
    }
    
    private fun isUserBlocked(username: String): Boolean {
        val blockUntil = blockedUsers[username] ?: return false
        return System.currentTimeMillis() < blockUntil
    }
    
    private fun blockUser(username: String) {
        blockedUsers[username] = System.currentTimeMillis() + BLOCK_DURATION_MS
        
        securityLogger.logSecurityEvent(
            eventType = SecurityEventType.SUSPICIOUS_ACTIVITY,
            username = username,
            details = "User blocked due to rate limiting",
            severity = LogLevel.WARNING
        )
    }
    
    fun getUser(username: String?): User? {
        return mockUsers[username]
    }
    
    fun getAccessAttempts(username: String?): List<AccessAttempt> {
        return accessAttempts[username] ?: emptyList()
    }
    
    fun getBlockedUsers(): Map<String, Long> {
        return blockedUsers.toMap()
    }
    
    fun unblockUser(username: String) {
        blockedUsers.remove(username)
    }
    
    fun clearAccessAttempts() {
        accessAttempts.clear()
    }
    
    fun clearBlockedUsers() {
        blockedUsers.clear()
    }
}