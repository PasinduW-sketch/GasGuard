package com.example.gasml.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gasml.model.Order
import com.example.gasml.ui.theme.*
import com.example.gasml.viewmodel.AuthViewModel
import com.example.gasml.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DealerOrdersScreen(navController: NavController, orderViewModel: OrderViewModel = viewModel()) {
    val orders by orderViewModel.orders.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        orderViewModel.loadDealerOrders()
    }

    val activeOrders = orders.filter { it.status != "Delivered" }
    val completedOrders = orders.filter { it.status == "Delivered" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Order Management", style = Typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryGreen,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryGreen
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active (${activeOrders.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Completed", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val displayList = if (selectedTab == 0) activeOrders else completedOrders

        if (displayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (selectedTab == 0) "No active orders" else "No completed orders", color = TextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayList) { order ->
                    DealerOrderCard(order) { newStatus ->
                        orderViewModel.updateStatus(order.id, newStatus)
                    }
                }
            }
        }
    }
}

@Composable
fun DealerOrderCard(order: Order, onStatusChange: (String) -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateString = sdf.format(order.getEffectiveTimestamp().toDate())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(order.userName, style = Typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(order.cylinderType, style = Typography.labelSmall, color = TextSecondary)
                }
                Text("Rs. ${order.getEffectiveTotalPrice().toInt()}", style = Typography.bodyLarge, color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(order.address, style = Typography.labelSmall, color = TextSecondary.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
            }
            
            if (order.mapLocationUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(order.mapLocationUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View on Map", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status: ", style = Typography.labelSmall, color = TextSecondary)
                val statusColor = when(order.status) {
                    "Delivered" -> StatusGreen
                    "Out for Delivery" -> SignalBlue
                    "Pending" -> TempOrange
                    else -> PrimaryGreen
                }
                Text(order.status, style = Typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
            }
            
            if (order.status != "Delivered") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status == "Pending") {
                        StatusButton("Accept", PrimaryGreen, Modifier.weight(1f)) { onStatusChange("Processing") }
                    }
                    if (order.status == "Processing") {
                        StatusButton("Dispatch", SignalBlue, Modifier.weight(1f)) { onStatusChange("Out for Delivery") }
                    }
                    if (order.status == "Out for Delivery") {
                        StatusButton("Mark Delivered", StatusGreen, Modifier.weight(1f)) { onStatusChange("Delivered") }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DealerStockScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        Text("Inventory Management", style = Typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        
        StockItem("Standard 5 kg", 142)
        Spacer(modifier = Modifier.height(16.dp))
        StockItem("Small 2.5 kg", 45)
        Spacer(modifier = Modifier.height(16.dp))
        StockItem("Large 12.5 kg", 12)
    }
}

@Composable
fun StockItem(name: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(PrimaryGreen.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = PrimaryGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, modifier = Modifier.weight(1f), style = Typography.bodyLarge)
            Text("$count in stock", color = if (count > 20) PrimaryGreen else Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DealerFleetScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        Text("Fleet Tracking", style = Typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        
        FleetDriverItem("Arjun", "Online", "2.1 km away")
        Spacer(modifier = Modifier.height(16.dp))
        FleetDriverItem("Sana", "Delivering", "0.6 km away")
    }
}

@Composable
fun FleetDriverItem(name: String, status: String, distance: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(SecondaryBlue.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = SecondaryBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = Typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(distance, style = Typography.labelSmall, color = TextSecondary)
            }
            Text(status, color = if(status == "Online") PrimaryGreen else SignalBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DealerMeScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(100.dp).background(PrimaryGreen, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(50.dp), tint = DarkBackground)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(authViewModel.user?.name ?: "Dealer", style = Typography.headlineMedium)
        Text("ID: ${authViewModel.user?.uid?.takeLast(5) ?: "N/A"}", color = TextSecondary)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        ProfileOption("Business Profile", Icons.Default.Business)
        ProfileOption("Sales Reports", Icons.Default.BarChart)
        ProfileOption("Settings", Icons.Default.Settings)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { 
                authViewModel.logout {
                    navController.navigate(Screen.Login.route) { 
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileOption(title: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = Typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
