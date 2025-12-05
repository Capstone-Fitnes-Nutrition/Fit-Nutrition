package sheridan.dheripu.fitnutrition

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
        val listener = OnCompleteListener<AuthResult> { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener)
    }

    fun logout() {
        auth.signOut()
    }
}
