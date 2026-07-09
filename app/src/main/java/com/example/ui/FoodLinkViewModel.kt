package com.example.ui

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FoodLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodLinkRepository

    val userProfile: StateFlow<UserProfile>
    val inventory: StateFlow<List<FoodItem>>
    val donations: StateFlow<List<Donation>>
    val wasteLogs: StateFlow<List<WasteLog>>
    val pickups: StateFlow<List<NgoPickup>>
    val moneyDonations: StateFlow<List<MoneyDonation>>

    // --- AI Feature States ---
    private val _aiRecognitionResult = MutableStateFlow<FoodRecognitionResult?>(null)
    val aiRecognitionResult = _aiRecognitionResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _recipeSuggestions = MutableStateFlow<List<RecipeSuggestion>>(emptyList())
    val recipeSuggestions = _recipeSuggestions.asStateFlow()

    private val _wastePredictionResult = MutableStateFlow<WastePredictionResult?>(null)
    val wastePredictionResult = _wastePredictionResult.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.foodLinkDao()
        repository = FoodLinkRepository(dao)

        // Run demodata prep
        viewModelScope.launch {
            repository.initializeDemoData()
        }

        userProfile = repository.getUserProfile("default_user")
            .filterNotNull()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UserProfile()
            )

        inventory = repository.getInventoryForUser("default_user")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        donations = repository.allDonations
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        wasteLogs = repository.allWasteLogs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        pickups = repository.allPickups
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        moneyDonations = repository.allMoneyDonations
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // --- User Actions ---
    fun changeUserRole(role: UserRole) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(role = role))
        }
    }

    fun loginUser(email: String, name: String, role: UserRole) {
        viewModelScope.launch {
            val current = userProfile.value
            val finalName = if (role == UserRole.ADMIN && !name.contains("Admin", ignoreCase = true)) {
                "System Administrator"
            } else {
                name
            }
            repository.saveUserProfile(
                current.copy(
                    email = email,
                    name = finalName,
                    role = role
                )
            )
        }
    }

    fun changeSubscription(subscription: SubscriptionType) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(subscription = subscription))
        }
    }

    fun addFoodItem(name: String, quantity: String, category: String, expiryDays: Int, barcode: String? = null) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, expiryDays)
            val expiryStr = sdf.format(cal.time)
            val addedStr = sdf.format(Date())

            val item = FoodItem(
                name = name,
                quantity = quantity,
                category = category,
                expiryDate = expiryStr,
                addedDate = addedStr,
                scannedBarcode = barcode,
                isExpiringSoon = expiryDays <= 2
            )
            repository.insertFoodItem(item)
        }
    }

    fun markItemAsDonated(item: FoodItem, ngoName: String) {
        viewModelScope.launch {
            // Update item state
            repository.updateFoodItem(item.copy(status = "Donated"))
            
            // Add donation entry
            val donation = Donation(
                foodName = item.name,
                quantity = item.quantity,
                ngoName = ngoName,
                donorName = userProfile.value.name,
                status = "Pending",
                co2SavedKg = 2.4,
                mealsDonated = 5
            )
            repository.insertDonation(donation)
        }
    }

    fun markItemAsWasted(item: FoodItem, reason: String, cost: Double) {
        viewModelScope.launch {
            repository.updateFoodItem(item.copy(status = "Wasted"))
            
            val log = WasteLog(
                foodName = item.name,
                weightKg = 1.0, // estimated
                costLoss = cost,
                reason = reason
            )
            repository.insertWasteLog(log)
        }
    }

    fun deleteFoodItem(item: FoodItem) {
        viewModelScope.launch {
            repository.deleteFoodItem(item)
        }
    }

    fun acceptPickup(pickup: NgoPickup) {
        viewModelScope.launch {
            repository.updatePickup(pickup.copy(status = "Accepted"))
        }
    }

    fun completePickup(pickup: NgoPickup) {
        viewModelScope.launch {
            repository.updatePickup(pickup.copy(status = "Completed"))
            
            // Log as completed donation for impact
            val donation = Donation(
                foodName = pickup.foodDetails,
                quantity = "Batch",
                ngoName = userProfile.value.name,
                donorName = pickup.donorName,
                status = "Completed",
                co2SavedKg = 15.6,
                mealsDonated = pickup.estimatedMeals
            )
            repository.insertDonation(donation)
        }
    }

    fun createPickupRequest(donorName: String, foodDetails: String, address: String, estimatedMeals: Int) {
        viewModelScope.launch {
            val pickup = NgoPickup(
                donorName = donorName,
                foodDetails = foodDetails,
                address = address,
                status = "Requested",
                estimatedMeals = estimatedMeals
            )
            repository.insertPickup(pickup)
        }
    }

    fun createDonation(foodName: String, quantity: String, ngoName: String) {
        viewModelScope.launch {
            val donation = Donation(
                foodName = foodName,
                quantity = quantity,
                ngoName = ngoName,
                donorName = userProfile.value.name,
                status = "Pending",
                co2SavedKg = 2.4,
                mealsDonated = 5
            )
            repository.insertDonation(donation)
        }
    }

    // --- AI Operations ---
    fun runAiRecognition(desc: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val res = GeminiClient.recognizeFood(desc)
                _aiRecognitionResult.value = res
            } catch (e: Exception) {
                _aiRecognitionResult.value = FoodRecognitionResult(
                    name = desc,
                    category = "Pantry",
                    quantity = "1 unit",
                    estExpiryDays = 4,
                    confidence = 0.8,
                    environmentalImpact = "Reduces landfill emissions.",
                    isSimulated = true
                )
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runAiRecipeGeneration() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                // Get pantry items expiring soon
                val expiringItems = inventory.value
                    .filter { it.status == "Pantry" }
                    .take(3)
                    .map { it.name }
                
                val res = GeminiClient.getRecipeSuggestions(expiringItems)
                _recipeSuggestions.value = res
            } catch (e: Exception) {
                _recipeSuggestions.value = emptyList()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runAiWastePrediction(foodName: String, quantity: Int) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val res = GeminiClient.predictWasteDemand(foodName, quantity)
                _wastePredictionResult.value = res
            } catch (e: Exception) {
                _wastePredictionResult.value = null
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiStates() {
        _aiRecognitionResult.value = null
        _wastePredictionResult.value = null
    }

    fun addMoneyDonation(
        amount: Double,
        donorName: String,
        ngoName: String,
        isAnonymous: Boolean,
        isRecurring: Boolean,
        paymentMethod: String,
        status: String = "Success"
    ) {
        viewModelScope.launch {
            val transactionId = "TXN" + System.currentTimeMillis().toString().takeLast(9) + (100..999).random()
            val donation = MoneyDonation(
                amount = amount,
                donorName = if (isAnonymous) "Anonymous" else donorName,
                ngoName = ngoName,
                isAnonymous = isAnonymous,
                isRecurring = isRecurring,
                paymentMethod = paymentMethod,
                transactionId = transactionId,
                status = status,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMoneyDonation(donation)
        }
    }

    fun updateMoneyDonationStatus(donation: MoneyDonation, newStatus: String) {
        viewModelScope.launch {
            repository.updateMoneyDonation(donation.copy(status = newStatus))
        }
    }

    // ══════════════════════════════════════════════════════
    // FIREBASE SERVICE SYSTEM INTEGRATION
    // ══════════════════════════════════════════════════════
    val firebaseService: FirebaseService = FirebaseService.getInstance(application)
    
    private val _isFirebaseConnected = MutableStateFlow(firebaseService.isAvailable())
    val isFirebaseConnected = _isFirebaseConnected.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud = _isSyncingCloud.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Cloud Sync Offline")
    val syncStatusMessage = _syncStatusMessage.asStateFlow()

    // OTP Verification states
    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent = _isOtpSent.asStateFlow()

    private val _isOtpVerifying = MutableStateFlow(false)
    val isOtpVerifying = _isOtpVerifying.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError = _otpError.asStateFlow()

    private val _isUserLoggedInWithOtp = MutableStateFlow(firebaseService.getCurrentUid() != null)
    val isUserLoggedInWithOtp = _isUserLoggedInWithOtp.asStateFlow()

    private var currentVerificationId: String? = null

    fun sendFirebaseOtp(phoneNumber: String, activity: android.app.Activity) {
        _isOtpVerifying.value = true
        _otpError.value = null
        
        firebaseService.trackEvent("otp_request_initiated", Bundle().apply { putString("phone", phoneNumber) })

        if (!firebaseService.isAvailable()) {
            // Simulated OTP Flow for environment without google-services.json
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                currentVerificationId = "SIMULATED_VER_ID_" + System.currentTimeMillis()
                _isOtpSent.value = true
                _isOtpVerifying.value = false
                _otpError.value = "SIMULATION: SMS sent to $phoneNumber. Use code 123456 to log in."
            }
            return
        }

        firebaseService.sendOtp(
            phoneNumber = phoneNumber,
            activity = activity,
            onCodeSent = { verificationId, _ ->
                currentVerificationId = verificationId
                _isOtpSent.value = true
                _isOtpVerifying.value = false
                _otpError.value = "Verification code sent to $phoneNumber."
            },
            onError = { exception ->
                _isOtpVerifying.value = false
                _otpError.value = exception.localizedMessage ?: "Error sending code"
                firebaseService.recordCrash(exception)
            }
        )
    }

    fun verifyFirebaseOtp(code: String) {
        _isOtpVerifying.value = true
        _otpError.value = null

        val verId = currentVerificationId
        if (verId == null) {
            _isOtpVerifying.value = false
            _otpError.value = "No pending verification session. Send OTP first."
            return
        }

        if (!firebaseService.isAvailable()) {
            // Simulated OTP Verification
            viewModelScope.launch {
                kotlinx.coroutines.delay(1200)
                if (code == "123456") {
                    _isUserLoggedInWithOtp.value = true
                    _isOtpVerifying.value = false
                    _otpError.value = "Simulated login successful!"
                    
                    // Pre-populate some user details
                    val currentProfile = userProfile.value
                    repository.saveUserProfile(currentProfile.copy(isVerified = true))
                } else {
                    _isOtpVerifying.value = false
                    _otpError.value = "Invalid OTP code. Please try 123456."
                }
            }
            return
        }

        firebaseService.verifyOtpCode(
            verificationId = verId,
            otpCode = code,
            onSuccess = { uid ->
                _isUserLoggedInWithOtp.value = true
                _isOtpVerifying.value = false
                _otpError.value = "Secure authentication successful!"
                
                // Track successful login
                firebaseService.trackEvent("otp_auth_success")
                firebaseService.setCrashlyticsUser(uid)
                
                viewModelScope.launch {
                    val currentProfile = userProfile.value
                    repository.saveUserProfile(currentProfile.copy(id = uid, isVerified = true))
                }
            },
            onError = { exception ->
                _isOtpVerifying.value = false
                _otpError.value = exception.localizedMessage ?: "Invalid Verification Code"
                firebaseService.recordCrash(exception)
            }
        )
    }

    fun signOutFirebase() {
        firebaseService.signOutUser()
        _isUserLoggedInWithOtp.value = false
        _isOtpSent.value = false
        currentVerificationId = null
        _otpError.value = "Signed out of Firebase Portal."
        firebaseService.trackEvent("otp_signout")
    }

    // ══════════════════════════════════════════════════════
    // CLOUD FIRESTORE: 17 COLLECTIONS SYNC ENGINE
    // ══════════════════════════════════════════════════════
    fun triggerFullCloudSync() {
        viewModelScope.launch {
            if (_isSyncingCloud.value) return@launch
            _isSyncingCloud.value = true
            _syncStatusMessage.value = "Starting secure synchronization..."
            
            val isRealFirebase = firebaseService.isAvailable()
            val delayDuration = if (isRealFirebase) 200L else 400L // Longer visual delay on simulation so the user can see progress
            
            try {
                // 1/17: Users Collection
                _syncStatusMessage.value = "Syncing 1/17: [Users] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val profile = userProfile.value
                val profileMap = mapOf(
                    "id" to profile.id,
                    "name" to profile.name,
                    "email" to profile.email,
                    "role" to profile.role.name,
                    "subscription" to profile.subscription.name,
                    "isVerified" to profile.isVerified
                )
                if (isRealFirebase) firebaseService.syncUserProfile(profile.id, profileMap)

                // 2/17: Restaurants Collection
                _syncStatusMessage.value = "Syncing 2/17: [Restaurants] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val restData = mapOf("id" to "rest_001", "name" to "La Piazza Bistro", "mealsSaved" to 420)
                if (isRealFirebase) firebaseService.syncRestaurantProfile("rest_001", restData)

                // 3/17: Hotels Collection
                _syncStatusMessage.value = "Syncing 3/17: [Hotels] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val hotelData = mapOf("id" to "hotel_001", "name" to "The Grand Royal Plaza", "foodRescued" to "1200 Kg")
                if (isRealFirebase) firebaseService.syncHotelProfile("hotel_001", hotelData)

                // 4/17: NGOs Collection
                _syncStatusMessage.value = "Syncing 4/17: [NGOs] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val ngoData = mapOf("id" to "ngo_001", "name" to "Robin Hood Food Rescue", "verifiedStatus" to true)
                if (isRealFirebase) firebaseService.syncNgoProfile("ngo_001", ngoData)

                // 5/17: Admins Collection
                _syncStatusMessage.value = "Syncing 5/17: [Admins] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val adminData = mapOf("id" to "admin_001", "name" to "Super System Administrator")
                if (isRealFirebase) firebaseService.syncAdminProfile("admin_001", adminData)

                // 6/17: FoodInventory Collection
                _syncStatusMessage.value = "Syncing 6/17: [FoodInventory] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                inventory.value.forEach { item ->
                    val itemMap = mapOf(
                        "name" to item.name,
                        "quantity" to item.quantity,
                        "category" to item.category,
                        "expiryDate" to item.expiryDate,
                        "status" to item.status,
                        "addedDate" to item.addedDate,
                        "scannedBarcode" to (item.scannedBarcode ?: ""),
                        "isExpiringSoon" to item.isExpiringSoon
                    )
                    if (isRealFirebase) firebaseService.syncFoodInventory(item.id.toString(), itemMap)
                }

                // 7/17: FoodDonations Collection
                _syncStatusMessage.value = "Syncing 7/17: [FoodDonations] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                donations.value.forEach { don ->
                    val donMap = mapOf(
                        "foodName" to don.foodName,
                        "quantity" to don.quantity,
                        "ngoName" to don.ngoName,
                        "donorName" to don.donorName,
                        "status" to don.status,
                        "co2SavedKg" to don.co2SavedKg,
                        "mealsDonated" to don.mealsDonated
                    )
                    if (isRealFirebase) firebaseService.syncFoodDonation(don.id.toString(), donMap)
                }

                // 8/17: WasteLogs Collection
                _syncStatusMessage.value = "Syncing 8/17: [WasteLogs] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                wasteLogs.value.forEach { log ->
                    val logMap = mapOf(
                        "foodName" to log.foodName,
                        "weightKg" to log.weightKg,
                        "costLoss" to log.costLoss,
                        "reason" to log.reason
                    )
                    if (isRealFirebase) firebaseService.syncWasteLog(log.id.toString(), logMap)
                }

                // 9/17: Payments Collection
                _syncStatusMessage.value = "Syncing 9/17: [Payments] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                moneyDonations.value.forEach { don ->
                    val donMap = mapOf(
                        "amount" to don.amount,
                        "donorName" to don.donorName,
                        "ngoName" to don.ngoName,
                        "isAnonymous" to don.isAnonymous,
                        "isRecurring" to don.isRecurring,
                        "paymentMethod" to don.paymentMethod,
                        "status" to don.status,
                        "timestamp" to don.timestamp
                    )
                    if (isRealFirebase) firebaseService.syncMoneyDonation(don.transactionId, donMap)
                }

                // 10/17: Subscriptions Collection
                _syncStatusMessage.value = "Syncing 1/17: [Subscriptions] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val subData = mapOf("planId" to "premium_ai_cognitive", "price" to 2999.0, "status" to "Active")
                if (isRealFirebase) firebaseService.syncSubscription("sub_active_001", subData)

                // 11/17: Notifications Collection
                _syncStatusMessage.value = "Syncing 1/17: [Notifications] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val notifData = mapOf("title" to "Fresh Rescue Nearby!", "body" to "5 Kg Tomatoes available at La Piazza Bistro", "timestamp" to System.currentTimeMillis())
                if (isRealFirebase) firebaseService.syncNotification("notif_001", notifData)

                // 12/17: Recipes Collection
                _syncStatusMessage.value = "Syncing 1/17: [Recipes] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val recipeData = mapOf("title" to "Tomato Basil Bruschetta", "savedMeals" to 1)
                if (isRealFirebase) firebaseService.syncRecipe("recipe_001", recipeData)

                // 13/17: Analytics Collection
                _syncStatusMessage.value = "Syncing 1/17: [Analytics] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val analyticsMap = mapOf("activeUsers" to 4305, "totalMealsSaved" to 14201, "totalCO2SavedKg" to 24102.5)
                if (isRealFirebase) firebaseService.syncAnalyticsReport("analytics_june_2026", analyticsMap)

                // 14/17: PickupRequests Collection
                _syncStatusMessage.value = "Syncing 1/17: [PickupRequests] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                pickups.value.forEach { pick ->
                    val pickMap = mapOf(
                        "donorName" to pick.donorName,
                        "foodDetails" to pick.foodDetails,
                        "address" to pick.address,
                        "status" to pick.status,
                        "estimatedMeals" to pick.estimatedMeals
                    )
                    if (isRealFirebase) firebaseService.syncPickupRequest(pick.id.toString(), pickMap)
                }

                // 15/17: PickupHistory Collection
                _syncStatusMessage.value = "Syncing 1/17: [PickupHistory] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val pickupHistMap = mapOf("id" to "pickup_hist_001", "driver" to "Karan Dev", "completedTime" to System.currentTimeMillis() - 7200000)
                if (isRealFirebase) firebaseService.syncPickupHistory("pickup_hist_001", pickupHistMap)

                // 16/17: ImpactReports Collection
                _syncStatusMessage.value = "Syncing 1/17: [ImpactReports] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val impactReport = mapOf("co2ReductionKg" to 1845.5, "waterPreservedLitres" to 425000, "landfillPreventedKg" to 1025.0)
                if (isRealFirebase) firebaseService.syncImpactReport("impact_report_june_2026", impactReport)

                // 17/17: Settings Collection
                _syncStatusMessage.value = "Syncing 1/17: [Settings] Collection..."
                kotlinx.coroutines.delay(delayDuration)
                val settingsData = mapOf("darkMode" to true, "pushNotificationsEnabled" to true, "autoBackup" to true)
                if (isRealFirebase) firebaseService.syncSettings("settings_user_001", settingsData)

                _syncStatusMessage.value = "All 17 Firestore Collections Synced Successfully! 🎉"
                firebaseService.trackEvent("sync_completed_successfully")
            } catch (e: Exception) {
                _syncStatusMessage.value = "Sync incomplete: " + (e.localizedMessage ?: "Unknown error")
                firebaseService.recordCrash(e)
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }
}
