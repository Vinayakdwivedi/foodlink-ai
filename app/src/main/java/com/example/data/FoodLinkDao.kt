package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLinkDao {

    // --- User Profile ---
    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // --- Food Items (Inventory) ---
    @Query("SELECT * FROM food_items WHERE userId = :userId ORDER BY expiryDate ASC")
    fun getInventory(userId: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllInventory(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :itemId LIMIT 1")
    suspend fun getFoodItemById(itemId: Int): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem)

    @Update
    suspend fun updateFoodItem(item: FoodItem)

    @Delete
    suspend fun deleteFoodItem(item: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :itemId")
    suspend fun deleteFoodItemById(itemId: Int)

    // --- Donations ---
    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAllDonations(): Flow<List<Donation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: Donation)

    @Update
    suspend fun updateDonation(donation: Donation)

    // --- Waste Logs ---
    @Query("SELECT * FROM waste_logs ORDER BY timestamp DESC")
    fun getAllWasteLogs(): Flow<List<WasteLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWasteLog(log: WasteLog)

    // --- NGO Pickups ---
    @Query("SELECT * FROM ngo_pickups ORDER BY timestamp DESC")
    fun getAllPickups(): Flow<List<NgoPickup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPickup(pickup: NgoPickup)

    @Update
    suspend fun updatePickup(pickup: NgoPickup)

    // --- Money Donations ---
    @Query("SELECT * FROM money_donations ORDER BY timestamp DESC")
    fun getAllMoneyDonations(): Flow<List<MoneyDonation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyDonation(donation: MoneyDonation)

    @Update
    suspend fun updateMoneyDonation(donation: MoneyDonation)
}
