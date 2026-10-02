package sheridan.dheripu.fitnutrition

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import sheridan.dheripu.fitnutrition.model.User
import sheridan.dheripu.fitnutrition.repository.ProfileRepository

object AuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val profileRepository = ProfileRepository()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val listener = OnCompleteListener<AuthResult> { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener)
    }

    fun register(
        email: String,
        password: String,
        name: String,
        weight: String,
        height: String,
        fitnessGoal: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(false, task.exception?.message ?: "Unable to create account")
                    return@addOnCompleteListener
                }

                val firebaseUser = task.result?.user
                if (firebaseUser == null) {
                    onResult(false, "Account was created, but user details could not be loaded")
                    return@addOnCompleteListener
                }

                firebaseUser.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                )

                profileRepository.saveProfile(
                    profile = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email.orEmpty(),
                        name = name.trim(),
                        weight = weight.trim(),
                        height = height.trim(),
                        fitnessGoal = fitnessGoal
                    ),
                    isNewProfile = true,
                    onSuccess = { onResult(true, null) },
                    onError = { message ->
                        onResult(
                            false,
                            "Account created, but the profile could not be saved: $message"
                        )
                    }
                )
            }
    }

    fun logout() {
        auth.signOut()
    }
}
