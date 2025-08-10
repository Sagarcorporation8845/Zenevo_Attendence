package com.zenevo.core_ATS.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.zenevo.core_ATS.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    // State to toggle between geo-fence fail and camera view
    var isWithinGeofence by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Handle back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp)) // To balance the title
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseWhite)
            )
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController, currentRoute = "dashboard")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BaseWhite),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Welcome, Ethan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray800)
            Text(
                text = "Attendance time: 9:30 AM – 9:45 AM",
                color = Gray600,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isWithinGeofence) {
                SelfieCaptureView(navController)
            } else {
                GeoFenceFailedView(navController)
            }
        }
    }
}


@Composable
fun GeoFenceFailedView(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Gray100, RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Gray200, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location Icon", tint = Gray500, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You are not within the designated location.",
                    color = Gray700,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Button(
            onClick = { /* Disabled */ },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                disabledContainerColor = BrandPrimary.copy(alpha = 0.5f)
            )
        ) {
            Text("Mark Attendance", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SelfieCaptureView(navController: NavController) {
    var photoCaptured by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "09:00 AM", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Gray900)
            Text(text = "Monday, 24 July 2024", color = Gray500)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Position your face within the circle", color = Gray900)

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Gray800, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Gray400, modifier = Modifier.size(80.dp))
            }
        }


        Button(
            onClick = {
                if (!photoCaptured) {
                    photoCaptured = true
                } else {
                    navController.navigate("success") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (photoCaptured) Success500 else BrandPrimary
            )
        ) {
            if (!photoCaptured) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture Icon", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(Icons.Default.Check, contentDescription = "Mark Attendance Icon", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark Attendance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = BaseWhite,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            BottomNavItem("Home", Icons.Default.Home, "dashboard"),
            BottomNavItem("History", Icons.Default.History, "history"),
            BottomNavItem("Profile", Icons.Default.Person, "profile")
        )
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title, fontSize = 12.sp, fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Medium) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    unselectedIconColor = Gray500,
                    selectedTextColor = BrandPrimary,
                    unselectedTextColor = Gray500,
                    indicatorColor = Gray100
                )
            )
        }
    }
}

data class BottomNavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)


@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun DashboardScreenGeofenceFailedPreview() {
    CoreTheme {
        DashboardScreen(rememberNavController())
    }
}
