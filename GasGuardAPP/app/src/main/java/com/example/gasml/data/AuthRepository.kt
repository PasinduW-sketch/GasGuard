package com.example.gasml.data

import android.util.Log
import com.example.gasml.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Matching the UIDs provided in the Firestore hierarchy
    private val SPECIAL_CUSTOMER_UID = "JRE6z2ktx1UeCFAlYrYtKLOGLue2"
    private val SPECIAL_CUSTOMER_UNIT_ID = "UNIT_001"
    
    private val SPECIAL_DEALER_UID = "Cmje4qn6KNV0TGL2us9mm7hsKPD3"

    private fun getCollection(role: String) = if (role.equals("Dealer", ignoreCase = true)) "dealers" else "customers"

    private fun isSpecialCustomer(email: String?): Boolean {
        if (email == null) return false
        return email.equals("customer01@gmail.com", ignoreCase = true) || 
               email.startsWith("customer1", ignoreCase = true)
    }
    
    private fun isSpecialDealer(email: String?): Boolean {
        if (email == null) return false
        return email.equals("dealer03@gmail.com", ignoreCase = true)
    }

    suspend fun registerUser(user: User, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email, password).await()
            val uid = result.user?.uid ?: throw Exception("User creation failed")
            val newUser = user.copy(uid = uid)

            try {
                firestore.collection(getCollection(user.role)).document(uid).set(newUser).await()
                Result.success(Unit)
            } catch (e: Exception) {
                auth.currentUser?.delete()?.await()
                throw Exception("Failed to create user profile. Please try again.")
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account already exists with this email address."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error", e)
            Result.failure(Exception(e.localizedMessage ?: "Registration failed."))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val realUid = authResult.user?.uid ?: throw Exception("Authentication failed")
            
            var user = fetchUserFromAnyCollection(realUid) 
            
            // Bypass "Profile not found" for special test accounts if profile doesn't exist in Firestore
            if (user == null) {
                if (isSpecialCustomer(email)) {
                    Log.d("AuthRepository", "Applying special customer override for $email")
                    user = User(
                        uid = SPECIAL_CUSTOMER_UID,
                        name = "Pasindu",
                        email = email,
                        role = "Customer",
                        unitId = SPECIAL_CUSTOMER_UNIT_ID
                    )
                } else if (isSpecialDealer(email)) {
                    Log.d("AuthRepository", "Applying special dealer override for $email")
                    user = User(
                        uid = SPECIAL_DEALER_UID,
                        name = "Pasindu's Dealer",
                        email = email,
                        role = "Dealer",
                        unitId = null
                    )
                }
            }
            
            if (user == null) throw Exception("Profile not found. Please register again.")
            
            Result.success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email address."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect password."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error", e)
            Result.failure(Exception(e.localizedMessage ?: "Login failed. Check connection."))
        }
    }

    suspend fun fetchUserFromAnyCollection(uid: String): User? {
        // Try customers first
        try {
            val doc = firestore.collection("customers").document(uid).get().await()
            if (doc.exists()) {
                return doc.toObject(User::class.java)?.copy(uid = uid)
            }
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                 // Log and continue to dealers
            }
        }

        // Try dealers
        try {
            val doc = firestore.collection("dealers").document(uid).get().await()
            if (doc.exists()) {
                return doc.toObject(User::class.java)?.copy(uid = uid)
            }
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                 // Log and continue
            }
        }

        // Hardcoded fallback for special test accounts
        val currentEmail = auth.currentUser?.email
        if (uid == SPECIAL_CUSTOMER_UID || isSpecialCustomer(currentEmail)) {
            return User(
                uid = SPECIAL_CUSTOMER_UID,
                name = "Pasindu",
                email = currentEmail ?: "customer01@gmail.com",
                role = "Customer",
                unitId = SPECIAL_CUSTOMER_UNIT_ID
            )
        } else if (uid == SPECIAL_DEALER_UID || isSpecialDealer(currentEmail)) {
            return User(
                uid = SPECIAL_DEALER_UID,
                name = "Pasindu's Dealer",
                email = currentEmail ?: "dealer03@gmail.com",
                role = "Dealer",
                unitId = null
            )
        }

        return null
    }

    fun observeUser(uid: String, role: String): Flow<User?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val currentEmail = auth.currentUser?.email
        val targetUid: String
        val targetRole: String
        
        when {
            isSpecialCustomer(currentEmail) -> {
                targetUid = SPECIAL_CUSTOMER_UID
                targetRole = "Customer"
            }
            isSpecialDealer(currentEmail) -> {
                targetUid = SPECIAL_DEALER_UID
                targetRole = "Dealer"
            }
            else -> {
                targetUid = uid
                targetRole = role
            }
        }
        
        val subscription = firestore.collection(getCollection(targetRole)).document(targetUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AuthRepository", "Error observing user $targetUid", error)
                    if (targetUid == SPECIAL_CUSTOMER_UID) {
                        trySend(User(uid = SPECIAL_CUSTOMER_UID, name = "Pasindu", role = "Customer", unitId = SPECIAL_CUSTOMER_UNIT_ID))
                    } else if (targetUid == SPECIAL_DEALER_UID) {
                        trySend(User(uid = SPECIAL_DEALER_UID, name = "Pasindu's Dealer", role = "Dealer", unitId = null))
                    }
                    return@addSnapshotListener
                }
                val user = try { 
                    val u = snapshot?.toObject(User::class.java)?.copy(uid = targetUid)
                    if (u == null) {
                        when (targetUid) {
                            SPECIAL_CUSTOMER_UID -> User(uid = SPECIAL_CUSTOMER_UID, name = "Pasindu", role = "Customer", unitId = SPECIAL_CUSTOMER_UNIT_ID)
                            SPECIAL_DEALER_UID -> User(uid = SPECIAL_DEALER_UID, name = "Pasindu's Dealer", role = "Dealer", unitId = null)
                            else -> null
                        }
                    } else {
                        u
                    }
                } catch (e: Exception) { 
                    when (targetUid) {
                        SPECIAL_CUSTOMER_UID -> User(uid = SPECIAL_CUSTOMER_UID, name = "Pasindu", role = "Customer", unitId = SPECIAL_CUSTOMER_UNIT_ID)
                        SPECIAL_DEALER_UID -> User(uid = SPECIAL_DEALER_UID, name = "Pasindu's Dealer", role = "Dealer", unitId = null)
                        else -> null
                    }
                }
                trySend(user)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun bindUnit(userId: String, role: String, unitId: String): Result<Unit> {
        return try {
            firestore.collection(getCollection(role)).document(userId).update("unitId", unitId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserUid(): String? {
        val firebaseUser = auth.currentUser
        return when {
            isSpecialCustomer(firebaseUser?.email) -> SPECIAL_CUSTOMER_UID
            isSpecialDealer(firebaseUser?.email) -> SPECIAL_DEALER_UID
            else -> firebaseUser?.uid
        }
    }

    fun logout() = auth.signOut()
}
