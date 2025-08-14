package com.zenevo.core_ATS.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.zenevo.core_ATS.security.SecurityEventType
import com.zenevo.core_ATS.security.SecurityLogger
import com.zenevo.core_ATS.security.LogLevel
import com.zenevo.core_ATS.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditScreen(navController: NavController) {
    var selectedFilter by remember { mutableStateOf<SecurityEventType?>(null) }
    var selectedSeverity by remember { mutableStateOf<LogLevel?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    val securityLogger = SecurityLogger.getInstance()
    val logs = remember { mutableStateListOf<com.zenevo.core_ATS.security.SecurityLogEntry>() }
    
    // Refresh logs periodically
    LaunchedEffect(Unit) {
        while (true) {
            val recentLogs = securityLogger.getRecentLogs(100)
            logs.clear()
            logs.addAll(recentLogs)
            kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
        }
    }
    
    val filteredLogs = logs.filter { log ->
        (selectedFilter == null || log.eventType == selectedFilter) &&
        (selectedSeverity == null || log.severity == selectedSeverity)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Audit Logs", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                    IconButton(onClick = { 
                        securityLogger.clearLogs()
                        logs.clear()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Gray50)
        ) {
            // Filter section
            if (showFilters) {
                FilterSection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    selectedSeverity = selectedSeverity,
                    onSeveritySelected = { selectedSeverity = it }
                )
            }
            
            // Stats section
            StatsSection(logs = logs)
            
            // Logs section
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        EmptyStateView()
                    }
                } else {
                    items(filteredLogs) { logEntry ->
                        SecurityLogCard(logEntry = logEntry)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    selectedFilter: SecurityEventType?,
    onFilterSelected: (SecurityEventType?) -> Unit,
    selectedSeverity: LogLevel?,
    onSeveritySelected: (LogLevel?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = BaseWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filters",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Event Type Filter
            Text(
                text = "Event Type",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray700
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onFilterSelected(null) },
                        label = { Text("All") }
                    )
                }
                
                items(SecurityEventType.values()) { eventType ->
                    FilterChip(
                        selected = selectedFilter == eventType,
                        onClick = { onFilterSelected(eventType) },
                        label = { Text(eventType.name.replace("_", " ")) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Severity Filter
            Text(
                text = "Severity",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray700
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSeverity == null,
                        onClick = { onSeveritySelected(null) },
                        label = { Text("All") }
                    )
                }
                
                items(LogLevel.values()) { severity ->
                    FilterChip(
                        selected = selectedSeverity == severity,
                        onClick = { onSeveritySelected(severity) },
                        label = { Text(severity.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSection(logs: List<com.zenevo.core_ATS.security.SecurityLogEntry>) {
    val totalLogs = logs.size
    val securityEvents = logs.count { it.severity == LogLevel.SECURITY }
    val warnings = logs.count { it.severity == LogLevel.WARNING }
    val errors = logs.count { it.severity == LogLevel.ERROR }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Total",
            value = totalLogs.toString(),
            color = BrandPrimary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Security",
            value = securityEvents.toString(),
            color = Red500,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Warnings",
            value = warnings.toString(),
            color = WarningText,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Errors",
            value = errors.toString(),
            color = DangerText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BaseWhite)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Gray600
            )
        }
    }
}

@Composable
fun SecurityLogCard(logEntry: com.zenevo.core_ATS.security.SecurityLogEntry) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val timestamp = dateFormat.format(Date(logEntry.timestamp))
    
    val severityColor = when (logEntry.severity) {
        LogLevel.DEBUG -> Gray500
        LogLevel.INFO -> BrandPrimary
        LogLevel.WARNING -> WarningText
        LogLevel.ERROR -> DangerText
        LogLevel.SECURITY -> Red500
    }
    
    val severityBgColor = when (logEntry.severity) {
        LogLevel.DEBUG -> Gray100
        LogLevel.INFO -> Color(0xFFE0E7FF)
        LogLevel.WARNING -> WarningBg
        LogLevel.ERROR -> DangerBg
        LogLevel.SECURITY -> Color(0xFFFEE2E2)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BaseWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timestamp,
                    fontSize = 12.sp,
                    color = Gray500
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = severityBgColor),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = logEntry.severity.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = logEntry.eventType.name.replace("_", " "),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )
            
            if (logEntry.username != null) {
                Text(
                    text = "User: ${logEntry.username}",
                    fontSize = 12.sp,
                    color = Gray600,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Text(
                text = logEntry.details,
                fontSize = 12.sp,
                color = Gray700,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            if (logEntry.ipAddress != null) {
                Text(
                    text = "IP: ${logEntry.ipAddress}",
                    fontSize = 11.sp,
                    color = Gray500,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "No Logs",
            modifier = Modifier.size(64.dp),
            tint = Gray400
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No security logs found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Gray600
        )
        
        Text(
            text = "Security events will appear here when they occur",
            fontSize = 14.sp,
            color = Gray500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SecurityAuditScreenPreview() {
    CoreTheme {
        SecurityAuditScreen(rememberNavController())
    }
}