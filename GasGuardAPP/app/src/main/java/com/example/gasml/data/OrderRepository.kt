package com.example.gasml.data

import android.util.Log
import com.example.gasml.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")

    suspend fun placeOrder(order: Order): Result<Unit> {
        return try {
            val docRef = ordersCollection.document()
            val now = Timestamp.now()
            
            // Ensuring both standard and "safe" fields are populated as per Firestore format
            val finalOrder = order.copy(
                id = docRef.id,
                timestamp = now,
                safeTimestamp = now,
                safeQuantity = order.quantity,
                safeTotalPrice = order.totalPrice
            )

            docRef.set(finalOrder).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Place order failed", e)
            Result.failure(e)
        }
    }

    fun getCustomerOrders(userId: String): Flow<List<Order>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderRepository", "Orders listener failed", error)
                    close()
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("OrderRepository", "Failed to parse order ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(orders)
            }
        awaitClose { subscription.remove() }
    }

    fun getAllOrdersForDealer(): Flow<List<Order>> = callbackFlow {
        val subscription = ordersCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderRepository", "Dealer listener failed", error)
                    close()
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("OrderRepository", "Failed to parse dealer order ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(orders)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Update status failed", e)
            Result.failure(e)
        }
    }
}
