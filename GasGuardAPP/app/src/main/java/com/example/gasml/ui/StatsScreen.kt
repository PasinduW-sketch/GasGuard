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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gasml.model.GasStats
import com.example.gasml.ui.theme.*
import com.example.gasml.viewmodel.AuthViewModel
import com.example.gasml.viewmodel.GasStatsViewModel

@Composable
fun StatsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    statsViewModel: GasStatsViewModel
) {
    val scrollState = rememberScrollState()
    val stats by statsViewModel.stats.collectAsState()
    val user = authViewModel.user
    val daysRemaining by statsViewModel.daysRemaining.collectAsState()
    val refillDate by statsViewModel.refillDate.collectAsState()

    // FIXED: Load stats using the user's unitId instead of their uid
    LaunchedEffect(user?.unitId) {
        user?.unitId?.let { statsViewModel.loadStats(it) }
    }

    // UI FIXED: Removed internal Scaffold to prevent "Screens Mix" with MainScreen's global Scaffold
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        StatsHeader(authViewModel, navController)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        RefillPredictionCard(
            daysRemaining = daysRemaining.toInt(),
            refillDate = refillDate,
            predictedUsage = if ((stats?.dailyUsage ?: 0.0) > 0.3) "High consumption" else "Normal consumption"
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        MetricsRow(stats, daysRemaining.toInt())
        
        Spacer(modifier = Modifier.height(24.dp))
        DailyUsageChartCard(stats)
        
        Spacer(modifier = Modifier.height(24.dp))
        MonthlyCostChartCard(stats)
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsHeader(authViewModel: AuthViewModel, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(PrimaryGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Usage & Insights",
                style = Typography.titleLarge,
                fontSize = 22.sp
            )
            Text(
                text = "Standard 5 kg Cylinder Analytics",
                style = Typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
        
        IconButton(
            onClick = {
                authViewModel.logout {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .size(44.dp)
                .background(Color.Red.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MetricsRow(stats: GasStats?, daysRemaining: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val currentDailyUsage = stats?.dailyUsage ?: 0.12
        val estimatedCost = stats?.estimatedCost ?: 1200.0

        MetricCard(
            label = "CURRENT DAILY USAGE",
            value = "${"%.2f".format(currentDailyUsage)} kg",
            icon = Icons.Default.TrendingDown,
            color = PrimaryGreen
        )
        MetricCard(
            label = "ESTIMATED MONTHLY COST",
            value = "Rs. ${"%,.0f".format(estimatedCost)}", 
            icon = Icons.Default.Payments,
            color = SecondaryBlue
        )
        MetricCard(
            label = "ESTIMATED DAYS LEFT",
            value = "$daysRemaining days",
            icon = Icons.Default.History,
            color = TempOrange
        )
    }
}

@Composable
fun DailyUsageChartCard(stats: GasStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Recent Usage Trend (kg)", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            val mockTrend = listOf(0.15, 0.2, 0.18, 0.22, 0.14, 0.25, stats?.dailyUsage ?: 0.19)
            DailyUsageLineChart(mockTrend)
        }
    }
}

@Composable
fun DailyUsageLineChart(dailyUsage: List<Double>) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val gridColor = Color.White.copy(alpha = 0.05f)
            val steps = 4
            for (i in 0..steps) {
                val y = height - (i * height / steps)
                drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
            }

            val path = Path()
            val xStep = width / (dailyUsage.size - 1)
            val maxY = dailyUsage.maxOrNull()?.coerceAtLeast(0.5) ?: 0.5
            
            dailyUsage.forEachIndexed { index, value ->
                val x = index * xStep
                val y = height - (value.toFloat() / maxY.toFloat() * height)
                if (index == 0) path.moveTo(x, y)
                else {
                    val prevX = (index - 1) * xStep
                    val prevY = height - (dailyUsage[index-1].toFloat() / maxY.toFloat() * height)
                    path.cubicTo(
                        prevX + xStep/2, prevY,
                        x - xStep/2, y,
                        x, y
                    )
                }
            }

            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(fillPath, brush = Brush.verticalGradient(colors = listOf(PrimaryGreen.copy(alpha = 0.3f), Color.Transparent)))
            drawPath(path, color = PrimaryGreen, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(top = 160.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(it, style = Typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun MonthlyCostChartCard(stats: GasStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Monthly cost (Rs.)", style = Typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            val mockMonthly = listOf(1200.0, 1150.0, 1250.0, 1100.0, 1300.0, stats?.estimatedCost ?: 1200.0)
            MonthlyCostBarChart(mockMonthly)
        }
    }
}

@Composable
fun MonthlyCostBarChart(monthlyCost: List<Double>) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = 40.dp.toPx()
            val spacing = (width - (monthlyCost.size * barWidth)) / (monthlyCost.size - 1).coerceAtLeast(1)
            val maxVal = monthlyCost.maxOrNull()?.coerceAtLeast(1500.0) ?: 1500.0
            
            monthlyCost.forEachIndexed { index, value ->
                val barHeight = (value.toFloat() / maxVal.toFloat()) * height
                drawRoundRect(
                    color = Color(0xFF42A5F5),
                    topLeft = Offset(index * (barWidth + spacing), height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(top = 160.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun").forEach {
                Text(it, style = Typography.labelSmall, color = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.width(40.dp))
            }
        }
    }
}
