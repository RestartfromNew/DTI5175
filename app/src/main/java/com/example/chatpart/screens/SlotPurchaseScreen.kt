package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.data.SlotStatus
import com.example.chatpart.ui.theme.Peach
import com.example.chatpart.ui.theme.SoftWhite
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

data class SlotProduct(
    val priceId: String,
    val label: String,
    val priceDisplay: String,
    val slotCount: Int,
    val badge: String? = null,
    val description: String,
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun SlotPurchaseScreen(
    isDarkMode: Boolean = false,
    currentSlotStatus: SlotStatus?,
    onBack: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
) {
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // ⚠️ Replace price_xxx values with real Price IDs from Stripe Dashboard after Step 3.1 of the design doc
    val products = listOf(
        SlotProduct(
            priceId = "price_1THcjgPVVT1phEo7cfMk1VsO",
            label = "+1 Voice Slot",
            priceDisplay = "$2.99",
            slotCount = 1,
            description = "Clone one more voice",
        ),
        SlotProduct(
            priceId = "price_1THcjhPVVT1phEo7b0MZJb0b",
            label = "+3 Voice Slots",
            priceDisplay = "$6.99",
            slotCount = 3,
            badge = "Save 20%",
            description = "Best value — 3 slots for less",
        ),
    )

    val paymentSheet = com.stripe.android.paymentsheet.rememberPaymentSheet { result ->
        isLoading = false
        when (result) {
            is com.stripe.android.paymentsheet.PaymentSheetResult.Completed -> {
                successMessage = "Payment successful! Your new slot will appear shortly."
                onPurchaseSuccess()
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Failed -> {
                errorMessage = result.error.localizedMessage ?: "Payment failed"
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Canceled -> {
                // User cancelled — do nothing
            }
        }
    }

    fun purchase(product: SlotProduct) {
        isLoading = true
        errorMessage = null

        Firebase.functions
            .getHttpsCallable("createPaymentIntent")
            .call(mapOf("priceId" to product.priceId))
            .addOnSuccessListener { result: com.google.firebase.functions.HttpsCallableResult ->
                val data = result.getData() as? Map<*, *>
                val clientSecret = data?.get("clientSecret") as? String
                if (clientSecret == null) {
                    errorMessage = "Failed to start payment. Try again."
                    isLoading = false
                    return@addOnSuccessListener
                }
                paymentSheet.presentWithPaymentIntent(
                    clientSecret,
                    com.stripe.android.paymentsheet.PaymentSheet.Configuration(
                        merchantDisplayName = "ChatPart",
                    )
                )
                // isLoading stays true until PaymentSheetResult fires
            }
            .addOnFailureListener { e ->
                errorMessage = e.localizedMessage ?: "Server error. Try again."
                isLoading = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColor, gradientEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = textColor)
                }
                Text(
                    "Voice Slots",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Current slot status
            if (currentSlotStatus != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, shadowElevation = 2.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Current: ${currentSlotStatus.used} / ${currentSlotStatus.limit} used",
                            fontSize = 14.sp,
                            color = if (currentSlotStatus.isFull) Color(0xFFE53935) else subtitleColor,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { currentSlotStatus.percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (currentSlotStatus.isFull) Color(0xFFE53935) else Peach,
                            trackColor = subtitleColor.copy(alpha = 0.15f),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Product cards
            products.forEach { product ->
                SlotProductCard(
                    product = product,
                    isLoading = isLoading,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onBuy = { purchase(product) },
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Success/error messages
            successMessage?.let {
                Text(it, color = Color(0xFF2E7D32), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
            }
            errorMessage?.let {
                Text(it, color = Color(0xFFE53935), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
            }

            Spacer(Modifier.weight(1f))

            // Stripe trust badge
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Lock, null, tint = subtitleColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Payments secured by Stripe", fontSize = 12.sp, color = subtitleColor)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Product card composable
// ---------------------------------------------------------------------------

@Composable
private fun SlotProductCard(
    product: SlotProduct,
    isLoading: Boolean,
    surfaceColor: Color,
    textColor: Color,
    subtitleColor: Color,
    onBuy: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.label, fontWeight = FontWeight.SemiBold, color = textColor)
                    product.badge?.let { badge ->
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Peach.copy(alpha = 0.15f),
                        ) {
                            Text(
                                badge,
                                fontSize = 10.sp,
                                color = Peach,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(product.description, fontSize = 12.sp, color = subtitleColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(product.priceDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Peach)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onBuy,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Peach),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Buy", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
