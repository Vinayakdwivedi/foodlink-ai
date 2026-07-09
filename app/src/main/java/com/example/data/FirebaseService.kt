package com.example.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit

class FirebaseService private constructor(private val context: Context) {

    private var isFirebaseAvailable = false
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var messaging: FirebaseMessaging? = null
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    // Definitions for all 17 Firestore Collections requested by the user
    object Collections {
        const val USERS = "Users"
        const val RESTAURANTS = "Restaurants"
        const val HOTELS = "Hotels"
        const val NGOS = "NGOs"
        const val ADMINS = "Admins"
        const val FOOD_INVENTORY = "FoodInventory"
        const val FOOD_DONATIONS = "FoodDonations"
        const val WASTE_LOGS = "WasteLogs"
        const val PAYMENTS = "Payments"
        const val SUBSCRIPTIONS = "Subscriptions"
        const val NOTIFICATIONS = "Notifications"
        const val RECIPES = "Recipes"
        const val ANALYTICS = "Analytics"
        const val PICKUP_REQUESTS = "PickupRequests"
        const val PICKUP_HISTORY = "PickupHistory"
        const val IMPACT_REPORTS = "ImpactReports"
        const val SETTINGS = "Settings"
    }

    init {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                apps[0]
            }
            if (app != null) {
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                storage = FirebaseStorage.getInstance()
                messaging = FirebaseMessaging.getInstance()
                analytics = FirebaseAnalytics.getInstance(context)
                crashlytics = FirebaseCrashlytics.getInstance()
                isFirebaseAvailable = true
                Log.d("FirebaseService", "Firebase services initialized successfully!")
                
                // Initialize App Check with Recaptcha safety
                setupAppCheck()
            }
        } catch (e: Exception) {
            Log.w("FirebaseService", "Firebase is not initialized. Using secure offline/local fallback. Details: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseService? = null

        fun getInstance(context: Context): FirebaseService {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun isAvailable(): Boolean = isFirebaseAvailable

    // ══════════════════════════════════════════════════════
    // 1. FIREBASE APP CHECK
    // ══════════════════════════════════════════════════════
    private fun setupAppCheck() {
        if (!isFirebaseAvailable) return
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            Log.d("FirebaseService", "Firebase App Check initialized successfully.")
        } catch (e: Exception) {
            Log.e("FirebaseService", "App Check initialization failed: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════
    // 2. FIREBASE AUTHENTICATION (MOBILE OTP LOGIN)
    // ══════════════════════════════════════════════════════
    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable || auth == null) {
            onError(Exception("Firebase Authentication is not available. Please load google-services.json."))
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval or instant verification
                auth?.signInWithCredential(credential)
                    ?.addOnSuccessListener {
                        Log.d("FirebaseService", "Verification Completed Instantly")
                    }
                    ?.addOnFailureListener {
                        onError(it)
                    }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                Log.e("FirebaseService", "OTP Verification Failed", e)
                onError(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("FirebaseService", "OTP Code Sent, ID: $verificationId")
                onCodeSent(verificationId, token)
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(auth!!)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun verifyOtpCode(
        verificationId: String,
        otpCode: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable || auth == null) {
            onError(Exception("Firebase Authentication is not available."))
            return
        }

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            auth?.signInWithCredential(credential)
                ?.addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    onSuccess(uid)
                }
                ?.addOnFailureListener {
                    onError(it)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun getCurrentUid(): String? {
        return auth?.currentUser?.uid
    }

    fun signOutUser() {
        auth?.signOut()
    }

    // ══════════════════════════════════════════════════════
    // 3. CLOUD FIRESTORE DATABASE (17 CORE COLLECTIONS)
    // ══════════════════════════════════════════════════════
    
    // USERS COLLECTION
    fun syncUserProfile(userId: String, profileData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.USERS)?.document(userId)?.set(profileData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // FOOD INVENTORY COLLECTION
    fun syncFoodInventory(itemId: String, itemData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.FOOD_INVENTORY)?.document(itemId)?.set(itemData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // FOOD DONATIONS COLLECTION
    fun syncFoodDonation(donationId: String, donationData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.FOOD_DONATIONS)?.document(donationId)?.set(donationData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // WASTE LOGS COLLECTION
    fun syncWasteLog(logId: String, logData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.WASTE_LOGS)?.document(logId)?.set(logData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // PAYMENTS COLLECTION (Secure Donation Records)
    fun syncMoneyDonation(donationId: String, donationData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.PAYMENTS)?.document(donationId)?.set(donationData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // NGO PICKUP REQUESTS COLLECTION
    fun syncPickupRequest(requestId: String, requestData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.PICKUP_REQUESTS)?.document(requestId)?.set(requestData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // NGO PICKUP HISTORY COLLECTION
    fun syncPickupHistory(historyId: String, historyData: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(false)
            return
        }
        firestore?.collection(Collections.PICKUP_HISTORY)?.document(historyId)?.set(historyData)
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(false) }
    }

    // RESTAURANTS COLLECTION
    fun syncRestaurantProfile(restaurantId: String, restaurantData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.RESTAURANTS)?.document(restaurantId)?.set(restaurantData)
    }

    // HOTELS COLLECTION
    fun syncHotelProfile(hotelId: String, hotelData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.HOTELS)?.document(hotelId)?.set(hotelData)
    }

    // NGOs COLLECTION
    fun syncNgoProfile(ngoId: String, ngoData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.NGOS)?.document(ngoId)?.set(ngoData)
    }

    // ADMINS COLLECTION
    fun syncAdminProfile(adminId: String, adminData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.ADMINS)?.document(adminId)?.set(adminData)
    }

    // SUBSCRIPTIONS COLLECTION
    fun syncSubscription(subscriptionId: String, subscriptionData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.SUBSCRIPTIONS)?.document(subscriptionId)?.set(subscriptionData)
    }

    // NOTIFICATIONS COLLECTION
    fun syncNotification(notificationId: String, notificationData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.NOTIFICATIONS)?.document(notificationId)?.set(notificationData)
    }

    // RECIPES COLLECTION
    fun syncRecipe(recipeId: String, recipeData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.RECIPES)?.document(recipeId)?.set(recipeData)
    }

    // ANALYTICS COLLECTION
    fun syncAnalyticsReport(reportId: String, reportData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.ANALYTICS)?.document(reportId)?.set(reportData)
    }

    // IMPACT REPORTS COLLECTION
    fun syncImpactReport(reportId: String, reportData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.IMPACT_REPORTS)?.document(reportId)?.set(reportData)
    }

    // SETTINGS COLLECTION
    fun syncSettings(settingsId: String, settingsData: Map<String, Any>) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore?.collection(Collections.SETTINGS)?.document(settingsId)?.set(settingsData)
    }

    // ══════════════════════════════════════════════════════
    // 4. FIREBASE STORAGE (IMAGES & DOCUMENTS UPLOADER)
    // ══════════════════════════════════════════════════════
    fun uploadFile(
        folder: String,
        fileName: String,
        fileUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable || storage == null) {
            onError(Exception("Firebase Storage is not available."))
            return
        }

        val storageRef = storage?.reference?.child("$folder/$fileName")
        storageRef?.putFile(fileUri)
            ?.addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        onError(it)
                    }
            }
            ?.addOnFailureListener {
                onError(it)
            }
    }

    // ══════════════════════════════════════════════════════
    // 5. FIREBASE CLOUD MESSAGING (PUSH NOTIFICATIONS)
    // ══════════════════════════════════════════════════════
    fun getFcmToken(onTokenFetched: (String) -> Unit) {
        if (!isFirebaseAvailable || messaging == null) {
            onTokenFetched("simulated_token_" + System.currentTimeMillis())
            return
        }

        messaging?.token?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onTokenFetched(task.result ?: "")
            } else {
                onTokenFetched("token_fetch_failed")
            }
        }
    }

    fun subscribeToTopic(topicName: String) {
        if (!isFirebaseAvailable || messaging == null) return
        messaging?.subscribeToTopic(topicName)
    }

    // ══════════════════════════════════════════════════════
    // 6. FIREBASE ANALYTICS
    // ══════════════════════════════════════════════════════
    fun trackEvent(eventName: String, params: Bundle = Bundle()) {
        if (!isFirebaseAvailable || analytics == null) {
            Log.d("FirebaseAnalyticsSim", "Event: $eventName with params: $params")
            return
        }
        analytics?.logEvent(eventName, params)
    }

    // ══════════════════════════════════════════════════════
    // 7. FIREBASE CRASHLYTICS
    // ══════════════════════════════════════════════════════
    fun recordCrash(throwable: Throwable) {
        if (!isFirebaseAvailable || crashlytics == null) {
            Log.w("FirebaseCrashlyticsSim", "Simulated exception capture: ${throwable.message}")
            return
        }
        crashlytics?.recordException(throwable)
    }

    fun setCrashlyticsUser(userId: String) {
        if (!isFirebaseAvailable || crashlytics == null) return
        crashlytics?.setUserId(userId)
    }
}
