package com.example.gasml.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gasml.model.GasStats
import com.example.gasml.ui.theme.*
import com.example.gasml.util.NetworkObserver
import com.example.gasml.viewmodel.AuthViewModel
import com.example.gasml.viewmodel.GasStatsViewModel
import com.example.gasml.viewmodel.OrderViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    statsViewModel: GasStatsViewModel,
    orderViewModel: OrderViewModel
) {
    val scrollState = rememberScrollState()
    val user = authViewModel.user
    val stats by statsViewModel.stats.collectAsState()
    val orders by orderViewModel.orders.collectAsState()
    val networkStatus by statsViewModel.networkStatus.collectAsState()
    val isLoading by statsViewModel.isLoading.collectAsState()

    val activeOrder = orders.firstOrNull { it.status != "Delivered" && it.status != "Cancelled" }

    LaunchedEffect(user?.unitId) {
        if (user?.unitId != null) {
            statsViewModel.loadStats(user.unitId)
        }
    }

    // UI FIXED: Removed internal Scaffold to prevent dual bottom navigation bars.
    // MainScreen provides the global Scaffold and bottom navigation.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (isLoading && stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else if (user?.unitId == null) {
            NoUnitBoundState(navController)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                HeaderSection(
                    userName = user.name,
                    currentDate = statsViewModel.getCurrentTime(),
                    networkStatus = networkStatus
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (activeOrder != null) {
                    ActiveOrderBanner(activeOrder) {
                        navController.navigate(Screen.Activity.route)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                CylinderCard(stats, statsViewModel, navController)
                Spacer(modifier = Modifier.height(24.dp))
                
                StatusBanner(stats, statsViewModel, user.unitId)
                
                Spacer(modifier = Modifier.height(16.dp))
                LeakSimulationCard(stats, statsViewModel, user.unitId)

                Spacer(modifier = Modifier.height(24.dp))
                SensorGridFlow(stats, statsViewModel)
                
                Spacer(modifier = Modifier.height(24.dp))
                UsageGraphCard(stats)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ActiveOrderBanner(order: com.example.gasml.model.Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, null, tint = PrimaryGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Ongoing Delivery", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Status: ${order.status}", style = Typography.labelSmall, color = PrimaryGreen)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
        }
    }
}

@Composable
fun LeakSimulationCard(stats: GasStats?, viewModel: GasStatsViewModel, unitId: String?) {
    val isLeak = stats?.leakDetected == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLeak) Color.Red.copy(alpha = 0.1f) else SurfaceColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Test Leak Alert", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (isLeak) "Simulation active. Stop to clear alert." else "Tap to simulate a gas leak notification.",
                    style = Typography.labelSmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = isLeak,
                onCheckedChange = { viewModel.toggleLeakSimulation(unitId, it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Red,
                    checkedTrackColor = Color.Red.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun NoUnitBoundState(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SettingsRemote, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("No hardware unit bound", style = Typography.headlineSmall, color = TextPrimary)
        Text(
            "Please bind your 3-digit unit code in settings or during registration to see live data.",
            style = Typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun CylinderCard(stats: GasStats?, viewModel: GasStatsViewModel, navController: NavController) {
    // Correctly coerced percentage for progress bar
    val percent = stats?.gasPercentage?.toInt()?.coerceIn(0, 100) ?: 0
    val daysRemaining by viewModel.daysRemaining.collectAsState()
    val refillDate by viewModel.refillDate.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF00695C), Color(0xFF003D33))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CylinderProgress(percent)
                    Spacer(modifier = Modifier.width(28.dp))
                    Column {
                        Text(
                            text = "LIVE UNIT: ${stats?.unitId ?: "..."}",
                            style = Typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stats?.currentWeight?.toString() ?: "0.0",
                                style = Typography.headlineMedium,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " kg / 5 kg",
                                style = Typography.bodyLarge,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ElectricBolt, null, tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Text(
                        text = " ~$daysRemaining days left • Predicted $refillDate",
                        style = Typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate(Screen.Order.route) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Text("Order refill →", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(userName: String, currentDate: String, networkStatus: NetworkObserver.Status) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp).background(PrimaryGreen, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocalFireDepartment, null, tint = DarkBackground, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Hi, $userName 👋", style = Typography.titleLarge, fontSize = 22.sp)
            Text(text = currentDate, style = Typography.bodyMedium, color = TextSecondary.copy(alpha = 0.7f) )
        }
        
        val networkColor = when(networkStatus) {
            NetworkObserver.Status.Available -> StatusGreen
            else -> Color.Red
        }
        
        Box(modifier = Modifier.size(44.dp).background(SurfaceColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (networkStatus == NetworkObserver.Status.Available) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = networkColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CylinderProgress(percent: Int) {
    Box(
        modifier = Modifier.size(width = 70.dp, height = 100.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight((percent / 100f).coerceIn(0f, 1f))
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
        )
        Text(text = "$percent%", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun StatusBanner(stats: GasStats?, viewModel: GasStatsViewModel, unitId: String?) {
    val isSafe = stats == null || stats.leakDetected == false
    val statusText = if (stats == null) "Connecting..." else if (isSafe) "Status: ${stats.systemStatus}" else "LEAK DETECTED!"
    val statusColor = if (stats == null) TextSecondary else if (isSafe) StatusGreen else Color.Red

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF081414))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSafe) Icons.Outlined.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText,
                        style = Typography.bodyLarge.copy(color = statusColor, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Last sync: ${stats?.timestamp?.let { viewModel.formatESP32Timestamp(it) } ?: "..."}",
                        style = Typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
            
            if (!isSafe) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.acknowledgeLeak(unitId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Acknowledge & Reset System", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorGridFlow(stats: GasStats?, viewModel: GasStatsViewModel) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = 2
    ) {
        val itemModifier = Modifier.weight(1f)
        // Fixed Leak Level coercion to avoid 255% or other invalid values
        SensorTile(
            Icons.Default.WaterDrop, 
            "Leak Level", 
            "${stats?.leakPercentage?.toInt()?.coerceIn(0, 100) ?: 0}%", 
            if (stats?.leakDetected == true) Color.Red else PrimaryGreen, 
            itemModifier
        )
        SensorTile(Icons.Default.Opacity, "Valve", if (stats?.valveClosed == true) "CLOSED" else "OPEN", if (stats?.valveClosed == true) Color.Red else SecondaryBlue, itemModifier)
        SensorTile(Icons.Default.DeviceThermostat, "Temp", "${stats?.temperature ?: 0.0}°C", TempOrange, itemModifier)
        SensorTile(Icons.Default.FlashOn, "System", stats?.systemStatus ?: "ONLINE", PrimaryGreen, itemModifier)
    }
}

@Composable
fun SensorTile(icon: ImageVector, label: String, value: String, iconColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(40.dp).background(iconColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(label, style = Typography.labelSmall, color = TextSecondary.copy(alpha = 0.7f))
            Text(value, style = Typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun UsageGraphCard(stats: GasStats?) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Weekly consumption", style = Typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
            Text("Weight History (kg)", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            UsageLineChart(listOf(5.0, 4.8, 4.7, 4.5, 4.4, 4.3, stats?.currentWeight ?: 4.2))
        }
    }
}

@Composable
fun UsageLineChart(data: List<Double>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        if (data.isNotEmpty()) {
            val path = Path()
            val xStep = size.width / (data.size - 1).coerceAtLeast(1)
            val maxVal = data.maxOrNull()?.coerceAtLeast(5.5) ?: 5.5
            val minVal = 0.0
            
            data.forEachIndexed { index, value ->
                val x = index * xStep
                val y = size.height - ((value - minVal) / (maxVal - minVal) * size.height).toFloat()
                if (index == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }
            drawPath(path = path, color = PrimaryGreen, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
