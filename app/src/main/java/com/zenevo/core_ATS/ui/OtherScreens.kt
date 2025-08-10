package com.zenevo.core_ATS.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.zenevo.core_ATS.R
import com.zenevo.core_ATS.ui.theme.*

// --- Attendance Success Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSuccessScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseWhite)
            )
        },
        bottomBar = {
            Column {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
                AppBottomNavigationBar(navController = navController, currentRoute = "dashboard")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BaseWhite),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .border(4.dp, Success500, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Success", tint = Success500, modifier = Modifier.size(80.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Attendance Marked", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray900)
            Text(
                text = "Your attendance has been successfully marked.",
                color = Gray700,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
            )
        }
    }
}


// --- History Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val historyItems = listOf(
        HistoryItem("July 22, 2024", "Marked", "9:00 AM", SuccessBg, SuccessText, Icons.Default.CheckCircle),
        HistoryItem("July 21, 2024", "Late", "9:15 AM", WarningBg, WarningText, Icons.Default.HourglassBottom),
        HistoryItem("July 20, 2024", "Absent", null, DangerBg, DangerText, Icons.Default.Cancel),
        HistoryItem("July 19, 2024", "Marked", "9:01 AM", SuccessBg, SuccessText, Icons.Default.CheckCircle),
        HistoryItem("July 18, 2024", "Marked", "9:03 AM", SuccessBg, SuccessText, Icons.Default.CheckCircle),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseWhite)
            )
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController, currentRoute = "history")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Gray50)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(historyItems) { item ->
                HistoryCard(item)
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BaseWhite, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(item.bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = item.status, tint = item.textColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.date, fontWeight = FontWeight.SemiBold, color = Gray800)
                Text(
                    text = item.status,
                    color = item.textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(item.bgColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        item.time?.let {
            Text(it, fontWeight = FontWeight.Medium, color = Gray600)
        }
    }
}

data class HistoryItem(val date: String, val status: String, val time: String?, val bgColor: Color, val textColor: Color, val icon: ImageVector)

// --- Profile Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseWhite)
            )
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController, currentRoute = "profile")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Gray100)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with a real profile image
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(Gray200),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ethan Carter", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray900)
            Text("Employee ID: 12345", color = Gray500)
            Text("ethan.carter@company.com", color = Gray500)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ACCOUNT SETTINGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray500,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .background(BaseWhite, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                ProfileMenuItem(
                    text = "Change Password",
                    icon = Icons.Default.VpnKey,
                    iconBgColor = Color(0xFFE0E7FF),
                    iconColor = Color(0xFF4338CA)
                )
                Divider(color = Gray200)
                ProfileMenuItem(
                    text = "Log Out",
                    icon = Icons.Default.Logout,
                    iconBgColor = Red100,
                    iconColor = Red500,
                    textColor = Red500
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItem(text: String, icon: ImageVector, iconBgColor: Color, iconColor: Color, textColor: Color = Gray900) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = text, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontWeight = FontWeight.Medium, color = textColor)
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Gray400, modifier = Modifier.size(16.dp))
    }
}


@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun SuccessScreenPreview() {
    CoreTheme {
        AttendanceSuccessScreen(rememberNavController())
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun HistoryScreenPreview() {
    CoreTheme {
        HistoryScreen(rememberNavController())
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun ProfileScreenPreview() {
    CoreTheme {
        ProfileScreen(rememberNavController())
    }
}
