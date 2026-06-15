package com.example.gasml.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Order(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val cylinderType: String = "",
    val quantity: Long = 1L,
    val totalPrice: Double = 0.0,
    val status: String = "Pending",
    val timestamp: Timestamp? = null,
    val address: String = "",
    val paymentMethod: String = "Cash on delivery",
    
    // Safety fields as seen in Firestore
    val safeQuantity: Long = 1L,
    val safeTimestamp: Timestamp? = null,
    val safeTotalPrice: Double = 0.0,

    // Map location fields
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapLocationUrl: String = ""
) {
    fun getEffectiveTimestamp(): Timestamp {
        return timestamp ?: safeTimestamp ?: Timestamp.now()
    }

    fun getEffectiveTotalPrice(): Double {
        return if (totalPrice > 0) totalPrice else safeTotalPrice
    }

    fun getEffectiveQuantity(): Int {
        val qty = if (quantity > 0) quantity else safeQuantity
        return qty.toInt()
    }
}
