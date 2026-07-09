package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    HOME_USER, RESTAURANT, NGO, ADMIN
}

enum class SubscriptionType {
    FREE, HOME_BASIC, RESTAURANT_PREMIUM, PREMIUM_COGNITIVE
}

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "default_user",
    val name: String = "John Green",
    val email: String = "eco.hero@foodlink.ai",
    val role: UserRole = UserRole.HOME_USER,
    val subscription: SubscriptionType = SubscriptionType.FREE,
    val isVerified: Boolean = true
)

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: String,
    val category: String,
    val expiryDate: String, // YYYY-MM-DD
    val status: String = "Pantry", // Pantry, Donated, Wasted
    val addedDate: String,
    val userId: String = "default_user",
    val scannedBarcode: String? = null,
    val co2SavedKg: Double = 0.0,
    val isExpiringSoon: Boolean = false
)

@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val quantity: String,
    val ngoName: String,
    val donorName: String = "John Green",
    val status: String = "Pending", // Pending, Accepted, Picked Up, Completed
    val timestamp: Long = System.currentTimeMillis(),
    val co2SavedKg: Double = 2.5,
    val mealsDonated: Int = 4
)

@Entity(tableName = "waste_logs")
data class WasteLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val weightKg: Double,
    val costLoss: Double,
    val reason: String = "Expired",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ngo_pickups")
data class NgoPickup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val donorName: String,
    val foodDetails: String,
    val address: String,
    val status: String = "Requested", // Requested, Accepted, Rejected, Completed
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedMeals: Int = 10
)

@Entity(tableName = "money_donations")
data class MoneyDonation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val donorName: String,
    val ngoName: String,
    val isAnonymous: Boolean = false,
    val isRecurring: Boolean = false, // false = One-time, true = Monthly
    val paymentMethod: String,
    val transactionId: String,
    val status: String = "Success", // Success, Pending, Failed
    val timestamp: Long = System.currentTimeMillis()
)

