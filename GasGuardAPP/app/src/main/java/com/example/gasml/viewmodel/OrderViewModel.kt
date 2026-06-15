package com.example.gasml.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gasml.model.Order
import com.example.gasml.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val notificationHelper = NotificationHelper(application)
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    val activeOrdersCount = _orders.map { list ->
        list.count { it.status != "Delivered" && it.status != "Cancelled" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalRevenue: StateFlow<Double> = _orders.map { list ->
        list.filter { it.status == "Delivered" }
            .fold(0.0) { acc, order -> acc + order.getEffectiveTotalPrice() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private var ordersListener: ListenerRegistration? = null

    var isPlacingOrder by mutableStateOf(false)
        private set

    fun placeOrder(order: Order, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isPlacingOrder) return
        viewModelScope.launch {
            isPlacingOrder = true
            try {
                val docRef = db.collection("orders").document()
                val finalOrder = order.copy(id = docRef.id)
                docRef.set(finalOrder).await()
                
                notificationHelper.showOrderNotification(
                    "Order Placed Successfully",
                    "Your request for ${order.cylinderType} refill has been received."
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Place order failed", e)
                onError(e.message ?: "Failed to place order")
            } finally {
                isPlacingOrder = false
            }
        }
    }

    fun loadCustomerOrders(userId: String) {
        if (userId.isBlank()) return
        ordersListener?.remove()
        ordersListener = db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _orders.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    fun loadDealerOrders() {
        ordersListener?.remove()
        ordersListener = db.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _orders.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    fun updateStatus(orderId: String, newStatus: String) {
        if (orderId.isEmpty()) return
        viewModelScope.launch {
            try {
                db.collection("orders").document(orderId).update("status", newStatus).await()
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Status update failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
    }
}
