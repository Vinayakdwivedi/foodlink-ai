package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class FoodLinkRepository(private val dao: FoodLinkDao) {

    val allInventory: Flow<List<FoodItem>> = dao.getAllInventory()
    val allDonations: Flow<List<Donation>> = dao.getAllDonations()
    val allWasteLogs: Flow<List<WasteLog>> = dao.getAllWasteLogs()
    val allPickups: Flow<List<NgoPickup>> = dao.getAllPickups()
    val allMoneyDonations: Flow<List<MoneyDonation>> = dao.getAllMoneyDonations()

    fun getInventoryForUser(userId: String): Flow<List<FoodItem>> = dao.getInventory(userId)
    fun getUserProfile(userId: String): Flow<UserProfile?> = dao.getUserProfile(userId)

    suspend fun saveUserProfile(profile: UserProfile) = dao.insertUserProfile(profile)

    suspend fun insertFoodItem(item: FoodItem) = dao.insertFoodItem(item)
    suspend fun updateFoodItem(item: FoodItem) = dao.updateFoodItem(item)
    suspend fun deleteFoodItem(item: FoodItem) = dao.deleteFoodItem(item)
    suspend fun deleteFoodItemById(itemId: Int) = dao.deleteFoodItemById(itemId)

    suspend fun insertDonation(donation: Donation) = dao.insertDonation(donation)
    suspend fun updateDonation(donation: Donation) = dao.updateDonation(donation)

    suspend fun insertWasteLog(log: WasteLog) = dao.insertWasteLog(log)

    suspend fun insertPickup(pickup: NgoPickup) = dao.insertPickup(pickup)
    suspend fun updatePickup(pickup: NgoPickup) = dao.updatePickup(pickup)

    suspend fun insertMoneyDonation(donation: MoneyDonation) = dao.insertMoneyDonation(donation)
    suspend fun updateMoneyDonation(donation: MoneyDonation) = dao.updateMoneyDonation(donation)

    // Pre-populates the database with beautiful, realistic sustainability data
    suspend fun initializeDemoData() {
        val defaultProfile = dao.getUserProfile("default_user").firstOrNull()
        if (defaultProfile == null) {
            dao.insertUserProfile(
                UserProfile(
                    id = "default_user",
                    name = "John Green",
                    email = "eco.hero@foodlink.ai",
                    role = UserRole.HOME_USER,
                    subscription = SubscriptionType.FREE,
                    isVerified = true
                )
            )

            // Dynamic date offsets for food expiry dates
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()

            cal.add(Calendar.DAY_OF_YEAR, 2)
            val expires2Days = sdf.format(cal.time)

            cal.setTime(Date())
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val expires1Day = sdf.format(cal.time)

            cal.setTime(Date())
            cal.add(Calendar.DAY_OF_YEAR, 5)
            val expires5Days = sdf.format(cal.time)

            cal.setTime(Date())
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val expiredYesterday = sdf.format(cal.time)

            // 1. Food items
            val items = listOf(
                FoodItem(name = "Organic Tomatoes", quantity = "1.5 kg", category = "Veggies", expiryDate = expires2Days, status = "Pantry", addedDate = "2026-06-25"),
                FoodItem(name = "Greek Yogurt", quantity = "500g", category = "Dairy", expiryDate = expires1Day, status = "Pantry", addedDate = "2026-06-25"),
                FoodItem(name = "Whole Wheat Bread", quantity = "1 Loaf", category = "Bakery", expiryDate = expires5Days, status = "Pantry", addedDate = "2026-06-26"),
                FoodItem(name = "Fresh Spinach Leaves", quantity = "250g", category = "Veggies", expiryDate = expiredYesterday, status = "Pantry", addedDate = "2026-06-24", isExpiringSoon = true)
            )
            for (item in items) {
                dao.insertFoodItem(item)
            }

            // 2. Donations
            val donations = listOf(
                Donation(foodName = "Surplus Gourmet Salads", quantity = "12 Bowls", ngoName = "Green Leaf Kitchen", donorName = "La Piazza Restaurant", status = "Completed", co2SavedKg = 18.4, mealsDonated = 12),
                Donation(foodName = "Whole Wheat Artisan Bread", quantity = "8 Loaves", ngoName = "Hope Feed Center", donorName = "The Hearth Bakery", status = "Completed", co2SavedKg = 10.2, mealsDonated = 16),
                Donation(foodName = "Fresh Apples & Pears", quantity = "15 Kg", ngoName = "Green Leaf Kitchen", donorName = "John Green", status = "Pending", co2SavedKg = 15.0, mealsDonated = 30)
            )
            for (donation in donations) {
                dao.insertDonation(donation)
            }

            // 3. Waste logs
            val waste = listOf(
                WasteLog(foodName = "Rotten Avocados", weightKg = 1.2, costLoss = 9.50, reason = "Rotten / Overripe"),
                WasteLog(foodName = "Expired Milk", weightKg = 2.0, costLoss = 4.20, reason = "Sour / Past Date"),
                WasteLog(foodName = "Leftover Buffet Rice", weightKg = 4.5, costLoss = 15.00, reason = "Over-prepared")
            )
            for (w in waste) {
                dao.insertWasteLog(w)
            }

            // 4. NGO pickups
            val pickups = listOf(
                NgoPickup(donorName = "Bella Italia Restaurant", foodDetails = "Surplus Lasagna (15 Portions)", address = "123 Pasta Avenue, Food District", status = "Requested", estimatedMeals = 15),
                NgoPickup(donorName = "Organic Supermarket", foodDetails = "Assorted Vegetable Boxes (40 Kg)", address = "789 Harvest Road, Eco Suburb", status = "Accepted", estimatedMeals = 80),
                NgoPickup(donorName = "Grand Banquet Hall", foodDetails = "Assorted Desserts & Rice (100 Servings)", address = "55 Luxury Boulevard", status = "Completed", estimatedMeals = 100)
            )
            for (p in pickups) {
                dao.insertPickup(p)
            }

            // 5. Money donations
            val moneyDonations = listOf(
                MoneyDonation(amount = 501.0, donorName = "Sarah Jenkins", ngoName = "Green Leaf Kitchen", isAnonymous = false, isRecurring = false, paymentMethod = "Google Pay (UPI)", transactionId = "TXN987654321", status = "Success", timestamp = System.currentTimeMillis() - 86400000 * 3),
                MoneyDonation(amount = 101.0, donorName = "Anonymous", ngoName = "Hope Feed Center", isAnonymous = true, isRecurring = true, paymentMethod = "Paytm", transactionId = "TXN456123789", status = "Success", timestamp = System.currentTimeMillis() - 86400000 * 2),
                MoneyDonation(amount = 1001.0, donorName = "La Piazza Restaurant", ngoName = "Green Leaf Kitchen", isAnonymous = false, isRecurring = false, paymentMethod = "Razorpay Gateway", transactionId = "TXN1122334455", status = "Success", timestamp = System.currentTimeMillis() - 86400000),
                MoneyDonation(amount = 19.0, donorName = "John Green", ngoName = "Robin Hood Food Rescue", isAnonymous = false, isRecurring = false, paymentMethod = "PhonePe", transactionId = "TXN5566778899", status = "Success", timestamp = System.currentTimeMillis() - 3600000 * 4)
            )
            for (md in moneyDonations) {
                dao.insertMoneyDonation(md)
            }
        }
    }
}
