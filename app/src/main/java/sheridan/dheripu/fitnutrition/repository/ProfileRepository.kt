package sheridan.dheripu.fitnutrition.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import sheridan.dheripu.fitnutrition.model.User

/**
 * Persists a user's profile in the Firestore document whose id matches their Firebase Auth uid.
 * A snapshot listener keeps the profile current when it is changed from another device.
 */
class ProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun profileDocument(userId: String) = firestore.collection(USERS_COLLECTION).document(userId)

    fun observeProfile(
        userId: String,
        onProfileChanged: (User?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration = profileDocument(userId).addSnapshotListener { snapshot, error ->
        if (error != null) {
            onError(error.message ?: "Unable to load your profile")
            return@addSnapshotListener
        }

        val profile = snapshot
            ?.takeIf { it.exists() }
            ?.toObject(User::class.java)
            ?.copy(id = userId)

        onProfileChanged(profile)
    }

    fun saveProfile(
        profile: User,
        isNewProfile: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val profileValues = hashMapOf<String, Any>(
            "email" to profile.email,
            "name" to profile.name,
            "weight" to profile.weight,
            "height" to profile.height,
            "fitnessGoal" to profile.fitnessGoal,
            "activityLevel" to profile.activityLevel,
            "dailyCalorieTarget" to profile.dailyCalorieTarget,
            "dailyProteinTarget" to profile.dailyProteinTarget,
            "dietaryPreference" to profile.dietaryPreference,
            "dietaryRestrictions" to profile.dietaryRestrictions,
            "availableEquipment" to profile.availableEquipment,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (isNewProfile) {
            profileValues["createdAt"] = FieldValue.serverTimestamp()
        }

        // Merge protects any future fields and never affects the user's workout subcollection.
        profileDocument(profile.id)
            .set(profileValues, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Unable to save your profile")
            }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
    }
}
