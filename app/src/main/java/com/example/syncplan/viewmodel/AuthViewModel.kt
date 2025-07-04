package com.example.syncplan.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profilePictureUrl: String
)

class AuthViewModel : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val callbackManager = CallbackManager.Factory.create()

    init {
        checkLoginStatus()
    }

    fun getCallbackManager(): CallbackManager = callbackManager

    private fun checkLoginStatus() {
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        _isLoggedIn.value = isLoggedIn
        if (isLoggedIn) {
            fetchUserInfo()
        }
    }

    fun handleFacebookLoginResult(loginResult: LoginResult) {
        _isLoading.value = true
        _errorMessage.value = null

        val request = GraphRequest.newMeRequest(
            loginResult.accessToken
        ) { jsonObject, _ ->
            _isLoading.value = false
            if (jsonObject != null) {
                try {
                    val user = User(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        email = jsonObject.optString("email", ""),
                        profilePictureUrl = "https://graph.facebook.com/${jsonObject.getString("id")}/picture?type=large"
                    )

                    _currentUser.value = user
                    _isLoggedIn.value = true
                } catch (e: Exception) {
                    _errorMessage.value = "Błąd podczas pobierania danych użytkownika"
                }
            } else {
                _errorMessage.value = "Nie udało się pobrać danych użytkownika"
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    fun handleFacebookLoginError(error: FacebookException) {
        _isLoading.value = false
        _errorMessage.value = when (error) {
            is FacebookAuthorizationException -> "Autoryzacja została anulowana"
            is FacebookServiceException -> "Błąd serwera Facebook"
            else -> "Błąd logowania: ${error.message}"
        }
    }

    fun handleFacebookLoginCancel() {
        _isLoading.value = false
        _errorMessage.value = "Logowanie zostało anulowane"
    }

    private fun fetchUserInfo() {
        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { jsonObject, _ ->
            if (jsonObject != null) {
                try {
                    val user = User(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        email = jsonObject.optString("email", ""),
                        profilePictureUrl = "https://graph.facebook.com/${jsonObject.getString("id")}/picture?type=large"
                    )

                    _currentUser.value = user
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    fun logout() {
        LoginManager.getInstance().logOut()
        _isLoggedIn.value = false
        _currentUser.value = null
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
