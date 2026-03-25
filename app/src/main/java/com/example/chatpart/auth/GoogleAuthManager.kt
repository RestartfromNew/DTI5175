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
    private val tokenSyncManager = TokenSyncManager()

    companion object {
        const val WEB_CLIENT_ID = "557921543964-5fviq7q9spg7uhhmrpkei3fkgfhkfnnr.apps.googleusercontent.com"
        private const val TAG = "GoogleAuthManager"
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Get the current user's Firebase ID Token / 获取当前用户的 Firebase ID Token
     * Used to send to your own server for authentication / 用于发送到自己的服务器进行身份验证
     */
    suspend fun getFirebaseIdToken(): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(true).await().token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase ID token", e)
            null
        }
    }

    /**
     * Sign in and sync to server / 登录并同步到服务器
     * After Firebase login succeeds, automatically sync user info to the configured server
     * 在 Firebase 登录成功后，自动将用户信息同步到配置的服务器
     */
    suspend fun signInAndSync(activityContext: Activity): Result<LoginSyncResult> {
        val signInResult = signIn(activityContext)

        return signInResult.fold(
            onSuccess = { user ->
                // Login success, try to sync to server / 登录成功，尝试同步到服务器
                syncToServer(user).fold(
                    onSuccess = { syncResult ->
                        Result.success(LoginSyncResult(user, syncResult))
                    },
                    onFailure = { syncError ->
                        // Server sync failed, but login succeeded / 服务器同步失败，但登录成功
                        // Can choose to return success or failure based on business needs
                        // 可以选择返回成功或失败，取决于业务需求
                        Log.w(TAG, "Login succeeded but server sync failed: ${syncError.message}")
                        Result.success(LoginSyncResult(user, null))
                    }
                )
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    /**
     * Sync login info to server / 将登录信息同步到服务器
     */
    private suspend fun syncToServer(user: FirebaseUser): Result<TokenSyncManager.SyncResponse> {
        val idToken = getFirebaseIdToken() ?: run {
            return Result.failure(Exception("Failed to get Firebase ID token"))
        }

        return tokenSyncManager.syncLoginToServer(
            firebaseToken = idToken,
            userEmail = user.email ?: "",
            userName = user.displayName ?: "",
            userId = user.uid
        )
    }

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
                            Log.d(TAG, "✅ Sign-in successful: ${user.displayName} (${user.email})")
                            Log.d(TAG, "✅ Avatar URL: ${user.photoUrl}")
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

    /**
     * Complete result of login and sync / 登录并同步的完整结果
     *
     * @param firebaseUser Firebase user object / Firebase 用户对象
     * @param syncResponse Server sync response (may be null if sync failed) / 服务器同步响应（可能为 null 如果同步失败）
     */
    data class LoginSyncResult(
        val firebaseUser: FirebaseUser,
        val syncResponse: TokenSyncManager.SyncResponse?
    ) {
        /**
         * Is server sync successful / 服务器同步是否成功
         */
        val isServerSyncSuccess: Boolean
            get() = syncResponse?.success == true

        /**
         * Get server returned token / 获取服务器返回的 token
         */
        val serverToken: String?
            get() = syncResponse?.server_token

        /**
         * Get user binding ID / 获取用户绑定 ID
         */
        val userBindingId: String?
            get() = syncResponse?.user_binding_id
    }
}
