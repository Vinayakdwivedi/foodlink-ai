package com.example.ui.screens

import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.FoodRecognitionResult
import com.example.api.GeminiClient
import com.example.api.RecipeSuggestion
import com.example.api.WastePredictionResult
import com.example.data.*
import com.example.ui.FoodLinkViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.NatureBackground
import com.example.ui.theme.*

// Screen navigation enum
enum class AppScreen {
    SPLASH,
    ONBOARDING,
    LOGIN,
    REGISTER,
    OTP,
    FORGOT_PASSWORD,
    MAIN_HUB,
    PAYMENT_FLOW,
    SUCCESS_RECEIPT
}

// Hub tabs
enum class HubTab {
    DASHBOARD,
    INVENTORY,
    AI_HUB,
    IMPACT,
    PROFILE
}

@Composable
fun FoodLinkApp(
    viewModel: FoodLinkViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var onboardingIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableStateOf(HubTab.DASHBOARD) }
    
    // Auth fields
    var emailInput by remember { mutableStateOf("moonicorganic@gmail.com") }
    var passwordInput by remember { mutableStateOf("password") }
    var nameInput by remember { mutableStateOf("John Green") }
    var otpInput by remember { mutableStateOf("") }
    
    // Subscription payment selection
    var selectedPlanPrice by remember { mutableIntStateOf(19) }
    var selectedPlanTitle by remember { mutableStateOf("Home Basic") }
    var selectedPaymentGateway by remember { mutableStateOf("Google Pay") }
    
    // Observers
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val donations by viewModel.donations.collectAsStateWithLifecycle()
    val wasteLogs by viewModel.wasteLogs.collectAsStateWithLifecycle()
    val pickups by viewModel.pickups.collectAsStateWithLifecycle()

    val aiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiRecognition by viewModel.aiRecognitionResult.collectAsStateWithLifecycle()
    val recipeSuggestions by viewModel.recipeSuggestions.collectAsStateWithLifecycle()
    val wastePrediction by viewModel.wastePredictionResult.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Automatically transition from splash to onboarding
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.SPLASH) {
            kotlinx.coroutines.delay(2800)
            currentScreen = AppScreen.ONBOARDING
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NatureBackground(isDarkTheme = true) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "AppScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SPLASH -> SplashScreen()
                        
                        AppScreen.ONBOARDING -> OnboardingScreen(
                            index = onboardingIndex,
                            onNext = {
                                if (onboardingIndex < 2) {
                                    onboardingIndex++
                                } else {
                                    currentScreen = AppScreen.LOGIN
                                }
                            },
                            onSkip = { currentScreen = AppScreen.LOGIN }
                        )
                        
                        AppScreen.LOGIN -> LoginScreen(
                            email = emailInput,
                            password = passwordInput,
                            onEmailChange = { emailInput = it },
                            onPasswordChange = { passwordInput = it },
                            onLogin = { role ->
                                if (role == UserRole.ADMIN && !emailInput.contains("admin", ignoreCase = true)) {
                                    Toast.makeText(context, "Access Denied: Only admins can log into the Admin Gateway.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.loginUser(emailInput, nameInput, role)
                                    currentScreen = AppScreen.MAIN_HUB
                                    Toast.makeText(context, "Logged in as ${role.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onNavigateToRegister = { currentScreen = AppScreen.REGISTER },
                            onNavigateToForgot = { currentScreen = AppScreen.FORGOT_PASSWORD }
                        )
                        
                        AppScreen.REGISTER -> RegisterScreen(
                            name = nameInput,
                            email = emailInput,
                            password = passwordInput,
                            onNameChange = { nameInput = it },
                            onEmailChange = { emailInput = it },
                            onPasswordChange = { passwordInput = it },
                            onRegister = {
                                currentScreen = AppScreen.OTP
                            },
                            onNavigateToLogin = { currentScreen = AppScreen.LOGIN }
                        )
                        
                        AppScreen.OTP -> OtpScreen(
                            otp = otpInput,
                            onOtpChange = { otpInput = it },
                            onVerify = {
                                currentScreen = AppScreen.MAIN_HUB
                                Toast.makeText(context, "Verification Successful!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                        AppScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(
                            email = emailInput,
                            onEmailChange = { emailInput = it },
                            onReset = {
                                Toast.makeText(context, "Reset link sent to $emailInput", Toast.LENGTH_LONG).show()
                                currentScreen = AppScreen.LOGIN
                            },
                            onBackToLogin = { currentScreen = AppScreen.LOGIN }
                        )
                        
                        AppScreen.MAIN_HUB -> MainHubScreen(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            viewModel = viewModel,
                            userProfile = userProfile,
                            inventory = inventory,
                            donations = donations,
                            wasteLogs = wasteLogs,
                            pickups = pickups,
                            aiLoading = aiLoading,
                            aiRecognition = aiRecognition,
                            recipeSuggestions = recipeSuggestions,
                            wastePrediction = wastePrediction,
                            onNavigateToPayment = { title, price ->
                                selectedPlanTitle = title
                                selectedPlanPrice = price
                                currentScreen = AppScreen.PAYMENT_FLOW
                            },
                            onLogout = {
                                currentScreen = AppScreen.LOGIN
                            }
                        )
                        
                        AppScreen.PAYMENT_FLOW -> PaymentGatewayScreen(
                            planTitle = selectedPlanTitle,
                            price = selectedPlanPrice,
                            selectedGateway = selectedPaymentGateway,
                            onGatewaySelected = { selectedPaymentGateway = it },
                            onPay = {
                                // Update user's subscription in viewModel
                                val sub = when (selectedPlanPrice) {
                                    19 -> SubscriptionType.HOME_BASIC
                                    999 -> SubscriptionType.RESTAURANT_PREMIUM
                                    2999 -> SubscriptionType.PREMIUM_COGNITIVE
                                    else -> SubscriptionType.FREE
                                }
                                viewModel.changeSubscription(sub)
                                currentScreen = AppScreen.SUCCESS_RECEIPT
                            },
                            onCancel = { currentScreen = AppScreen.MAIN_HUB }
                        )
                        
                        AppScreen.SUCCESS_RECEIPT -> PaymentSuccessScreen(
                            planTitle = selectedPlanTitle,
                            price = selectedPlanPrice,
                            gateway = selectedPaymentGateway,
                            onDone = {
                                currentScreen = AppScreen.MAIN_HUB
                            }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 1. SPLASH SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowProgress"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stylized Leaf / Earth Globe Logo using Canvas Drawing
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .drawWithContent {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(
                                    OliveLight.copy(alpha = 0.8f * glowProgress),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension / 1.1f
                        )
                        drawContent()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "FoodLink AI Logo",
                    tint = SoftGold,
                    modifier = Modifier.size(72.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "FoodLink AI",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Save Food. Feed People. Protect Earth.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MintSoft.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = SoftGold,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// 2. ONBOARDING SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun OnboardingScreen(
    index: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val title = when (index) {
        0 -> "Rescue Surplus Food"
        1 -> "Empower Nearby NGOs"
        else -> "Track Ecological Impact"
    }

    val description = when (index) {
        0 -> "Prevent fresh food from entering landfills. Scan grocery items, receive early alerts before expiry, and manage your inventory with automated AI predictions."
        1 -> "Coordinate instant pickups. Restaurants, supermarkets, and residential donors can upload surplus meals directly to certified volunteer networks in real-time."
        else -> "Calculate active CO2 reductions, meals donated to families in need, equivalent trees planted, and watch your local community environmental index soar."
    }

    val icon = when (index) {
        0 -> Icons.Default.EnergySavingsLeaf
        1 -> Icons.Default.Favorite
        else -> Icons.Default.Public
    }

    val stepLabel = "Step ${index + 1} of 3"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip", color = SoftGold, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(OliveLight, SoftGold)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MintSoft.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Simple indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == index) 12.dp else 8.dp)
                            .background(
                                color = if (i == index) SoftGold else OliveMain.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = if (index == 2) "Get Started" else "Next",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MintSoft.copy(alpha = 0.5f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// 3. LOGIN SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun LoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.HOME_USER) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Welcome to FoodLink AI",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            
            Text(
                text = "Save Food. Feed People. Protect Earth.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MintSoft.copy(alpha = 0.7f))
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Interactive Role Gateway Selector
            Text(
                text = "Select Portal Gateway",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SoftGold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            val rolesToShow = remember(email) {
                UserRole.values().filter { role ->
                    role != UserRole.ADMIN || email.contains("admin", ignoreCase = true)
                }
            }
            
            LaunchedEffect(email) {
                if (selectedRole == UserRole.ADMIN && !email.contains("admin", ignoreCase = true)) {
                    selectedRole = UserRole.HOME_USER
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rolesToShow.forEach { role ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedRole == role) OliveMain else Color.Transparent)
                            .clickable { selectedRole = role }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(role) {
                                UserRole.HOME_USER -> "Home"
                                UserRole.RESTAURANT -> "Restaurant"
                                UserRole.NGO -> "NGO"
                                UserRole.ADMIN -> "Admin"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRole == role) Color.White else MintSoft
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Sign In to continue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email Address", color = MintSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftGold,
                            unfocusedBorderColor = OliveLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password", color = MintSoft) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftGold,
                            unfocusedBorderColor = OliveLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onNavigateToForgot) {
                            Text("Forgot Password?", color = SoftGold, fontSize = 13.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onLogin(selectedRole) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Access Gateway", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account?", color = MintSoft.copy(alpha = 0.6f))
                TextButton(onClick = onNavigateToRegister) {
                    Text("Register Here", color = SoftGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 4. REGISTER SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun RegisterScreen(
    name: String,
    email: String,
    password: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Join FoodLink AI",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            
            Text(
                text = "Join the global sustainable food ecosystem.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MintSoft.copy(alpha = 0.7f)),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Full Name / Organization", color = MintSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftGold,
                            unfocusedBorderColor = OliveLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email Address", color = MintSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftGold,
                            unfocusedBorderColor = OliveLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password", color = MintSoft) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftGold,
                            unfocusedBorderColor = OliveLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already registered?", color = MintSoft.copy(alpha = 0.6f))
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", color = SoftGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 5. OTP VERIFICATION SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun OtpScreen(
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We've sent a 4-digit security code to your email.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MintSoft.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth(0.9f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 4) onOtpChange(it) },
                    label = { Text("4-Digit OTP", color = MintSoft) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGold,
                        unfocusedBorderColor = OliveLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.width(180.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onVerify,
                    enabled = otp.length == 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_otp_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Verify & Authenticate", fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { /* Resend simulated OTP */ }) {
                    Text("Resend Code", color = SoftGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 6. FORGOT PASSWORD SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun ForgotPasswordScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onReset: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LockReset,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Reset Password",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your registered email and we'll send instructions to restore access.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MintSoft.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Registered Email", color = MintSoft) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGold,
                        unfocusedBorderColor = OliveLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_password_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Send Recovery Email", fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onBackToLogin) {
                    Text("Back to Login", color = SoftGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 7. MAIN PLATFORM HUB (BOTTOM NAV + SIDEBAR + DASHBOARDS)
// ══════════════════════════════════════════════════════
@Composable
fun MainHubScreen(
    selectedTab: HubTab,
    onTabSelected: (HubTab) -> Unit,
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    inventory: List<FoodItem>,
    donations: List<Donation>,
    wasteLogs: List<WasteLog>,
    pickups: List<NgoPickup>,
    aiLoading: Boolean,
    aiRecognition: FoodRecognitionResult?,
    recipeSuggestions: List<RecipeSuggestion>,
    wastePrediction: WastePredictionResult?,
    onNavigateToPayment: (String, Int) -> Unit,
    onLogout: () -> Unit
) {
    // Scaffold containing Bottom Navigation Bar
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF131A0D),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                HubTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    HubTab.DASHBOARD -> Icons.Default.GridView
                                    HubTab.INVENTORY -> Icons.Default.ListAlt
                                    HubTab.AI_HUB -> Icons.Default.Hub
                                    HubTab.IMPACT -> Icons.Default.BarChart
                                    HubTab.PROFILE -> Icons.Default.AccountCircle
                                },
                                contentDescription = tab.name
                            )
                        },
                        label = {
                            Text(
                                text = when (tab) {
                                    HubTab.DASHBOARD -> "Portal"
                                    HubTab.INVENTORY -> "Stock"
                                    HubTab.AI_HUB -> "AI Core"
                                    HubTab.IMPACT -> "Eco-Impact"
                                    HubTab.PROFILE -> "Setup"
                                },
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SoftGold,
                            selectedTextColor = SoftGold,
                            indicatorColor = OliveDark,
                            unselectedIconColor = MintSoft.copy(alpha = 0.5f),
                            unselectedTextColor = MintSoft.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen content transition
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = "HubTabTransition"
            ) { tab ->
                when (tab) {
                    HubTab.DASHBOARD -> RoleSpecificDashboard(
                        viewModel = viewModel,
                        userProfile = userProfile,
                        inventory = inventory,
                        donations = donations,
                        wasteLogs = wasteLogs,
                        pickups = pickups,
                        onNavigateToPayment = onNavigateToPayment,
                        onTabSelected = onTabSelected
                    )
                    HubTab.INVENTORY -> StockManagementScreen(
                        viewModel = viewModel,
                        userProfile = userProfile,
                        inventory = inventory
                    )
                    HubTab.AI_HUB -> AiHubScreen(
                        viewModel = viewModel,
                        inventory = inventory,
                        aiLoading = aiLoading,
                        aiRecognition = aiRecognition,
                        recipeSuggestions = recipeSuggestions,
                        wastePrediction = wastePrediction
                    )
                    HubTab.IMPACT -> ImpactDashboardScreen(
                        donations = donations,
                        wasteLogs = wasteLogs,
                        viewModel = viewModel
                    )
                    HubTab.PROFILE -> ProfileSettingsScreen(
                        viewModel = viewModel,
                        userProfile = userProfile,
                        onNavigateToPayment = onNavigateToPayment,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// ROLE-SPECIFIC DASHBOARD SWITCHER
// ══════════════════════════════════════════════════════
@Composable
fun RoleSpecificDashboard(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    inventory: List<FoodItem>,
    donations: List<Donation>,
    wasteLogs: List<WasteLog>,
    pickups: List<NgoPickup>,
    onNavigateToPayment: (String, Int) -> Unit,
    onTabSelected: (HubTab) -> Unit
) {
    // Quick Demo Role Switcher Bar on Top of Dashboard to let anyone verify every module!
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F140A))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Portal Demo shortcuts:",
                style = MaterialTheme.typography.bodySmall.copy(color = SoftGold, fontWeight = FontWeight.Bold)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                UserRole.values()
                    .filter { role ->
                        role != UserRole.ADMIN || userProfile.role == UserRole.ADMIN
                    }
                    .forEach { role ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (userProfile.role == role) OliveMain else Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.changeUserRole(role) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = role.name.take(4),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (userProfile.role) {
                UserRole.HOME_USER -> HomeUserDashboard(viewModel, userProfile, inventory, donations, onNavigateToPayment, onTabSelected)
                UserRole.RESTAURANT -> RestaurantDashboard(viewModel, userProfile, inventory, wasteLogs, donations, onNavigateToPayment, onTabSelected)
                UserRole.NGO -> NgoDashboard(viewModel, userProfile, pickups)
                UserRole.ADMIN -> AdminDashboard(viewModel, userProfile, donations, wasteLogs)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// DECORATIVE & GRAPHICS UTILITIES
// ══════════════════════════════════════════════════════

@Composable
fun EarthIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        
        // Ocean (Blue background with gradient)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5)),
                center = centerOffset,
                radius = radius
            ),
            radius = radius,
            center = centerOffset
        )
        
        // Land masses (Green patches)
        val landBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF81C784), Color(0xFF4CAF50))
        )
        
        // Landmass 1 (Left-ish)
        val path1 = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(
                centerOffset.x - radius * 0.8f,
                centerOffset.y - radius * 0.6f,
                centerOffset.x + radius * 0.1f,
                centerOffset.y + radius * 0.4f
            ))
        }
        drawPath(path1, brush = landBrush)
        
        // Landmass 2 (Right-ish)
        val path2 = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(
                centerOffset.x + radius * 0.1f,
                centerOffset.y - radius * 0.4f,
                centerOffset.x + radius * 0.9f,
                centerOffset.y + radius * 0.3f
            ))
        }
        drawPath(path2, brush = landBrush)

        // Landmass 3 (Bottom-ish)
        val path3 = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(
                centerOffset.x - radius * 0.4f,
                centerOffset.y + radius * 0.2f,
                centerOffset.x + radius * 0.5f,
                centerOffset.y + radius * 0.8f
            ))
        }
        drawPath(path3, brush = landBrush)
        
        // Beautiful green sprout/leaf growing on top!
        val sproutPath = Path().apply {
            moveTo(centerOffset.x, centerOffset.y - radius)
            quadraticTo(centerOffset.x + radius * 0.2f, centerOffset.y - radius * 1.3f, centerOffset.x + radius * 0.4f, centerOffset.y - radius * 1.4f)
            quadraticTo(centerOffset.x + radius * 0.1f, centerOffset.y - radius * 1.1f, centerOffset.x, centerOffset.y - radius)
            
            moveTo(centerOffset.x, centerOffset.y - radius)
            quadraticTo(centerOffset.x - radius * 0.2f, centerOffset.y - radius * 1.3f, centerOffset.x - radius * 0.4f, centerOffset.y - radius * 1.4f)
            quadraticTo(centerOffset.x - radius * 0.1f, centerOffset.y - radius * 1.1f, centerOffset.x, centerOffset.y - radius)
        }
        drawPath(sproutPath, color = SoftGold, style = Stroke(width = 2.dp.toPx()))
        drawPath(sproutPath, brush = Brush.linearGradient(listOf(SoftGoldLight, Color(0xFFC5A028))))
        
        // Glowing outline
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = radius,
            center = centerOffset,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun EnvironmentalImpactDial(score: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        
        // Background track (soft grey/green)
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = radius,
            center = centerOffset,
            style = Stroke(width = strokeWidth)
        )
        
        // Progress arc
        val sweepAngle = 360f * (score / 100f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(SoftGold, SoftGoldLight, SoftGold)
            ),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
        )
    }
}

@Composable
fun WasteAnalyticsCurveChart(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 32.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 16.dp.toPx()
        val paddingBottom = 24.dp.toPx()
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        // Grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = paddingTop + chartHeight * (i.toFloat() / gridLines)
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Points data: 500, 750, 600, 1100, 950, 1300, 1150
        val points = listOf(500f, 750f, 600f, 1100f, 950f, 1300f, 1150f)
        val maxVal = 1500f
        
        val path = Path()
        val fillPath = Path()
        
        val stepX = chartWidth / (points.size - 1)
        
        for (i in points.indices) {
            val x = paddingLeft + i * stepX
            val y = paddingTop + chartHeight * (1f - (points[i] / maxVal))
            
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                val prevX = paddingLeft + (i - 1) * stepX
                val prevY = paddingTop + chartHeight * (1f - (points[i - 1] / maxVal))
                
                val controlX1 = prevX + stepX / 2f
                val controlY1 = prevY
                val controlX2 = prevX + stepX / 2f
                val controlY2 = y
                
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }
        
        fillPath.lineTo(paddingLeft + chartWidth, paddingTop + chartHeight)
        fillPath.lineTo(paddingLeft, paddingTop + chartHeight)
        fillPath.close()
        
        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(OliveLight.copy(alpha = 0.25f), Color.Transparent)
            )
        )
        
        // Draw curved line
        drawPath(
            path = path,
            color = SoftGold,
            style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Draw points dots
        for (i in points.indices) {
            val x = paddingLeft + i * stepX
            val y = paddingTop + chartHeight * (1f - (points[i] / maxVal))
            
            drawCircle(
                color = OliveDark,
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = SoftGoldLight,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun WasteCategoryPieChart(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2.2f
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        
        // Percentages: Vegetables 35%, Cooked Food 25%, Bakery 15%, Dairy 10%, Others 15%
        val slices = listOf(35f, 25f, 15f, 10f, 15f)
        val colors = listOf(
            Color(0xFF81C784), // Vegetables (soft green)
            Color(0xFFFFB74D), // Cooked Food (soft orange)
            Color(0xFFE8C863), // Bakery (soft gold)
            Color(0xFF64B5F6), // Dairy (soft blue)
            Color(0xFFBA68C8)  // Others (soft purple)
        )
        
        var startAngle = -90f
        for (i in slices.indices) {
            val sweepAngle = 360f * (slices[i] / 100f)
            drawArc(
                color = colors[i],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
            )
            startAngle += sweepAngle
        }
        
        // Center text hole shadow (or just empty hole for donut chart look)
        drawCircle(
            color = Color(0xFF131A0D),
            radius = radius - 8.dp.toPx(),
            center = centerOffset
        )
    }
}

// ══════════════════════════════════════════════════════
// HOME USER DASHBOARD SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun HomeUserDashboard(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    inventory: List<FoodItem>,
    donations: List<Donation>,
    onNavigateToPayment: (String, Int) -> Unit,
    onTabSelected: (HubTab) -> Unit
) {
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showDonateFoodDialog by remember { mutableStateOf(false) }
    
    // Dialog state fields
    var dialogFoodName by remember { mutableStateOf("") }
    var dialogQuantity by remember { mutableStateOf("") }
    var dialogCategory by remember { mutableStateOf("Cooked Food") }
    var dialogExpiryDays by remember { mutableStateOf("3") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hello, ${userProfile.name}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("👋", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Let's save food and protect our planet together.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFB9CDB3))
                    )
                }
                
                // Earth illustration
                EarthIllustration(
                    modifier = Modifier
                        .size(80.dp)
                        .weight(0.8f)
                )
            }
        }

        // YOUR IMPACT TODAY Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF384724)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "YOUR IMPACT TODAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SoftGold,
                                letterSpacing = 1.sp
                            )
                        )
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Col 1
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("2.4 kg", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Food Saved", color = Color(0xFFB9CDB3), fontSize = 10.sp)
                        }
                        // Col 2
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = SoftGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("12", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Meals Saved", color = Color(0xFFB9CDB3), fontSize = 10.sp)
                        }
                        // Col 3
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("320 L", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Water Saved", color = Color(0xFFB9CDB3), fontSize = 10.sp)
                        }
                        // Col 4
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("1.8 kg", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("CO₂ Reduced", color = Color(0xFFB9CDB3), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Status Metrics Row Grid (3 Columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Card 1
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Items", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                                Icon(Icons.Default.LocalMall, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${inventory.size + 12}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    // Card 2
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Expiring Soon", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val expiringCount = inventory.count { it.status == "Pantry" && it.isExpiringSoon } + 3
                            Text("$expiringCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    // Card 3
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Food Saved", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                                Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("12.4 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                
                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Card 4
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Food Donated", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("7.3 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    // Card 5
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Money Saved", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SoftGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹346", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    // Card 6
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CO₂ Reduced", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("5.6 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Action 1: Add Food
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAddFoodDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF384724)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Add Food", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }

                    // Action 2: Scan Barcode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTabSelected(HubTab.AI_HUB) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = SoftGold, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Scan Barcode", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }

                    // Action 3: Donate Food
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDonateFoodDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Donate Food", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }

                    // Action 4: AI Recipes
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTabSelected(HubTab.AI_HUB) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("AI Recipes", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // My Inventory Section with View All Link
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My Inventory",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    "View All →",
                    style = MaterialTheme.typography.bodySmall.copy(color = SoftGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onTabSelected(HubTab.INVENTORY) }
                )
            }
        }

        // Inventory Items Styled Elegantly
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Item 1: Milk
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥛", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Milk", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("2 Liter", color = Color(0xFFB9CDB3), fontSize = 11.sp)
                            Text("Expires on 29 May 2026", color = Color(0xFFB9CDB3).copy(alpha = 0.6f), fontSize = 9.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFE57373).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color(0xFFE57373).copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("2 Days Left", fontSize = 9.sp, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                    }
                }

                // Item 2: Tomato
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍅", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Tomato", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("500 gm", color = Color(0xFFB9CDB3), fontSize = 11.sp)
                            Text("Expires on 31 May 2026", color = Color(0xFFB9CDB3).copy(alpha = 0.6f), fontSize = 9.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFB74D).copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("4 Days Left", fontSize = 9.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                    }
                }

                // Item 3: Paneer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFFDE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧀", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Paneer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("200 gm", color = Color(0xFFB9CDB3), fontSize = 11.sp)
                            Text("Expires on 02 Jun 2026", color = Color(0xFFB9CDB3).copy(alpha = 0.6f), fontSize = 9.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF81C784).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color(0xFF81C784).copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("5 Days Left", fontSize = 9.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Side Panels Rendered Vertically for complete parity with image
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))
        }

        // AI Assistant Widget Box
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "AI Assistant Hub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SoftGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FoodLink AI", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 13.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // User Chat bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("What can I cook today?", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // AI Chat bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp))
                                    .background(Color(0xFF384724).copy(alpha = 0.4f))
                                    .border(0.5.dp, SoftGold.copy(alpha = 0.2f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        "You can make Vegetable Pulao with items you have.",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("🍛", fontSize = 32.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = { onTabSelected(HubTab.AI_HUB) },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View Recipe", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Environmental Impact Gauge Panel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Environmental Impact",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            EnvironmentalImpactDial(score = 85, modifier = Modifier.fillMaxSize())
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("85", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Excellent 🌱", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Impact Details List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val detailItems = listOf(
                                "Meals Saved" to "24",
                                "Water Saved" to "1,240 L",
                                "CO₂ Reduced" to "8.6 kg",
                                "Trees Equivalent" to "0.7"
                            )
                            detailItems.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, color = Color(0xFFB9CDB3), fontSize = 12.sp)
                                    Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = { onTabSelected(HubTab.IMPACT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF384724)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("See Full Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Recent Activity Panel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listOf(
                            Triple("Food donation accepted", "2h ago", "🟢"),
                            Triple("NGO pickup completed", "4h ago", "🟢"),
                            Triple("You earned Eco Hero Badge", "1d ago", "🏅")
                        ).forEach { (title, time, emoji) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        Text(time, color = Color(0xFFB9CDB3).copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = { onTabSelected(HubTab.IMPACT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View All", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // INTERACTIVE SURPLUS FOOD ENTRY DIALOG
    if (showAddFoodDialog) {
        Dialog(onDismissRequest = { showAddFoodDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Register Surplus Food 🍏", style = MaterialTheme.typography.titleLarge.copy(color = SoftGold, fontWeight = FontWeight.Bold))
                    Text("Add food items to your local stock or share them.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = dialogFoodName,
                        onValueChange = { dialogFoodName = it },
                        label = { Text("Food Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dialogQuantity,
                        onValueChange = { dialogQuantity = it },
                        label = { Text("Quantity (e.g. 1.5 kg)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dialogExpiryDays,
                        onValueChange = { dialogExpiryDays = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Est Expiry (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddFoodDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (dialogFoodName.isNotBlank() && dialogQuantity.isNotBlank()) {
                                    val days = dialogExpiryDays.toIntOrNull() ?: 3
                                    viewModel.addFoodItem(dialogFoodName, dialogQuantity, dialogCategory, days)
                                    showAddFoodDialog = false
                                    dialogFoodName = ""
                                    dialogQuantity = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                        ) {
                            Text("Add to Stock", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // DONATE FOOD DIALOG
    if (showDonateFoodDialog) {
        Dialog(onDismissRequest = { showDonateFoodDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Donate Surplus Food ❤️", style = MaterialTheme.typography.titleLarge.copy(color = SoftGold, fontWeight = FontWeight.Bold))
                    Text("Instantly list a meal donation request for local partner NGOs.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = dialogFoodName,
                        onValueChange = { dialogFoodName = it },
                        label = { Text("Meal Name (e.g. Cooked Rice & Curry)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dialogQuantity,
                        onValueChange = { dialogQuantity = it },
                        label = { Text("Portions (e.g. 15 Meals)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDonateFoodDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (dialogFoodName.isNotBlank() && dialogQuantity.isNotBlank()) {
                                    viewModel.createDonation(dialogFoodName, dialogQuantity, "NGO Partner")
                                    showDonateFoodDialog = false
                                    dialogFoodName = ""
                                    dialogQuantity = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                        ) {
                            Text("List Donation", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// RESTAURANT MODULE DASHBOARD
// ══════════════════════════════════════════════════════
@Composable
fun RestaurantDashboard(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    inventory: List<FoodItem>,
    wasteLogs: List<WasteLog>,
    donations: List<Donation>,
    onNavigateToPayment: (String, Int) -> Unit,
    onTabSelected: (HubTab) -> Unit
) {
    var selectedFoodType by remember { mutableStateOf("Cooked Food") }
    var inputQuantity by remember { mutableStateOf("10") }
    var inputUnit by remember { mutableStateOf("kg") }
    var inputDate by remember { mutableStateOf("21 May 2024") }
    var inputTime by remember { mutableStateOf("06:00 PM") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Commercial Kitchen Header with Verification Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Green Leaf Restaurant",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified Partner",
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${userProfile.subscription.name.replace("_", " ")} Plan",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SoftGold, fontWeight = FontWeight.SemiBold)
                    )
                }
                
                // Date Selector & Info Action Icons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("20 May – 26 May", fontSize = 11.sp, color = Color.White)
                    }
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Stats grid - 6 beautiful metric cells
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cell 1: Inventory Value
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Inventory Value", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹48,650", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(10.dp))
                                Text("12% vs last week", fontSize = 8.sp, color = Color(0xFF81C784))
                            }
                        }
                    }
                    
                    // Cell 2: Food Waste Today
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Food Waste Today", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹1,250", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(10.dp))
                                Text("8% vs yesterday", fontSize = 8.sp, color = Color(0xFF81C784))
                            }
                        }
                    }
                }

                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cell 3: Food Donated
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Food Donated", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("132.5 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(10.dp))
                                Text("18% vs last month", fontSize = 8.sp, color = Color(0xFF81C784))
                            }
                        }
                    }

                    // Cell 4: Estimated Cost Saved
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Estimated Saved", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹12,450", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(10.dp))
                                Text("15% vs last month", fontSize = 8.sp, color = Color(0xFF81C784))
                            }
                        }
                    }
                }

                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cell 5: Meals Donated
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Meals Donated", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("1,060", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(10.dp))
                                Text("20% vs last month", fontSize = 8.sp, color = Color(0xFF81C784))
                            }
                        }
                    }

                    // Cell 6: Impact Score
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Impact Score", fontSize = 10.sp, color = Color(0xFFB9CDB3))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("87/100", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoftGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Eco, contentDescription = null, tint = SoftGold, modifier = Modifier.size(10.dp))
                                Text("Excellent status", fontSize = 8.sp, color = SoftGold)
                            }
                        }
                    }
                }
            }
        }

        // Waste Analytics Curve Chart Widget
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Waste Analytics (Weekly Trend)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WasteAnalyticsCurveChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = listOf("20 May", "21 May", "22 May", "23 May", "24 May", "25 May", "26 May")
                            days.forEach { day ->
                                Text(day, fontSize = 9.sp, color = Color(0xFFB9CDB3))
                            }
                        }
                    }
                }
            }
        }

        // Waste by Category Donut Chart
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Waste by Category (This Month)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WasteCategoryPieChart(
                            modifier = Modifier
                                .size(110.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            val categories = listOf(
                                Triple("Vegetables", "35%", Color(0xFF81C784)),
                                Triple("Cooked Food", "25%", Color(0xFFFFB74D)),
                                Triple("Bakery", "15%", Color(0xFFE8C863)),
                                Triple("Dairy", "10%", Color(0xFF64B5F6)),
                                Triple("Others", "15%", Color(0xFFBA68C8))
                            )
                            
                            categories.forEach { (catName, pct, color) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(catName, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(80.dp))
                                    Text(pct, fontSize = 10.sp, color = Color(0xFFB9CDB3), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Inventory Overview Table
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Inventory Overview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    "View All Stock →",
                    style = MaterialTheme.typography.bodySmall.copy(color = SoftGold, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onTabSelected(HubTab.INVENTORY) }
                )
            }
        }

        // Inventory Overview Items
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val mockOverview = listOf(
                    Triple("Tomato", "12 kg", "2 Days Left"),
                    Triple("Paneer", "5 kg", "3 Days Left"),
                    Triple("Milk", "10 L", "4 Days Left"),
                    Triple("Bread", "30 pcs", "5 Days Left")
                )
                
                mockOverview.forEach { (name, qty, status) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(qty, color = Color(0xFFB9CDB3), fontSize = 11.sp)
                        }
                        
                        val isUrgent = status.contains("2") || status.contains("3")
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (isUrgent) Color(0xFFFFB74D).copy(alpha = 0.5f) else Color(0xFF81C784).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .background(if (isUrgent) Color(0xFFFFB74D).copy(alpha = 0.08f) else Color(0xFF81C784).copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(status, fontSize = 9.sp, color = if (isUrgent) Color(0xFFFFB74D) else Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Recent Donations Tracker List
        item {
            Text(
                "Recent Donations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val mockDonations = listOf(
                    Triple("NGO: Helping Hands", "25 kg", "20 May 2024"),
                    Triple("NGO: Food for All", "18 kg", "19 May 2024"),
                    Triple("NGO: Hope Foundation", "22 kg", "18 May 2024")
                )
                
                mockDonations.forEach { (ngo, amt, date) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(ngo, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("$amt Donated • $date", color = Color(0xFFB9CDB3), fontSize = 11.sp)
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B3B22))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Picked Up", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Donation Request Entry Form Panel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Donation Request Form",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Initiate Bulk Surplus Donation", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 13.sp)
                        
                        OutlinedTextField(
                            value = selectedFoodType,
                            onValueChange = { selectedFoodType = it },
                            label = { Text("Food Type (e.g. Cooked Rice)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputQuantity,
                                onValueChange = { inputQuantity = it },
                                label = { Text("Quantity") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = inputUnit,
                                onValueChange = { inputUnit = it },
                                label = { Text("Unit") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = SoftGold, unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = SoftGold, unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedFoodType.isNotBlank() && inputQuantity.isNotBlank()) {
                                    viewModel.createDonation(selectedFoodType, "$inputQuantity $inputUnit", "Local NGO Hub")
                                    Toast.makeText(viewModel.getApplication(), "Created donation request for $selectedFoodType!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create Request", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Environmental Impact Metrics Row Panel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Environmental Impact (This Month)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val metrics = listOf(
                            Triple("132.5 kg", "Food Rescued", Color(0xFF81C784)),
                            Triple("1,060", "Meals Provided", SoftGold),
                            Triple("13,250 L", "Water Saved", Color(0xFF64B5F6)),
                            Triple("88.6 kg", "CO₂ Reduced", Color(0xFFBA68C8))
                        )
                        metrics.forEach { (value, label, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(label, fontSize = 8.sp, color = Color(0xFFB9CDB3), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // AI Insights Tips Box
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "AI Insights & Recommendations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "You can reduce 18% more waste with better inventory prediction.",
                            "High waste in vegetables category. Optimize Tomato & Onion procurement.",
                            "Sunday has the highest average waste. Adjust prep volume by -10%."
                        ).forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("✦", color = SoftGold, fontWeight = FontWeight.Bold)
                                Text(tip, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// NGO VOLUNTEER MODULE DASHBOARD
// ══════════════════════════════════════════════════════
@Composable
fun NgoDashboard(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    pickups: List<NgoPickup>
) {
    var acceptedPickupForNav by remember { mutableStateOf<NgoPickup?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Certified Rescue NGO Portal 🤝",
                        style = MaterialTheme.typography.titleSmall.copy(color = SoftGold, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Green Leaf Alliance",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        "Verified Partner NGO",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFA2FFA2))
                    )
                }
            }
        }

        // Active Maps Routing Simulation if a pickup is selected
        item {
            AnimatedVisibility(visible = acceptedPickupForNav != null) {
                acceptedPickupForNav?.let { pickup ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A0D)),
                        border = BorderStroke(1.dp, SoftGold)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("GPS Active Navigation 🗺️", fontWeight = FontWeight.Bold, color = SoftGold)
                                IconButton(onClick = { acceptedPickupForNav = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("Donor: ${pickup.donorName}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Address: ${pickup.address}", color = MintSoft)
                            Text("Cargo: ${pickup.foodDetails}", color = MintSoft.copy(alpha = 0.8f))
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visual simulated GPS map
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .drawBehind {
                                        // Draw simulated route curves
                                        val p = Path().apply {
                                            moveTo(50f, 200f)
                                            quadraticTo(300f, 50f, 600f, 150f)
                                        }
                                        drawPath(p, color = SoftGold, style = Stroke(width = 6f))
                                        drawCircle(Color.Green, radius = 12f, center = Offset(50f, 200f))
                                        drawCircle(SoftGold, radius = 12f, center = Offset(600f, 150f))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Live Route: 1.8 Km (4 mins left)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.completePickup(pickup)
                                    acceptedPickupForNav = null
                                    Toast.makeText(viewModel.getApplication(), "Donation Picked Up & Logged!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OliveMain)
                            ) {
                                Text("Confirm Completion & Log Distribution", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Surplus Pickup Requests",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (pickups.isEmpty()) {
            item {
                Text("No active pickup requests.", color = MintSoft.copy(alpha = 0.5f))
            }
        } else {
            items(pickups) { pickup ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pickup.donorName, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Est. ${pickup.estimatedMeals} Meals", color = SoftGold, fontWeight = FontWeight.Bold)
                        }
                        
                        Text("Food Details: ${pickup.foodDetails}", color = MintSoft)
                        Text("Address: ${pickup.address}", color = MintSoft.copy(alpha = 0.7f), fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pickup.status == "Requested") {
                                Button(
                                    onClick = { viewModel.acceptPickup(pickup) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Accept Pickup", fontSize = 11.sp, color = Color.White)
                                }
                            } else if (pickup.status == "Accepted") {
                                Button(
                                    onClick = { acceptedPickupForNav = pickup },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Start Navigation 🚀", fontSize = 11.sp, color = Color(0xFF10160B))
                                }
                            } else {
                                Text("Completed", color = Color(0xFFA2FFA2), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// ADMIN AUDITING PANEL
// ══════════════════════════════════════════════════════
@Composable
fun AdminDashboard(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    donations: List<Donation>,
    wasteLogs: List<WasteLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Secure AI Executive Panel 🔒",
                        style = MaterialTheme.typography.titleSmall.copy(color = SoftGold, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "FoodLink Admin Center",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        "System Root Authorized",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SoftGold)
                    )
                }
            }
        }

        // Live Platform statistics
        item {
            Text("Global Environmental Totals", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102818))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CO2 Prevented", fontSize = 11.sp, color = MintSoft)
                        val co2Val = donations.sumOf { it.co2SavedKg } + 1450.0
                        Text("${String.format("%.1f", co2Val)} Kg", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102818))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Meals Saved", fontSize = 11.sp, color = MintSoft)
                        val meals = donations.sumOf { it.mealsDonated } + 420
                        Text("$meals Meals", fontWeight = FontWeight.Bold, color = SoftGold)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102818))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Water Preserved", fontSize = 11.sp, color = MintSoft)
                        Text("852,000 Litres", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102818))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Trees Equivalent", fontSize = 11.sp, color = MintSoft)
                        Text("72.5 Mature", fontWeight = FontWeight.Bold, color = SoftGold)
                    }
                }
            }
        }

        // Simulate Support ticket queue
        item {
            Text("Active System Tickets Queue", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "La Piazza Restaurant: App crashed during monthly report generation" to "High Priority",
                    "John Green: Subscribed to GPay basic but status didn't update" to "Medium Priority",
                    "Hope Soup Kitchen NGO: Google maps routing lags near Sector 4" to "Low Priority"
                ).forEach { (desc, prio) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(desc, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if ("High" in prio) Color(0xFF4C1D1D) else Color(0xFF4C3E1D))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(prio, fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// STOCK & INVENTORY MANAGEMENT SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun StockManagementScreen(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    inventory: List<FoodItem>
) {
    var foodName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Veggies") }
    var expiryDaysStr by remember { mutableStateOf("4") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Fresh Food Inventory",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Text(
                "Keep track of pantry and shelf life to minimize waste.",
                style = MaterialTheme.typography.bodySmall.copy(color = MintSoft)
            )
        }

        // Add Item Form card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Add Food Item manually", fontWeight = FontWeight.Bold, color = SoftGold)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food Item Name", color = MintSoft) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftGold,
                            unfocusedBorderColor = OliveLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity (e.g. 1.2 kg)", color = MintSoft) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftGold,
                                unfocusedBorderColor = OliveLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = expiryDaysStr,
                            onValueChange = { expiryDaysStr = it },
                            label = { Text("Expires in (Days)", color = MintSoft) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftGold,
                                unfocusedBorderColor = OliveLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Category selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Veggies", "Fruits", "Dairy", "Bakery").forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (category == cat) OliveMain else Color.White.copy(alpha = 0.05f))
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (foodName.isNotBlank() && quantity.isNotBlank()) {
                                val days = expiryDaysStr.toIntOrNull() ?: 3
                                viewModel.addFoodItem(foodName, quantity, category, days)
                                foodName = ""
                                quantity = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OliveMain)
                    ) {
                        Text("Add to Shelf", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Inventory list
        item {
            Text("Active Stock Pantry", fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (inventory.isEmpty()) {
            item {
                Text("Your pantry is completely empty! Add some items above.", color = MintSoft.copy(alpha = 0.4f))
            }
        } else {
            items(inventory) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Qty: ${item.quantity}", fontSize = 12.sp, color = MintSoft)
                            Text("Expires: ${item.expiryDate}", fontSize = 12.sp, color = if (item.isExpiringSoon) Color(0xFFFFA2A2) else SoftGold)
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.markItemAsDonated(item, "Green Leaf Alliance") }) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = "Donate", tint = SoftGold)
                        }
                        IconButton(onClick = { viewModel.deleteFoodItem(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// AI HUB CORE SCREEN (BARCODE, RECIPES, PREDICTIONS)
// ══════════════════════════════════════════════════════
@Composable
fun AiHubScreen(
    viewModel: FoodLinkViewModel,
    inventory: List<FoodItem>,
    aiLoading: Boolean,
    aiRecognition: FoodRecognitionResult?,
    recipeSuggestions: List<RecipeSuggestion>,
    wastePrediction: WastePredictionResult?
) {
    var descInput by remember { mutableStateOf("A bunch of ripening yellow bananas") }
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: AI Scan, 1: AI Recipe, 2: AI Waste Pred
    var predictFoodName by remember { mutableStateOf("Fresh Lettuce Cases") }
    var predictQty by remember { mutableStateOf("15") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Gemini AI Hub 🤖",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sub-tabs selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("AI Scan", "Eco Recipes", "Waste Predictor").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeSubTab == index) OliveMain else Color.Transparent)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (activeSubTab == index) Color.White else MintSoft
                            )
                        )
                    }
                }
            }
        }

        when (activeSubTab) {
            0 -> {
                // AI Scanner Simulation & Recognition
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Interactive Camera Port Scanner", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Visual Camera feed box with scanlines
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black, RoundedCornerShape(12.dp))
                                    .border(1.dp, SoftGold, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Simulated scanline animation pattern
                                    drawLine(
                                        color = SoftGold.copy(alpha = 0.6f),
                                        start = Offset(0f, size.height / 2f),
                                        end = Offset(size.width, size.height / 2f),
                                        strokeWidth = 4f
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Point at food items to auto-analyze", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = descInput,
                                onValueChange = { descInput = it },
                                label = { Text("Simulated item to scan", color = MintSoft) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftGold,
                                    unfocusedBorderColor = OliveLight,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { viewModel.runAiRecognition(descInput) },
                                enabled = !aiLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                } else {
                                    Text("Capture & Analyze with Gemini AI", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Show AI results
                aiRecognition?.let { res ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2F1B)),
                            border = BorderStroke(1.dp, SoftGold)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gemini AI Analysis:", fontWeight = FontWeight.Bold, color = SoftGold)
                                    if (res.isSimulated) {
                                        Text("Simulation Fallback", color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Name: ${res.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Category: ${res.category}", color = MintSoft)
                                Text("Est. Quantity: ${res.quantity}", color = MintSoft)
                                Text("Predicted Expiry: ${res.estExpiryDays} days", color = Color.White)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Ecological Impact saved: ${res.environmentalImpact}", color = SoftGold, fontSize = 12.sp)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = {
                                        viewModel.addFoodItem(res.name, res.quantity, res.category, res.estExpiryDays)
                                        viewModel.clearAiStates()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = OliveMain)
                                ) {
                                    Text("Save detected item to Pantry", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            1 -> {
                // AI Zero-Waste Recipes
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("AI Gourmet Zero-Waste Recipe Chef", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Analyzes expiring items in your pantry and designs customized gourmet dishes automatically.", color = MintSoft, fontSize = 12.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.runAiRecipeGeneration() },
                                enabled = !aiLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                } else {
                                    Text("Generate Gourmet Recipes", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                if (recipeSuggestions.isEmpty()) {
                    item {
                        Text("No suggestions generated yet.", color = MintSoft.copy(alpha = 0.5f))
                    }
                } else {
                    items(recipeSuggestions) { recipe ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(recipe.title, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                                    Text("Est. ${recipe.prepTime}", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                
                                Text("Difficulty: ${recipe.difficulty}", color = MintSoft, fontSize = 11.sp)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text("Required expiring ingredients:", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(recipe.ingredients.joinToString(", "), color = Color.White, fontSize = 12.sp)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text("Eco Chef Instructions:", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                recipe.instructions.forEachIndexed { idx, step ->
                                    Text("${idx+1}. $step", color = MintSoft, fontSize = 12.sp)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("CO2 emissions saved: ${recipe.co2PreventedKg} kg", color = Color(0xFFA2FFA2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            2 -> {
                // AI Waste Predictor
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Restaurant AI Waste Auditor", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = predictFoodName,
                                onValueChange = { predictFoodName = it },
                                label = { Text("Product / Ingredient", color = MintSoft) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftGold,
                                    unfocusedBorderColor = OliveLight,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = predictQty,
                                onValueChange = { predictQty = it },
                                label = { Text("Stock quantity units", color = MintSoft) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftGold,
                                    unfocusedBorderColor = OliveLight,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { viewModel.runAiWastePrediction(predictFoodName, predictQty.toIntOrNull() ?: 10) },
                                enabled = !aiLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                } else {
                                    Text("Audit Waste & Predict demand", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                wastePrediction?.let { pred ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3F2F1B)),
                            border = BorderStroke(1.dp, SoftGold)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("AI Waste Prediction Risk: ${pred.surplusRisk}", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Recommended Stock adjustment: ${pred.recommendedStockAdjustment}", color = Color.White)
                                Text("Predicted Waste weight: ${pred.predictedWasteKg} kg", color = MintSoft)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Saving Hack: ${pred.actionableSavingTip}", color = Color(0xFFA2FFA2), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// IMPACT DASHBOARD SCREEN (STATS & VISUALS)
// ══════════════════════════════════════════════════════
@Composable
fun ImpactDashboardScreen(
    donations: List<Donation>,
    wasteLogs: List<WasteLog>,
    viewModel: FoodLinkViewModel
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Eco Tracker, 1: Financial Support
    
    val moneyDonations by viewModel.moneyDonations.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog state controllers
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    
    // Donation form fields
    var selectedAmountPreset by remember { mutableStateOf("101") }
    var customAmountText by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(false) }
    var selectedNgo by remember { mutableStateOf("Robin Hood Food Rescue") }
    
    // Dynamic calculation helper
    val activeAmount: Double = if (selectedAmountPreset == "Custom") {
        customAmountText.toDoubleOrNull() ?: 0.0
    } else {
        selectedAmountPreset.toDoubleOrNull() ?: 0.0
    }

    // Active donation details being processed
    var pendingDonationAmount by remember { mutableDoubleStateOf(0.0) }
    var pendingDonationNgo by remember { mutableStateOf("") }
    var pendingDonationMethod by remember { mutableStateOf("Google Pay") }
    var pendingDonationIsRecurring by remember { mutableStateOf(false) }
    var pendingDonationIsAnonymous by remember { mutableStateOf(false) }
    var lastCreatedTransactionId by remember { mutableStateOf("TXN00000000") }
    var lastCreatedTimestamp by remember { mutableLongStateOf(0L) }
    
    // Viewing specific historical receipts
    var selectedReceiptForDialog by remember { mutableStateOf<MoneyDonation?>(null) }
    
    // Admin override mode
    var adminOverrideMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Module Heading
        Text(
            "Impact & Support Hub 🌱",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        Text(
            "Track our ecological savings or contribute financially to end hunger.",
            style = MaterialTheme.typography.bodySmall.copy(color = MintSoft)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Custom Capsule Selector (Eco vs Financial)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 0) SoftGold.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { subTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Eco-Impact Tracker",
                    fontWeight = FontWeight.Bold,
                    color = if (subTab == 0) SoftGold else Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 1) SoftGold.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { subTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (subTab == 1) SoftGold else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp).padding(end = 2.dp)
                    )
                    Text(
                        "Donate Funds",
                        fontWeight = FontWeight.Bold,
                        color = if (subTab == 1) SoftGold else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated Screen Selector
        if (subTab == 0) {
            // ══════════════════════════════════════════════════════
            // TAB 1: ECO IMPACT TRACKER
            // ══════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Carbon visual chart drawing
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("CO2 Reduced (Kg) - Last 6 Months", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Canvas chart drawing
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    
                                    // Draw grid lines
                                    drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, h * 0.25f), Offset(w, h * 0.25f))
                                    drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f))
                                    drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, h * 0.75f), Offset(w, h * 0.75f))
                                    
                                    // Draw carbon curve
                                    val path = Path().apply {
                                        moveTo(0f, h * 0.8f)
                                        cubicTo(w * 0.2f, h * 0.6f, w * 0.4f, h * 0.3f, w * 0.6f, h * 0.4f)
                                        cubicTo(w * 0.8f, h * 0.5f, w * 0.9f, h * 0.1f, w, h * 0.15f)
                                    }
                                    drawPath(path, color = SoftGold, style = Stroke(width = 6f))
                                    
                                    // Draw points
                                    drawCircle(SoftGold, radius = 8f, center = Offset(w * 0.6f, h * 0.4f))
                                    drawCircle(SoftGold, radius = 8f, center = Offset(w, h * 0.15f))
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun").forEach { month ->
                                    Text(month, fontSize = 10.sp, color = MintSoft)
                                }
                            }
                        }
                    }
                }

                // Metrics breakdown
                item {
                    val totalSaved = donations.filter { it.status == "Completed" }.sumOf { it.mealsDonated } + 10
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Impact Metrics Summary", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Meals Saved", fontSize = 11.sp, color = MintSoft)
                                    Text("$totalSaved", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = SoftGold))
                                }
                                
                                Column {
                                    Text("Water Preserved", fontSize = 11.sp, color = MintSoft)
                                    Text("${totalSaved * 250} Litres", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                }
                                
                                Column {
                                    Text("Landfill Prevented", fontSize = 11.sp, color = MintSoft)
                                    Text("${String.format("%.1f", totalSaved * 0.5)} Kg", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFFA2FFA2)))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ══════════════════════════════════════════════════════
            // TAB 2: FINANCIAL SUPPORT CORE
            // ══════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Transparency Report & Total Raised
                item {
                    val raisedFromDb = moneyDonations.filter { it.status == "Success" }.sumOf { it.amount }
                    val totalRaised = raisedFromDb + 32450.0 // Base realistic donation pool

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Transparency Hub 🛡️", style = MaterialTheme.typography.titleSmall.copy(color = SoftGold, fontWeight = FontWeight.Bold))
                                    Text("Total funds pooled securely", fontSize = 11.sp, color = MintSoft)
                                }
                                Text(
                                    "₹${String.format("%,.2f", totalRaised)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF6BFF73))
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Fund Allocation Breakdown", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Visual horizontal gauge bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                Box(modifier = Modifier.weight(0.75f).background(Color(0xFF81C784))) // 75% Meals
                                Box(modifier = Modifier.weight(0.15f).background(Color(0xFF64B5F6))) // 15% Cold Chain Logistics
                                Box(modifier = Modifier.weight(0.10f).background(Color(0xFFFFB74D))) // 10% Operations
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Legend keys
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF81C784)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("75% Hunger Relief", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF64B5F6)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("15% Cold Logistics", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFB74D)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("10% Ops & Admin", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                // Interactive Donation Form
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Support NGO Initiatives", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                
                                // One-time vs Monthly Switch
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (!isRecurring) SoftGold.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { isRecurring = false }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("One-Time", fontSize = 10.sp, color = if (!isRecurring) SoftGold else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isRecurring) SoftGold.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { isRecurring = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Monthly", fontSize = 10.sp, color = if (isRecurring) SoftGold else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // NGO Target dropdown selector
                            Text("Recipient NGO", fontSize = 11.sp, color = MintSoft)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedNgo, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Row {
                                    listOf("Robin Hood", "Green Leaf", "Hope Feed", "No Hungry Child").forEach { shortName ->
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (selectedNgo.contains(shortName)) SoftGold.copy(alpha = 0.25f) 
                                                    else Color.White.copy(alpha = 0.05f)
                                                )
                                                .clickable {
                                                    selectedNgo = when (shortName) {
                                                        "Robin Hood" -> "Robin Hood Food Rescue"
                                                        "Green Leaf" -> "Green Leaf Kitchen NGO"
                                                        "Hope Feed" -> "Hope Feed Center"
                                                        else -> "No Hungry Child Initiative"
                                                    }
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(shortName, fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Preset Amount Grid (₹10, ₹19, ₹51, ₹101, ₹501, ₹1001, Custom)
                            Text("Select Contribution Amount", fontSize = 11.sp, color = MintSoft)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val presets = listOf("10", "19", "51", "101", "501", "1001", "Custom")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.take(4).forEach { preset ->
                                    val isSel = selectedAmountPreset == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SoftGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f))
                                            .border(1.dp, if (isSel) SoftGold else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .clickable { selectedAmountPreset = preset }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("₹$preset", fontWeight = FontWeight.Bold, color = if (isSel) SoftGold else Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.drop(4).forEach { preset ->
                                    val isSel = selectedAmountPreset == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) SoftGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f))
                                            .border(1.dp, if (isSel) SoftGold else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .clickable { selectedAmountPreset = preset }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (preset == "Custom") "Custom" else "₹$preset", fontWeight = FontWeight.Bold, color = if (isSel) SoftGold else Color.White)
                                    }
                                }
                            }
                            
                            if (selectedAmountPreset == "Custom") {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = customAmountText,
                                    onValueChange = { customAmountText = it.filter { char -> char.isDigit() } },
                                    label = { Text("Enter custom amount (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SoftGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = SoftGold,
                                        unfocusedLabelColor = MintSoft,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // IMPACT CALCULATOR DISPLAY
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SoftGold.copy(alpha = 0.06f)),
                                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Impact Calculator",
                                        tint = SoftGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Your Contribution Impact:", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 11.sp)
                                        Text(
                                            text = when {
                                                activeAmount <= 0.0 -> "Please choose or enter a valid support amount."
                                                activeAmount < 19.0 -> "Provides direct financial support for NGO administrative overhead to keep programs active."
                                                activeAmount == 19.0 -> "Supports exactly 1 full wholesome meal for a child in need! 🍛"
                                                activeAmount < 101.0 -> "Helps rescue and distribute approx. ${String.format("%.1f", activeAmount / 19)} freshly made surplus meals."
                                                activeAmount == 101.0 -> "Helps rescue and redistribute multiple surplus meals, saving approx 12.5 Kg carbon emissions!"
                                                activeAmount < 501.0 -> "Sponsors cold collection logs and feeds ${String.format("%.0f", activeAmount / 19)} children! 🍎"
                                                activeAmount == 501.0 -> "Sponsors temperature-controlled logistics to collect and distribute 110 Kg of fresh surplus food."
                                                activeAmount < 1001.0 -> "Feeds over ${String.format("%.0f", activeAmount / 19)} people and funds critical NGO collection routes."
                                                else -> "Feeds over ${String.format("%.0f", activeAmount / 19)} hungry souls, rescues massive surplus, and sponsors 1 entire cold distribution run! 🚚"
                                            },
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Anonymous checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAnonymous,
                                    onCheckedChange = { isAnonymous = it },
                                    colors = CheckboxDefaults.colors(checkedColor = SoftGold, uncheckedColor = Color.White.copy(alpha = 0.6f))
                                )
                                Text(
                                    "Make donation anonymous (hides my name from the Leaderboard)",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // SUBMIT DONATION BUTTON
                            Button(
                                onClick = {
                                    if (activeAmount > 0) {
                                        pendingDonationAmount = activeAmount
                                        pendingDonationNgo = selectedNgo
                                        pendingDonationIsRecurring = isRecurring
                                        pendingDonationIsAnonymous = isAnonymous
                                        showPaymentDialog = true
                                    } else {
                                        Toast.makeText(context, "Please select or enter a valid amount", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("secure_donate_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF131A0D))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Secure Checkout ₹${String.format("%.0f", activeAmount)}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Generous Donors Leaderboard
                item {
                    val staticLeaderboard = listOf(
                        "Sarah Jenkins" to 1502.0,
                        "La Piazza Bakery" to 1001.0,
                        "Grand Banquet Hall" to 501.0,
                        "Michael Vance" to 251.0
                    )
                    
                    // Merge user transactions to leaderboard if they aren't anonymous
                    val userSuccessSum = moneyDonations
                        .filter { it.status == "Success" && !it.isAnonymous && it.donorName == userProfile.name }
                        .sumOf { it.amount }
                        
                    val combinedLeaders = (staticLeaderboard + (userProfile.name to userSuccessSum))
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                        .take(5)

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Generous Leaderboard 🏆", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Publicly updated", fontSize = 10.sp, color = MintSoft)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            combinedLeaders.forEachIndexed { idx, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(if (pair.first == userProfile.name) SoftGold.copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (idx) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                2 -> "🥉"
                                                else -> "  ${idx + 1}. "
                                            },
                                            modifier = Modifier.width(32.dp)
                                        )
                                        Text(
                                            text = if (pair.first == userProfile.name) "${pair.first} (You)" else pair.first,
                                            fontWeight = if (pair.first == userProfile.name) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text("₹${String.format("%.0f", pair.second)}", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // User's Donation History List
                item {
                    val userDonations = moneyDonations
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Your Contribution History 📜", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (userDonations.isEmpty()) {
                                Text(
                                    "You haven't made any financial contributions yet. Be the first to secure a meal! 🙏",
                                    fontSize = 12.sp,
                                    color = MintSoft,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                userDonations.forEach { historyItem ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(historyItem.ngoName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("₹${String.format("%.0f", historyItem.amount)}", fontSize = 13.sp, color = SoftGold, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (historyItem.status == "Success") Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFEF6C00).copy(alpha = 0.2f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(historyItem.status, fontSize = 8.sp, color = if (historyItem.status == "Success") Color(0xFF81C784) else Color(0xFFFFB74D))
                                                    }
                                                }
                                                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(historyItem.timestamp))
                                                Text(dateStr, fontSize = 9.sp, color = MintSoft)
                                            }
                                            
                                            // View Receipt Actions
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        selectedReceiptForDialog = historyItem
                                                        showReceiptDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ReceiptLong, contentDescription = "Receipt", tint = SoftGold, modifier = Modifier.size(16.dp))
                                                }
                                                if (historyItem.status == "Success") {
                                                    IconButton(
                                                        onClick = {
                                                            pendingDonationAmount = historyItem.amount
                                                            pendingDonationNgo = historyItem.ngoName
                                                            pendingDonationIsAnonymous = historyItem.isAnonymous
                                                            pendingDonationIsRecurring = historyItem.isRecurring
                                                            lastCreatedTransactionId = historyItem.transactionId
                                                            lastCreatedTimestamp = historyItem.timestamp
                                                            showCertificateDialog = true
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.CardMembership, contentDescription = "Certificate", tint = Color(0xFFA2FFA2), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ADMIN VERIFICATION / OVERRIDE PANEL
                if (userProfile.role == UserRole.ADMIN) {
                    item {
                        val isAdmin = userProfile.role == UserRole.ADMIN
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("System Administrator Mode", fontSize = 12.sp, color = MintSoft, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = adminOverrideMode || isAdmin,
                                onCheckedChange = { adminOverrideMode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = SoftGold, checkedTrackColor = SoftGold.copy(alpha = 0.3f))
                            )
                        }
                        
                        if (adminOverrideMode || isAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Admin Control Panel ⚙️", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 14.sp)
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "Donations report exported to: FoodLink_Donations_Report.csv", Toast.LENGTH_LONG).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            modifier = Modifier.height(28.dp).padding(0.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Download, null, modifier = Modifier.size(12.dp), tint = SoftGold)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Export CSV", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Global Pending Transactions Verification:", fontSize = 11.sp, color = MintSoft)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val allPending = moneyDonations.filter { it.status == "Pending" }
                                    if (allPending.isEmpty()) {
                                        Text("No pending transactions awaiting admin verification.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                    } else {
                                        allPending.forEach { pendingTx ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("${pendingTx.donorName} → ${pendingTx.ngoName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                                        Text("Amount: ₹${pendingTx.amount} | ID: ${pendingTx.transactionId}", fontSize = 10.sp, color = SoftGold)
                                                        Text("Gateway: ${pendingTx.paymentMethod}", fontSize = 9.sp, color = MintSoft)
                                                    }
                                                    Row {
                                                        Button(
                                                            onClick = {
                                                                viewModel.updateMoneyDonationStatus(pendingTx, "Success")
                                                                Toast.makeText(context, "Transaction Approved", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                            contentPadding = PaddingValues(horizontal = 6.dp),
                                                            modifier = Modifier.height(26.dp).padding(end = 4.dp)
                                                        ) {
                                                            Text("Approve", fontSize = 9.sp, color = Color.White)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                viewModel.updateMoneyDonationStatus(pendingTx, "Failed")
                                                                Toast.makeText(context, "Transaction Rejected", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                            contentPadding = PaddingValues(horizontal = 6.dp),
                                                            modifier = Modifier.height(26.dp)
                                                        ) {
                                                            Text("Reject", fontSize = 9.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════
    // DIALOG 1: PAYMENT GATEWAY CHOOSE FLOW
    // ══════════════════════════════════════════════════════
    if (showPaymentDialog) {
        var selectedPaymentMethod by remember { mutableStateOf("Google Pay (UPI)") }
        var isSimulatingPayment by remember { mutableStateOf(false) }
        var mockCardNumber by remember { mutableStateOf("") }
        var mockCardExpiry by remember { mutableStateOf("") }
        var mockCardCvv by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isSimulatingPayment) showPaymentDialog = false },
            containerColor = Color(0xFF192211),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SoftGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Gateway", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose your preferred secure payment method for ₹${pendingDonationAmount}:", fontSize = 12.sp, color = Color.White)
                    
                    if (isSimulatingPayment) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SoftGold)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Simulating secure sandbox encryption...", fontSize = 11.sp, color = MintSoft)
                            Text("Connecting via Razorpay secure layers...", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    } else {
                        val paymentOptions = listOf(
                            "Google Pay (UPI)" to Icons.Default.Smartphone,
                            "Paytm" to Icons.Default.QrCode,
                            "PhonePe" to Icons.Default.FlashOn,
                            "BHIM UPI" to Icons.Default.AccountBalance,
                            "Credit/Debit Card" to Icons.Default.CreditCard,
                            "Net Banking" to Icons.Default.Language,
                            "Razorpay Sandbox Gateway" to Icons.Default.Security
                        )
                        
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(paymentOptions) { (option, icon) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedPaymentMethod == option) SoftGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, if (selectedPaymentMethod == option) SoftGold else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedPaymentMethod = option }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = if (selectedPaymentMethod == option) SoftGold else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(option, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                        
                        if (selectedPaymentMethod == "Credit/Debit Card") {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = mockCardNumber,
                                onValueChange = { mockCardNumber = it.take(16) },
                                label = { Text("Card Number", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SoftGold, focusedBorderColor = SoftGold)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = mockCardExpiry,
                                    onValueChange = { mockCardExpiry = it.take(5) },
                                    label = { Text("Expiry (MM/YY)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SoftGold, focusedBorderColor = SoftGold)
                                )
                                OutlinedTextField(
                                    value = mockCardCvv,
                                    onValueChange = { mockCardCvv = it.take(3) },
                                    label = { Text("CVV", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SoftGold, focusedBorderColor = SoftGold)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isSimulatingPayment) {
                    Button(
                        onClick = {
                            isSimulatingPayment = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2200) // Simulated checkout latency
                                isSimulatingPayment = false
                                showPaymentDialog = false
                                
                                // Generate receipt states
                                lastCreatedTransactionId = "TXN" + System.currentTimeMillis().toString().takeLast(9) + (100..999).random()
                                lastCreatedTimestamp = System.currentTimeMillis()
                                pendingDonationMethod = selectedPaymentMethod
                                
                                // Add to Room DB!
                                viewModel.addMoneyDonation(
                                    amount = pendingDonationAmount,
                                    donorName = userProfile.name,
                                    ngoName = pendingDonationNgo,
                                    isAnonymous = pendingDonationIsAnonymous,
                                    isRecurring = pendingDonationIsRecurring,
                                    paymentMethod = selectedPaymentMethod,
                                    status = "Success"
                                )
                                
                                showThankYouDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF131A0D))
                    ) {
                        Text("Pay Securely", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isSimulatingPayment) {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════
    // DIALOG 2: THANK YOU MODAL (WITH ACCENT ANIMATIONS)
    // ══════════════════════════════════════════════════════
    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = { showThankYouDialog = false },
            containerColor = Color(0xFF111E0D),
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success checkmark",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(54.dp)
                )
            },
            title = {
                Text(
                    "Thank You For Saving Lives!",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Your transaction was processed successfully. 100% of these funds are directly assigned to end local food scarcity.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // DIGITAL RECEIPT BREAKDOWN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("OFFICIAL DIGITAL RECEIPT", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transaction ID", fontSize = 10.sp, color = MintSoft)
                                Text(lastCreatedTransactionId, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Donor Name", fontSize = 10.sp, color = MintSoft)
                                Text(if (pendingDonationIsAnonymous) "Anonymous Benefactor" else userProfile.name, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Recipient NGO", fontSize = 10.sp, color = MintSoft)
                                Text(pendingDonationNgo, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Contributed Amount", fontSize = 10.sp, color = MintSoft)
                                Text("₹${pendingDonationAmount}", fontSize = 11.sp, color = SoftGold, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Billing Profile", fontSize = 10.sp, color = MintSoft)
                                Text(if (pendingDonationIsRecurring) "Monthly Support" else "One-time Donation", fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Method", fontSize = 10.sp, color = MintSoft)
                                Text(pendingDonationMethod, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Date / Status", fontSize = 10.sp, color = MintSoft)
                                Text("Completed / Secure", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showThankYouDialog = false
                        showCertificateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF131A0D))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardMembership, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Certificate", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showThankYouDialog = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════
    // DIALOG 3: DETAILED RECEIPT RETRIEVAL DIALOG
    // ══════════════════════════════════════════════════════
    if (showReceiptDialog && selectedReceiptForDialog != null) {
        val rItem = selectedReceiptForDialog!!
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            containerColor = Color(0xFF161F10),
            title = { Text("Transaction Details", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("FOODLINK OFFICIAL BILLING RECEIPT", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Receipt Reference", fontSize = 10.sp, color = MintSoft)
                                Text(rItem.transactionId, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Donor", fontSize = 10.sp, color = MintSoft)
                                Text(rItem.donorName, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Assigned Partner NGO", fontSize = 10.sp, color = MintSoft)
                                Text(rItem.ngoName, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Billing Subscriptions", fontSize = 10.sp, color = MintSoft)
                                Text(if (rItem.isRecurring) "Monthly Recurring" else "One-Time", fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gateway Vendor", fontSize = 10.sp, color = MintSoft)
                                Text(rItem.paymentMethod, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount", fontSize = 10.sp, color = MintSoft)
                                Text("₹${rItem.amount}", fontSize = 11.sp, color = SoftGold, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Date / Time", fontSize = 10.sp, color = MintSoft)
                                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(rItem.timestamp))
                                Text(dateStr, fontSize = 10.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Verification Status", fontSize = 10.sp, color = MintSoft)
                                Text(rItem.status, fontSize = 10.sp, color = if (rItem.status == "Success") Color(0xFF81C784) else Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Launch Certificate from inside receipt
                        pendingDonationAmount = rItem.amount
                        pendingDonationNgo = rItem.ngoName
                        pendingDonationIsAnonymous = rItem.isAnonymous
                        pendingDonationIsRecurring = rItem.isRecurring
                        lastCreatedTransactionId = rItem.transactionId
                        lastCreatedTimestamp = rItem.timestamp
                        showReceiptDialog = false
                        showCertificateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF131A0D))
                ) {
                    Text("Certificate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReceiptDialog = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════
    // DIALOG 4: STUNNING APPRECIATION CERTIFICATE (PDF LOOKALIKE)
    // ══════════════════════════════════════════════════════
    if (showCertificateDialog) {
        val certId = "FL-CERT-" + lastCreatedTransactionId.takeLast(7).uppercase()
        val formattedDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(lastCreatedTimestamp))
        
        Dialog(onDismissRequest = { showCertificateDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFCFBF7)) // Premium Cream Paper background
                    .border(BorderStroke(4.dp, Brush.sweepGradient(listOf(Color(0xFFD4AF37), Color(0xFFC5A02B), Color(0xFFE5C158), Color(0xFFD4AF37)))), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Golden Ribbon / Star emblem
                    Icon(
                        imageVector = Icons.Default.CardMembership,
                        contentDescription = null,
                        tint = Color(0xFFC5A02B),
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Text(
                        "CERTIFICATE OF APPRECIATION",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF332B15)),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "FOODLINK SUSTAINABILITY IMPACT ALLIANCE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC5A02B),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    HorizontalDivider(modifier = Modifier.width(100.dp), color = Color(0xFFC5A02B).copy(alpha = 0.4f))
                    
                    Text(
                        "THIS CERTIFICATE IS PROUDLY CONFERRED UPON",
                        fontSize = 7.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = if (pendingDonationIsAnonymous) "OUR ANONYMOUS BENEFACTOR" else userProfile.name.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3516),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "For their selfless and noble financial contribution of ₹${String.format("%.0f", pendingDonationAmount)} supporting ${pendingDonationNgo}. This humanitarian fund successfully secured vital nutritional resources, rescued farm surplus, and directly prevented high-impact methane emissions from municipal landfills.",
                        fontSize = 9.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formattedDate, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF332B15))
                            Text("DATE OF CONFERRAL", fontSize = 6.sp, color = Color.Gray)
                        }
                        
                        // Golden Seal Symbol
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5C158))
                                .border(1.dp, Color(0xFFC5A02B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Eco, null, tint = Color(0xFF1E3516), modifier = Modifier.size(16.dp))
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(certId, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF332B15))
                            Text("CERTIFICATE ID", fontSize = 6.sp, color = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Action Buttons on Certificate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Certificate PDF downloaded to Downloads/${certId}.pdf", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3516)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save PDF", fontSize = 11.sp, color = Color.White)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { showCertificateDialog = false },
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// PROFILE & SUBSCRIPTION MANAGEMENT SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun ProfileSettingsScreen(
    viewModel: FoodLinkViewModel,
    userProfile: UserProfile,
    onNavigateToPayment: (String, Int) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(2.dp, SoftGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Avatar", tint = SoftGold, modifier = Modifier.size(50.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(userProfile.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                    Text(userProfile.email, color = MintSoft, fontSize = 13.sp)
                }
            }
        }

        // Subscriptions Management
        item {
            Text("Subscription Plan", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(userProfile.subscription.name.replace("_", " "), fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = when(userProfile.subscription) {
                                SubscriptionType.FREE -> "Basic access"
                                SubscriptionType.HOME_BASIC -> "₹19/month - Expiry prediction active"
                                SubscriptionType.RESTAURANT_PREMIUM -> "₹999/month - Commercial tools active"
                                SubscriptionType.PREMIUM_COGNITIVE -> "₹2999/month - Unlimited AI auditing"
                            },
                            color = MintSoft,
                            fontSize = 11.sp
                        )
                    }
                    
                    Button(
                        onClick = { onNavigateToPayment("Premium AI cognitive", 2999) },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upgrade", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Firebase & Database Backend Systems Hub
        item {
            val context = LocalContext.current
            val activity = context as? android.app.Activity
            
            val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsStateWithLifecycle()
            val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
            val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
            
            val isOtpSent by viewModel.isOtpSent.collectAsStateWithLifecycle()
            val isOtpVerifying by viewModel.isOtpVerifying.collectAsStateWithLifecycle()
            val otpError by viewModel.otpError.collectAsStateWithLifecycle()
            val isUserLoggedInWithOtp by viewModel.isUserLoggedInWithOtp.collectAsStateWithLifecycle()
            
            var phoneInputText by remember { mutableStateOf("") }
            var otpInputText by remember { mutableStateOf("") }

            Text("Firebase Backend Control Hub ⚡", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(2.dp)) {
                    // System Status Grid Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Backend Cloud Gateway", fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 14.sp)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirebaseConnected) Color(0xFF81C784) else Color(0xFFFFB74D))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFirebaseConnected) "Connected" else "Secure Offline Cache",
                                fontSize = 11.sp,
                                color = if (isFirebaseConnected) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Display Badge/Grid representing each of the 7 Firebase services
                    Text("Configured Firebase Systems:", fontSize = 10.sp, color = MintSoft)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Auth OTP" to Icons.Default.Lock,
                            "Firestore" to Icons.Default.Storage,
                            "Cloud Storage" to Icons.Default.CloudUpload,
                            "Cloud Messaging" to Icons.Default.NotificationsActive,
                            "Analytics" to Icons.Default.BarChart,
                            "Crashlytics" to Icons.Default.BugReport,
                            "App Check" to Icons.Default.VerifiedUser
                        ).forEach { (service, icon) ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = SoftGold, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(service, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 1: Firebase OTP Mobile Authentication
                    Text("Secure OTP Mobile Identity", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text("Authenticate securely using Firebase verification.", fontSize = 11.sp, color = MintSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isUserLoggedInWithOtp) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3B22))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Logged In via Firebase OTP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                        Text("Verified Session Active", fontSize = 9.sp, color = Color(0xFFC8E6C9))
                                    }
                                }
                                Button(
                                    onClick = { viewModel.signOutFirebase() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C1D1D)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Sign Out", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    } else {
                        if (!isOtpSent) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = phoneInputText,
                                    onValueChange = { phoneInputText = it },
                                    label = { Text("Mobile No (e.g. +919876543210)") },
                                    placeholder = { Text("+91...") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SoftGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                        focusedLabelColor = SoftGold,
                                        unfocusedLabelColor = MintSoft,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (phoneInputText.isNotBlank()) {
                                            if (activity != null) {
                                                viewModel.sendFirebaseOtp(phoneInputText, activity)
                                            } else {
                                                Toast.makeText(context, "Activity context not found", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
                                    enabled = !isOtpVerifying,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.7f).height(54.dp).testTag("otp_send_button")
                                ) {
                                    Text("Send OTP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            // OTP input state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = otpInputText,
                                    onValueChange = { otpInputText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Enter 6-Digit OTP") },
                                    placeholder = { Text("123456") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SoftGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                        focusedLabelColor = SoftGold,
                                        unfocusedLabelColor = MintSoft,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (otpInputText.length == 6) {
                                            viewModel.verifyFirebaseOtp(otpInputText)
                                        } else {
                                            Toast.makeText(context, "Please enter 6 digit code", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                                    enabled = !isOtpVerifying,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.7f).height(54.dp).testTag("otp_verify_button")
                                ) {
                                    Text("Verify", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Didn't receive? Click to restart verification.",
                                fontSize = 10.sp,
                                color = SoftGold,
                                modifier = Modifier.clickable { viewModel.signOutFirebase() }
                            )
                        }
                    }

                    otpError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            fontSize = 11.sp,
                            color = if (err.contains("successful") || err.contains("Verification")) Color(0xFF81C784) else SoftGold,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 2: 17 Firestore Collections Sync Engine
                    Text("Secure 17-Collection Cloud Backup", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text(
                        "Synchronizes: Users, Restaurants, Hotels, NGOs, Admins, Inventory, Donations, Waste, Payments, Subscriptions, Notifications, Recipes, Analytics, Pickups, and Settings.",
                        fontSize = 11.sp,
                        color = MintSoft
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSyncingCloud) {
                        Column {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                                color = SoftGold,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(syncStatusMessage, fontSize = 11.sp, color = SoftGold, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.triggerFullCloudSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF131A0D)),
                            modifier = Modifier.fillMaxWidth().testTag("trigger_cloud_backup_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trigger Secure Cloud Backup (17 Collections)", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (syncStatusMessage != "Cloud Sync Offline" && syncStatusMessage != "Idle") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(syncStatusMessage, fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Help, Terms & Legal
        item {
            Text("Information & Legals", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "About FoodLink AI" to "Version 1.0.2 Premium",
                    "Privacy Policy & Terms" to "Last updated June 2026",
                    "Secure Backup Services" to "Automated Cloud Storage Logs"
                ).forEach { (label, detail) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = Color.White)
                        Text(detail, color = SoftGold, fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C1D1D)),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Logout from Portal", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// 8. PAYMENT GATEWAY SELECTION SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun PaymentGatewayScreen(
    planTitle: String,
    price: Int,
    selectedGateway: String,
    onGatewaySelected: (String) -> Unit,
    onPay: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Payment, contentDescription = null, tint = SoftGold, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Secure FoodLink Checkout", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text("Your micro-payment fuels natural preservation.", color = MintSoft)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(planTitle, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Auto-renewal Monthly", fontSize = 12.sp, color = MintSoft)
                    }
                    Text("₹$price", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = SoftGold))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Payment Gateway", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Gateway selectors
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Google Pay", "PhonePe", "Paytm", "Razorpay Checkout").forEach { gateway ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedGateway == gateway) OliveMain else Color.White.copy(alpha = 0.05f))
                            .clickable { onGatewaySelected(gateway) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(gateway, fontWeight = FontWeight.Bold, color = Color.White)
                        RadioButton(
                            selected = selectedGateway == gateway,
                            onClick = { onGatewaySelected(gateway) },
                            colors = RadioButtonDefaults.colors(selectedColor = SoftGold, unselectedColor = OliveLight)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onPay,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("pay_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text("Pay Securely ₹$price", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ══════════════════════════════════════════════════════
// 9. PAYMENT SUCCESS RECEIPT SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun PaymentSuccessScreen(
    planTitle: String,
    price: Int,
    gateway: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFA2FFA2),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Transaction Successful!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        
        Text(
            text = "Thank you for supporting zero-waste food rescue ecosystems.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MintSoft),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Receipt Details", fontWeight = FontWeight.Bold, color = SoftGold)
                
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subscribed Plan:", color = MintSoft)
                    Text(planTitle, color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amount Paid:", color = MintSoft)
                    Text("₹$price", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Payment Channel:", color = MintSoft)
                    Text(gateway, color = Color.White)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status:", color = MintSoft)
                    Text("Auto-Renewal Active", color = Color(0xFFA2FFA2), fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("done_button"),
            colors = ButtonDefaults.buttonColors(containerColor = OliveMain),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
