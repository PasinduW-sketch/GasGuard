package com.example.gasml.data

import android.util.Log
import com.example.gasml.model.GasStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class GasStatsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val statsCollection = firestore.collection("gas_stats")

    fun getUnitStats(unitId: String): Flow<GasStats?> = callbackFlow {
        if (unitId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val subscription = statsCollection.document(unitId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GasStatsRepository", "Error listening to unit stats for $unitId", error)
                    close()
                    return@addSnapshotListener
                }
                
                val stats = try {
                    if (snapshot != null && snapshot.exists()) {
                        snapshot.toObject(GasStats::class.java)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("GasStatsRepository", "Failed to parse GasStats for $unitId", e)
                    null
                }
                trySend(stats)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateUnitStats(unitId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            statsCollection.document(unitId).set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GasStatsRepository", "Failed to update unit stats for $unitId", e)
            Result.failure(e)
        }
    }

    suspend fun simulateLeak(unitId: String, isLeak: Boolean): Result<Unit> {
        return try {
            val doc = statsCollection.document(unitId).get().await()
            val updates = mutableMapOf<String, Any>(
                "leakDetected" to isLeak,
                "leakPercentage" to if (isLeak) 85.0 else 0.0,
                "systemStatus" to if (isLeak) "CRITICAL" else "NORMAL",
                "valveClosed" to isLeak
            )
            
            // If the document doesn't exist (new unit), populate it with sensible mock data 
            // so the simulation doesn't show 0.0kg and empty UI.
            if (!doc.exists()) {
                updates.putAll(mapOf(
                    "unitId" to unitId,
                    "currentWeight" to 5.0,
                    "gasLevel" to 100L,
                    "gasPercentage" to 100.0,
                    "temperature" to 27.5,
                    "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
                    "dailyUsage" to 0.12,
                    "daysRemaining" to 41L,
                    "capacity" to "5kg"
                ))
            }
            
            updateUnitStats(unitId, updates)
        } catch (e: Exception) {
            Log.e("GasStatsRepository", "Simulation failed", e)
            Result.failure(e)
        }
    }

    suspend fun acknowledgeLeak(unitId: String): Result<Unit> {
        return updateUnitStats(unitId, mapOf(
            "leakDetected" to false,
            "leakPercentage" to 0.0,
            "systemStatus" to "NORMAL",
            "valveClosed" to false
        ))
    }
}
