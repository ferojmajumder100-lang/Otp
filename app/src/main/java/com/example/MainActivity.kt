package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.api.LiveService
import com.example.data.db.ActiveNumber
import com.example.data.db.Saved2FASecret
import com.example.ui.ServicesUiState
import com.example.ui.VoltxViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoseGold
import com.example.ui.theme.VioletWine
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.MintFresh
import com.example.ui.theme.SoftPlum

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: VoltxViewModel = viewModel()
    var currentTab by remember { mutableIntStateOf(0) }
    val activeNumbers by viewModel.activeNumbers.collectAsState()
    val activeCount = activeNumbers.count { it.status == "ACTIVE" }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Get Number") },
                    label = { Text("Get Number") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RoseGold,
                        selectedTextColor = RoseGold,
                        indicatorColor = RoseGold.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Box {
                            Icon(Icons.Default.Inbox, contentDescription = "OTPs Inbox")
                            if (activeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, CircleShape)
                                        .size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = activeCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    label = { Text("Inbox") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VioletWine,
                        selectedTextColor = VioletWine,
                        indicatorColor = VioletWine.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "2FA Authenticator") },
                    label = { Text("2FA Vault") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Main Content Area based on Selected Tab
            when (currentTab) {
                0 -> GetNumberTab(viewModel)
                1 -> InboxTab(viewModel)
                2 -> TwoFactorTab(viewModel)
            }
        }
    }
}

// ============================================================================
// TAB 1: GET NUMBER
// ============================================================================
@Composable
fun GetNumberTab(viewModel: VoltxViewModel) {
    val servicesState by viewModel.servicesState.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val isPurchasing by viewModel.isPurchasing.collectAsState()
    val purchaseMessage by viewModel.purchaseMessage.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Brand Title
        item {
            Spacer(modifier = Modifier.height(16.dp))
            val artisticShape = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 8.dp, bottomStart = 8.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(artisticShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RoseGold, VioletWine, AmberGlow)
                        )
                    )
                    .padding(2.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 30.dp, bottomEnd = 30.dp, topEnd = 6.dp, bottomStart = 6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✧ ARAFAT OTP HUB ✧",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        letterSpacing = 2.sp,
                        color = RoseGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instant Virtual Numbers • Artfully Handled SMS Receiver",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Active notification overlays/alerts
        purchaseMessage?.let { msg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.contains("Successfully")) MintFresh.copy(alpha = 0.15f) else RoseGold.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, if (msg.contains("Successfully")) MintFresh else RoseGold),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (msg.contains("Successfully")) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Status",
                                tint = if (msg.contains("Successfully")) MintFresh else RoseGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.dismissPurchaseMessage() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Section: Live Services List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "1. Select Service",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    selectedService?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(RoseGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, RoseGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = it.sid,
                                color = RoseGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(onClick = { viewModel.fetchServices() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Services",
                        tint = RoseGold
                    )
                }
            }
        }

        // Beautiful Search Bar for Live Services
        item {
            val asymmetricCardShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search service (e.g. facebook, imo, telegram)...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_search_input"),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = RoseGold,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = asymmetricCardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoseGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Services representation based on loading state
        when (val state = servicesState) {
            is ServicesUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoseGold)
                    }
                }
            }
            is ServicesUiState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.Red,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.fetchServices() },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseGold)
                        ) {
                            Text("Retry Connection")
                        }
                    }
                }
            }
            is ServicesUiState.Success -> {
                val filteredServices = if (searchQuery.isBlank()) {
                    state.services
                } else {
                    state.services.filter {
                        it.sid.contains(searchQuery, ignoreCase = true)
                    }
                }

                item {
                    val asymmetricCardShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surface, asymmetricCardShape)
                            .padding(8.dp)
                    ) {
                        if (filteredServices.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching services found",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredServices) { service ->
                                    val isSelected = selectedService?.sid == service.sid
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(asymmetricCardShape)
                                            .background(
                                                if (isSelected) RoseGold.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.background
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) RoseGold else Color.Transparent,
                                                shape = asymmetricCardShape
                                            )
                                            .clickable { viewModel.selectService(service) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ServiceLogo(
                                            service = service.sid,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = service.sid,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) RoseGold else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${service.ranges?.size ?: 0} live ranges",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Live Ranges for Selected Service
        selectedService?.let { service ->
            item {
                Text(
                    text = "2. Select Range (Service: ${service.sid})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val ranges = service.ranges ?: emptyList()
            if (ranges.isEmpty()) {
                item {
                    Text(
                        text = "No live ranges active for this service.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                item {
                    val asymmetricCardShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surface, asymmetricCardShape)
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(ranges) { r ->
                                val isSelected = selectedRange == r
                                val countryInfo = getCountryInfo(r)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(asymmetricCardShape)
                                        .background(
                                            if (isSelected) VioletWine.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.background
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) VioletWine else Color.Transparent,
                                            shape = asymmetricCardShape
                                        )
                                        .clickable { viewModel.selectRange(r) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = countryInfo.first, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = r,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) VioletWine else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = service.sid,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) VioletWine.copy(alpha = 0.8f) else RoseGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (countryInfo.second.isNotEmpty()) {
                                            Text(
                                                text = countryInfo.second,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Purchase / Action Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val buttonShape = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 6.dp, bottomStart = 6.dp)
            Button(
                onClick = { viewModel.buyNumber() },
                enabled = !isPurchasing && selectedRange != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("buy_number_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoseGold,
                    disabledContainerColor = RoseGold.copy(alpha = 0.5f)
                ),
                shape = buttonShape
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Fetching from API...", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = "Buy", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedRange != null) "GET VIRTUAL NUMBER" else "SELECT RANGE FIRST",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Helper to match service names to dynamic visual emojis
fun getServiceIconEmoji(service: String): String {
    return when (service.lowercase()) {
        "facebook" -> "📘"
        "whatsapp" -> "🟢"
        "instagram" -> "📸"
        "telegram" -> "✈️"
        "tiktok" -> "🎵"
        "google" -> "🔴"
        else -> "🎯"
    }
}

@Composable
fun ServiceLogo(service: String, modifier: Modifier = Modifier) {
    val serviceLower = service.lowercase().trim()
    val shape = RoundedCornerShape(8.dp)
    
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            serviceLower.contains("facebook") || serviceLower == "fb" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1877F2)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "f",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.offset(x = 4.dp, y = 2.dp)
                    )
                }
            }
            serviceLower.contains("whatsapp") || serviceLower == "wa" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            serviceLower.contains("instagram") || serviceLower == "ig" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFCAF45),
                                    Color(0xFFF77737),
                                    Color(0xFFE1306C),
                                    Color(0xFFC13584),
                                    Color(0xFF833AB4)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.5.dp, Color.White, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(2.dp)
                                .background(Color.White, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
            serviceLower.contains("telegram") || serviceLower == "tg" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF229ED9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Telegram",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(-35f)
                            .offset(x = (-1).dp, y = 1.dp)
                    )
                }
            }
            serviceLower.contains("tiktok") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "♪",
                            color = Color(0xFF00F2FE),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 1.5.dp, bottom = 1.5.dp)
                        )
                        Text(
                            text = "♪",
                            color = Color(0xFFFE0979),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 1.5.dp, top = 1.5.dp)
                        )
                        Text(
                            text = "♪",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            serviceLower.contains("imo") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF00A2E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "imo",
                            color = Color(0xFF00A2E8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
            serviceLower.contains("google") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF4285F4),
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            serviceLower.contains("wechat") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF09BB07)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💬",
                        fontSize = 18.sp
                    )
                }
            }
            serviceLower.contains("tinder") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFE5068), Color(0xFFFF7854))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 18.sp
                    )
                }
            }
            serviceLower.contains("viber") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF7360F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Viber",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            serviceLower.contains("snapchat") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFC00)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👻",
                        fontSize = 18.sp
                    )
                }
            }
            serviceLower.contains("twitter") || serviceLower == "x" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "𝕏",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            serviceLower.contains("discord") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF5865F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎮",
                        fontSize = 18.sp
                    )
                }
            }
            serviceLower.contains("netflix") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        color = Color(0xFFE50914),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
            serviceLower.contains("signal") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF3A76F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💬",
                        fontSize = 18.sp
                    )
                }
            }
            serviceLower.contains("microsoft") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFF25022)))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF7FBA00)))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF00A4EF)))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFB900)))
                        }
                    }
                }
            }
            serviceLower.contains("yahoo") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF6001D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Y!",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            serviceLower.contains("amazon") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF131921)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "a",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
            serviceLower.contains("apple") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
            serviceLower.contains("bkash") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE2125B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "bKash",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            serviceLower.contains("nagad") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF15A22)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "নাগদ",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            serviceLower.contains("line") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF06C755)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LINE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            serviceLower.contains("kakao") -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFE812)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFF3C1E1E), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TALK",
                            color = Color(0xFFFFE812),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            else -> {
                val initial = if (service.isNotEmpty()) service.take(1).uppercase() else "S"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(RoseGold, VioletWine)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: INBOX & ACTIVE NUMBERS
// ============================================================================
@Composable
fun InboxTab(viewModel: VoltxViewModel) {
    val activeNumbers by viewModel.activeNumbers.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SMS OTP Inbox",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activeNumbers.size} total items",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (activeNumbers.isNotEmpty()) {
                Button(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear All", color = Color.Red, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (activeNumbers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "Empty",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Inbox is Empty",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Numbers and OTP messages will appear here",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeNumbers) { number ->
                    ActiveNumberCard(number, viewModel, context)
                }
            }
        }
    }
}

@Composable
fun ActiveNumberCard(number: ActiveNumber, viewModel: VoltxViewModel, context: Context) {
    val isOtpReceived = !number.otp.isNullOrBlank()

    // Pulse animation helper for active numbers
    val infiniteTransition = rememberInfiniteTransition()
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val artisticCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 6.dp, bottomStart = 6.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_number_card_${number.phone}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isOtpReceived) MintFresh.copy(alpha = 0.6f) else RoseGold.copy(alpha = 0.4f)
        ),
        shape = artisticCardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Service info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ServiceLogo(
                        service = number.service,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = number.service.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RoseGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = number.rangeCode,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Delete/Cancel button
                IconButton(
                    onClick = { viewModel.cancelNumber(number.phone) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phone Number Representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "+${number.phone}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("phone", number.phone)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Phone copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy phone",
                        tint = MintFresh,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )

            // Bottom Section: Status / OTP
            if (isOtpReceived) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MintFresh.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, MintFresh.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OTP RECEIVED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MintFresh,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("otp", number.otp)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "OTP copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy OTP",
                                tint = MintFresh,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = number.otp ?: "",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MintFresh,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = number.fullMessage ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alphaAnim),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = RoseGold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Waiting for OTP SMS...",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = RoseGold
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: 2FA AUTHENTICATOR VAULT
// ============================================================================
@Composable
fun InboxTabEmpty() {} // Deprecated dummy

@Composable
fun TwoFactorTab(viewModel: VoltxViewModel) {
    val savedSecrets by viewModel.savedSecrets.collectAsState()
    val totpMap by viewModel.totpMap.collectAsState()
    val totpProgress by viewModel.totpProgress.collectAsState()
    val context = LocalContext.current

    var isAdding by remember { mutableStateOf(false) }
    var labelInput by remember { mutableStateOf("") }
    var secretInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with button to open input form
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "2FA Authenticator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Local TOTP Generator",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(
                onClick = { isAdding = !isAdding },
                modifier = Modifier
                    .background(
                        if (isAdding) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isAdding) Icons.Default.Delete else Icons.Default.Add,
                    contentDescription = "Add 2FA token",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Expandable input form to add tokens
        AnimatedVisibility(visible = isAdding) {
            val inputCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 6.dp, bottomStart = 6.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = inputCardShape,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ADD NEW 2FA SECRET",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Account Label (e.g. Facebook)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("label_input_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = { Text("Secret Key (Base32 String)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secret_input_field"),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (labelInput.isNotBlank() && secretInput.isNotBlank()) {
                                viewModel.save2faSecret(labelInput.trim(), secretInput.trim())
                                labelInput = ""
                                secretInput = ""
                                isAdding = false
                                Toast.makeText(context, "Secret saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_token_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
                    ) {
                        Text("Save Secret", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Live timer progress indicator for token values
        if (savedSecrets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Timer",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dynamic 2FA Codes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { totpProgress / 30f },
                        color = if (totpProgress < 6f) Color.Red else MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${totpProgress.toInt()}s remaining",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totpProgress < 6f) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Token list view
        if (savedSecrets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Empty",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No 2FA Secrets Added",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Click the '+' button above to secure an account",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedSecrets) { item ->
                    val tokenCode = totpMap[item.id] ?: "000000"
                    // Format code with space: "123 456"
                    val formattedCode = if (tokenCode.length == 6) {
                        "${tokenCode.substring(0, 3)} ${tokenCode.substring(3)}"
                    } else tokenCode

                    val tokenCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 6.dp, bottomStart = 6.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secret_token_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = tokenCardShape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    color = if (totpProgress < 6f) Color.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("2fa", tokenCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.delete2faSecret(item.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete secret",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COUNTRY MAPPER & HELPER
// ============================================================================

val COUNTRY_FLAG_MAP = mapOf(
    "1" to Pair("🇺🇸", "USA/Canada"),
    "7" to Pair("🇷🇺", "Russia/Kazakhstan"),
    "20" to Pair("🇪🇬", "Egypt"),
    "27" to Pair("🇿🇦", "South Africa"),
    "30" to Pair("🇬🇷", "Greece"),
    "31" to Pair("🇳🇱", "Netherlands"),
    "32" to Pair("🇧🇪", "Belgium"),
    "33" to Pair("🇫🇷", "France"),
    "34" to Pair("🇪🇸", "Spain"),
    "36" to Pair("🇭🇺", "Hungary"),
    "39" to Pair("🇮🇹", "Italy"),
    "40" to Pair("🇷🇴", "Romania"),
    "41" to Pair("🇨🇭", "Switzerland"),
    "43" to Pair("🇦🇹", "Austria"),
    "44" to Pair("🇬🇧", "United Kingdom"),
    "45" to Pair("🇩🇰", "Denmark"),
    "46" to Pair("🇸🇪", "Sweden"),
    "47" to Pair("🇳🇴", "Norway"),
    "48" to Pair("🇵🇱", "Poland"),
    "49" to Pair("🇩🇪", "Germany"),
    "51" to Pair("🇵🇪", "Peru"),
    "52" to Pair("🇲🇽", "Mexico"),
    "53" to Pair("🇨🇺", "Cuba"),
    "54" to Pair("🇦🇷", "Argentina"),
    "55" to Pair("🇧🇷", "Brazil"),
    "56" to Pair("🇨🇱", "Chile"),
    "57" to Pair("🇨🇴", "Colombia"),
    "58" to Pair("🇻🇪", "Venezuela"),
    "60" to Pair("🇲🇾", "Malaysia"),
    "61" to Pair("🇦🇺", "Australia"),
    "62" to Pair("🇮🇩", "Indonesia"),
    "63" to Pair("🇵🇭", "Philippines"),
    "64" to Pair("🇳🇿", "New Zealand"),
    "65" to Pair("🇸🇬", "Singapore"),
    "66" to Pair("🇹🇭", "Thailand"),
    "81" to Pair("🇯🇵", "Japan"),
    "82" to Pair("🇰🇷", "South Korea"),
    "84" to Pair("🇻🇳", "Vietnam"),
    "86" to Pair("🇨🇳", "China"),
    "90" to Pair("🇹🇷", "Turkey"),
    "91" to Pair("🇮🇳", "India"),
    "92" to Pair("🇵🇰", "Pakistan"),
    "93" to Pair("🇦🇫", "Afghanistan"),
    "94" to Pair("🇱🇰", "Sri Lanka"),
    "95" to Pair("🇲🇲", "Myanmar"),
    "98" to Pair("🇮🇷", "Iran"),
    "211" to Pair("🇸🇸", "South Sudan"),
    "212" to Pair("🇲🇦", "Morocco"),
    "213" to Pair("🇩🇿", "Algeria"),
    "216" to Pair("🇹🇳", "Tunisia"),
    "218" to Pair("🇱🇾", "Libya"),
    "220" to Pair("🇬🇲", "Gambia"),
    "221" to Pair("🇸🇳", "Senegal"),
    "222" to Pair("🇲🇷", "Mauritania"),
    "223" to Pair("🇲🇱", "Mali"),
    "224" to Pair("🇬🇳", "Guinea"),
    "225" to Pair("🇨🇮", "Ivory Coast"),
    "226" to Pair("🇧🇫", "Burkina Faso"),
    "227" to Pair("🇳🇪", "Niger"),
    "228" to Pair("🇹🇬", "Togo"),
    "229" to Pair("🇧🇯", "Benin"),
    "230" to Pair("🇲🇺", "Mauritius"),
    "231" to Pair("🇱🇷", "Liberia"),
    "232" to Pair("🇸🇱", "Sierra Leone"),
    "233" to Pair("🇬🇭", "Ghana"),
    "234" to Pair("🇳🇬", "Nigeria"),
    "235" to Pair("🇹🇩", "Chad"),
    "236" to Pair("🇨🇫", "Central African Republic"),
    "237" to Pair("🇨🇲", "Cameroon"),
    "238" to Pair("🇨🇻", "Cape Verde"),
    "239" to Pair("🇸🇹", "Sao Tome & Principe"),
    "240" to Pair("🇬🇶", "Equatorial Guinea"),
    "241" to Pair("🇬🇦", "Gabon"),
    "242" to Pair("🇨🇬", "Congo"),
    "243" to Pair("🇨🇩", "DR Congo"),
    "244" to Pair("🇦🇴", "Angola"),
    "245" to Pair("🇬🇼", "Guinea-Bissau"),
    "248" to Pair("🇸🇨", "Seychelles"),
    "249" to Pair("🇸🇩", "Sudan"),
    "250" to Pair("🇷🇼", "Rwanda"),
    "251" to Pair("🇪🇹", "Ethiopia"),
    "252" to Pair("🇸🇴", "Somalia"),
    "253" to Pair("🇩🇯", "Djibouti"),
    "254" to Pair("🇰🇪", "Kenya"),
    "255" to Pair("🇹🇿", "Tanzania"),
    "256" to Pair("🇺🇬", "Uganda"),
    "257" to Pair("🇧🇮", "Burundi"),
    "258" to Pair("🇲🇿", "Mozambique"),
    "260" to Pair("🇿🇲", "Zambia"),
    "261" to Pair("🇲🇬", "Madagascar"),
    "262" to Pair("🇷🇪", "Reunion"),
    "263" to Pair("🇿🇼", "Zimbabwe"),
    "264" to Pair("🇳🇦", "Namibia"),
    "265" to Pair("🇲🇼", "Malawi"),
    "266" to Pair("🇱🇸", "Lesotho"),
    "267" to Pair("🇧🇼", "Botswana"),
    "268" to Pair("🇸🇿", "Eswatini"),
    "269" to Pair("🇰🇲", "Comoros"),
    "290" to Pair("🇸🇭", "Saint Helena"),
    "291" to Pair("🇪🇷", "Eritrea"),
    "297" to Pair("🇦🇼", "Aruba"),
    "298" to Pair("🇫🇴", "Faroe Islands"),
    "299" to Pair("🇬🇱", "Greenland"),
    "350" to Pair("🇬🇮", "Gibraltar"),
    "351" to Pair("🇵🇹", "Portugal"),
    "352" to Pair("🇱🇺", "Luxembourg"),
    "353" to Pair("🇮🇪", "Ireland"),
    "354" to Pair("🇮🇸", "Iceland"),
    "355" to Pair("🇦🇱", "Albania"),
    "356" to Pair("🇲🇹", "Malta"),
    "357" to Pair("🇨🇾", "Cyprus"),
    "358" to Pair("🇫🇮", "Finland"),
    "359" to Pair("🇧🇬", "Bulgaria"),
    "370" to Pair("🇱🇹", "Lithuania"),
    "371" to Pair("🇱🇻", "Latvia"),
    "372" to Pair("🇪🇪", "Estonia"),
    "373" to Pair("🇲🇩", "Moldova"),
    "374" to Pair("🇦🇲", "Armenia"),
    "375" to Pair("🇧🇾", "Belarus"),
    "376" to Pair("🇦🇩", "Andorra"),
    "377" to Pair("🇲🇨", "Monaco"),
    "378" to Pair("🇸🇲", "San Marino"),
    "380" to Pair("🇺🇦", "Ukraine"),
    "381" to Pair("🇷🇸", "Serbia"),
    "382" to Pair("🇲🇪", "Montenegro"),
    "383" to Pair("🇽🇰", "Kosovo"),
    "385" to Pair("🇭🇷", "Croatia"),
    "386" to Pair("🇸🇮", "Slovenia"),
    "387" to Pair("🇧🇦", "Bosnia"),
    "389" to Pair("🇲🇰", "North Macedonia"),
    "420" to Pair("🇨🇿", "Czechia"),
    "421" to Pair("🇸🇰", "Slovakia"),
    "423" to Pair("🇱🇮", "Liechtenstein"),
    "500" to Pair("🇫🇰", "Falkland Islands"),
    "501" to Pair("🇧🇿", "Belize"),
    "502" to Pair("🇬🇹", "Guatemala"),
    "503" to Pair("🇸🇻", "El Salvador"),
    "504" to Pair("🇭🇳", "Honduras"),
    "505" to Pair("🇳🇮", "Nicaragua"),
    "506" to Pair("🇨🇷", "Costa Rica"),
    "507" to Pair("🇵🇦", "Panama"),
    "508" to Pair("🇵🇲", "St. Pierre & Miquelon"),
    "509" to Pair("🇭🇹", "Haiti"),
    "590" to Pair("🇬🇵", "Guadeloupe"),
    "591" to Pair("🇧🇴", "Bolivia"),
    "592" to Pair("🇬🇾", "Guyana"),
    "593" to Pair("🇪🇨", "Ecuador"),
    "594" to Pair("🇬🇫", "French Guiana"),
    "595" to Pair("🇵🇾", "Paraguay"),
    "596" to Pair("🇲🇶", "Martinique"),
    "597" to Pair("🇸🇷", "Suriname"),
    "598" to Pair("🇺🇾", "Uruguay"),
    "599" to Pair("🇨🇼", "Curacao"),
    "670" to Pair("🇹🇱", "East Timor"),
    "672" to Pair("🇦🇶", "Antarctica/Norfolk"),
    "673" to Pair("🇧🇳", "Brunei"),
    "674" to Pair("🇳🇷", "Nauru"),
    "675" to Pair("🇵🇬", "Papua New Guinea"),
    "676" to Pair("🇹🇴", "Tonga"),
    "677" to Pair("🇸🇧", "Solomon Islands"),
    "678" to Pair("🇻🇺", "Vanuatu"),
    "679" to Pair("🇫🇯", "Fiji"),
    "680" to Pair("🇵🇼", "Palau"),
    "681" to Pair("🇼🇫", "Wallis & Futuna"),
    "682" to Pair("🇨🇰", "Cook Islands"),
    "683" to Pair("🇳🇺", "Niue"),
    "685" to Pair("🇼🇸", "Samoa"),
    "686" to Pair("🇰🇮", "Kiribati"),
    "687" to Pair("🇳🇨", "New Caledonia"),
    "688" to Pair("🇹🇻", "Tuvalu"),
    "689" to Pair("🇵🇫", "French Polynesia"),
    "690" to Pair("🇹🇰", "Tokelau"),
    "691" to Pair("🇫🇲", "Micronesia"),
    "692" to Pair("🇲🇭", "Marshall Islands"),
    "850" to Pair("🇰🇵", "North Korea"),
    "852" to Pair("🇭🇰", "Hong Kong"),
    "853" to Pair("🇲🇴", "Macau"),
    "855" to Pair("🇰🇭", "Cambodia"),
    "856" to Pair("🇱🇦", "Laos"),
    "880" to Pair("🇧🇩", "Bangladesh"),
    "886" to Pair("🇹🇼", "Taiwan"),
    "960" to Pair("🇲🇻", "Maldives"),
    "961" to Pair("🇱🇧", "Lebanon"),
    "962" to Pair("🇯🇴", "Jordan"),
    "963" to Pair("🇸🇾", "Syria"),
    "964" to Pair("🇮🇶", "Iraq"),
    "965" to Pair("🇰🇼", "Kuwait"),
    "966" to Pair("🇸🇦", "Saudi Arabia"),
    "967" to Pair("🇾🇪", "Yemen"),
    "968" to Pair("🇴🇲", "Oman"),
    "970" to Pair("🇵🇸", "Palestine"),
    "971" to Pair("🇦🇪", "UAE"),
    "972" to Pair("🇮🇱", "Israel"),
    "973" to Pair("🇧🇭", "Bahrain"),
    "974" to Pair("🇶🇦", "Qatar"),
    "975" to Pair("🇧🇹", "Bhutan"),
    "976" to Pair("🇲🇳", "Mongolia"),
    "977" to Pair("🇳🇵", "Nepal"),
    "992" to Pair("🇹🇯", "Tajikistan"),
    "993" to Pair("🇹🇲", "Turkmenistan"),
    "994" to Pair("🇦🇿", "Azerbaijan"),
    "995" to Pair("🇬🇪", "Georgia"),
    "996" to Pair("🇰🇬", "Kyrgyzstan"),
    "998" to Pair("🇺🇿", "Uzbekistan"),
    "1242" to Pair("🇧🇸", "Bahamas"),
    "1246" to Pair("🇧🇧", "Barbados"),
    "1264" to Pair("🇦🇮", "Anguilla"),
    "1268" to Pair("🇦🇬", "Antigua & Barbuda"),
    "1284" to Pair("🇻🇬", "British Virgin Islands"),
    "1345" to Pair("🇰🇾", "Cayman Islands"),
    "1441" to Pair("🇧🇲", "Bermuda"),
    "1473" to Pair("🇬🇩", "Grenada"),
    "1649" to Pair("🇹🇨", "Turks & Caicos"),
    "1664" to Pair("🇲🇸", "Montserrat"),
    "1721" to Pair("🇸🇽", "Sint Maarten"),
    "1758" to Pair("🇱🇨", "Saint Lucia"),
    "1767" to Pair("🇩🇲", "Dominica"),
    "1784" to Pair("🇻🇨", "St. Vincent & Grenadines"),
    "1809" to Pair("🇩🇴", "Dominican Republic"),
    "1829" to Pair("🇩🇴", "Dominican Republic"),
    "1849" to Pair("🇩🇴", "Dominican Republic"),
    "1868" to Pair("🇹🇹", "Trinidad & Tobago"),
    "1869" to Pair("🇰🇳", "Saint Kitts & Nevis"),
    "1876" to Pair("🇯🇲", "Jamaica"),
    "1658" to Pair("🇯🇲", "Jamaica")
)

fun getCountryInfo(rangeCode: String): Pair<String, String> {
    val digits = rangeCode.replace(Regex("[^0-9]"), "")
    for (len in 4 downTo 1) {
        if (digits.length >= len) {
            val prefix = digits.substring(0, len)
            val info = COUNTRY_FLAG_MAP[prefix]
            if (info != null) return info
        }
    }
    return Pair("📱", "")
}

// BorderStroke helper
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}
