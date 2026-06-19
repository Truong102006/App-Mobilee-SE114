package com.soulmate.app.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.soulmate.app.domain.model.PremiumPaymentOrder
import com.soulmate.app.ui.login.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PremiumUpgradeScreen(
    authViewModel: AuthViewModel,
    viewModel: PremiumUpgradeViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState
    val currentUser by authViewModel.currentUser
    var handledPaidOrderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCheckout()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.order?.orderId, uiState.order?.status) {
        val order = uiState.order ?: return@LaunchedEffect
        if (order.isPending()) {
            while (true) {
                delay(3000)
                viewModel.refreshOrderStatus()
                if (viewModel.uiState.value.order?.isPending() != true) {
                    break
                }
            }
        }
    }

    LaunchedEffect(uiState.order?.orderId, uiState.order?.status) {
        val order = uiState.order ?: return@LaunchedEffect
        if (order.isPaid() && handledPaidOrderId != order.orderId) {
            handledPaidOrderId = order.orderId
            val userId = currentUser?.userId?.takeIf { it.isNotBlank() }
            if (userId != null) {
                authViewModel.fetchUserProfile(userId)
            }
            android.widget.Toast.makeText(context, "Thanh toan thanh cong. Premium da duoc kich hoat.", android.widget.Toast.LENGTH_SHORT).show()
            delay(900)
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Upgrade") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.onBackground,
                elevation = 0.dp
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.offer == null && uiState.order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumHeaderCard(
                    isPremium = currentUser?.isPremiumActive() == true,
                    premiumUntil = currentUser?.premiumUntil
                )

                uiState.offer?.let { offer ->
                    OfferCard(
                        priceVnd = offer.priceVnd,
                        durationDays = offer.durationDays,
                        expiresInMinutes = offer.orderExpireMinutes
                    )
                }

                uiState.order?.let { order ->
                    PaymentStatusCard(order = order)
                    PaymentQrCard(order = order)
                    PaymentInstructionCard(order = order)
                    PaymentActions(
                        order = order,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshOrderStatus() },
                        onReconcile = { viewModel.reconcileOrder() },
                        onCreateNew = { viewModel.loadCheckout() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumHeaderCard(
    isPremium: Boolean,
    premiumUntil: Long?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF3E0)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFFB300),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (isPremium) "PREMIUM ACTIVE" else "FREE ACCOUNT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isPremium) "Tai khoan cua ban dang o goi Premium." else "Nang cap tai khoan bang SePay.",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPremium && premiumUntil != null) {
                    "Premium hien tai co hieu luc den ${formatDate(premiumUntil)}."
                } else {
                    "V1 chi kich hoat trang thai Premium, chua khoa bat ky tinh nang hien tai nao."
                },
                color = Color(0xFF5D4037),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun OfferCard(
    priceVnd: Long,
    durationDays: Int,
    expiresInMinutes: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Premium 30 days",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatCurrency(priceVnd)} / $durationDays ngay",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Moi ma thanh toan co hieu luc trong $expiresInMinutes phut. Neu ban da co Premium, thoi han se duoc cong don.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PaymentStatusCard(order: PremiumPaymentOrder) {
    val statusColor = when {
        order.isPaid() -> Color(0xFF2E7D32)
        order.isExpired() -> Color(0xFFC62828)
        else -> Color(0xFFEF6C00)
    }

    var nowMillis by remember(order.orderId) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(order.orderId, order.isPending()) {
        if (order.isPending()) {
            while (true) {
                delay(1000)
                nowMillis = System.currentTimeMillis()
                if (!order.isPending()) break
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.onSurface
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = order.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Order ID: ${order.orderId}", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Transfer content: ${order.paymentCode}", color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Amount: ${formatCurrency(order.amountVnd)}", color = MaterialTheme.colors.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    order.isPaid() && order.premiumGrantedUntil != null ->
                        "Premium active until ${formatDate(order.premiumGrantedUntil)}."
                    order.isExpired() ->
                        "QR code expired. Create a new code to continue."
                    else ->
                        "Time left: ${formatDuration((order.expiresAt - nowMillis).coerceAtLeast(0L))}"
                },
                color = if (order.isExpired()) Color.Red else Color.Gray,
                fontSize = 14.sp
            )
            if (order.latePayment) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Late payment was accepted within the reconcile window.",
                    color = Color(0xFF2E7D32),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PaymentQrCard(order: PremiumPaymentOrder) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan QR with your banking app",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                AsyncImage(
                    model = order.qrImageUrl,
                    contentDescription = "Premium payment QR",
                    modifier = Modifier.size(240.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentInstructionCard(order: PremiumPaymentOrder) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Manual transfer details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bank: ${order.bankCode}", color = MaterialTheme.colors.onSurface)
                    Text("Account number: ${order.bankAccount}", color = MaterialTheme.colors.onSurface)
                    Text("Account holder: ${order.accountHolder}", color = MaterialTheme.colors.onSurface)
                    Text("Amount: ${formatCurrency(order.amountVnd)}", color = MaterialTheme.colors.onSurface)
                    Text("Content: ${order.paymentCode}", color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Use the exact amount and transfer content so backend can match your payment automatically.",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PaymentActions(
    order: PremiumPaymentOrder,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onReconcile: () -> Unit,
    onCreateNew: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (order.isPending()) {
            Button(
                onClick = onReconcile,
                enabled = !isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Kiem tra lai thanh toan")
                }
            }
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Lam moi trang thai")
            }
        }

        if (order.isExpired()) {
            Button(
                onClick = onCreateNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Text("Tao ma thanh toan moi", color = Color.White)
            }
        }
    }
}

private fun formatCurrency(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount) + " VND"
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
