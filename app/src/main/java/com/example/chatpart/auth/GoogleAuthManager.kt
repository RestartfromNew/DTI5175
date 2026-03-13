package com.example.chatpart.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.chatpart.network.APIService
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    companion object {
        const val WEB_CLIENT_ID = "557921543964-5fviq7q9spg7uhhmrpkei3fkgfhkfnnr.apps.googleusercontent.com"
        private const val TAG = "GoogleAuthManager"
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    data class GoogleLoginResult(
        val user: FirebaseUser,
        val idToken: String
    )

    /**
     * Sign in using the "Sign in with Google" button flow.
     * This uses GetSignInWithGoogleOption which always shows the account picker,
     * and works more reliably than GetGoogleIdOption on emulators.
     */
    suspend fun signIn(activityContext: Activity): Result<FirebaseUser> {
        return try {
            // Use GetSignInWithGoogleOption - this is the "Sign in with Google" button flow
            // It's more reliable than GetGoogleIdOption and always shows the Google sign-in UI
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            Log.d(TAG, "Starting Google Sign-In...")

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext  // Must be Activity context!
            )

            handleSignInResult(result)
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credentials available. Make sure a Google account is added to the device.", e)
            Result.failure(Exception("No Google account found on this device. Please add a Google account in device Settings > Accounts."))
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user")
            Result.failure(Exception("Sign-in was cancelled."))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Result<FirebaseUser> {
        val credential = result.credential

        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()
                        val user = authResult.user

                        if (user != null) {
                            val tokenResult = user.getIdToken(true).await()
                            val idToken = tokenResult.token
                            Log.d(TAG, "✅ Sign-in successful: ${user.displayName} (${user.email})")
                            Log.d(TAG, "✅ Avatar URL: ${user.photoUrl}")
                            if (idToken != null) {
                                APIService.googleLogin(this.context, idToken)
                            }
                            Result.success(user)
                        } else {
                            Result.failure(Exception("Sign-in returned null user"))
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Invalid Google ID token", e)
                        Result.failure(e)
                    }
                } else {
                    Result.failure(Exception("Unexpected credential type: ${credential.type}"))
                }
            }
            else -> {
                Result.failure(Exception("Unexpected credential type"))
            }
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "✅ Signed out successfully")
    }
}
