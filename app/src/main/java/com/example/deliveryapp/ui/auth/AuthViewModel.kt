package com.example.deliveryapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.*
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.utils.JwtUtils
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    // Signup flow
    private val _signupState = MutableStateFlow<Resource<Unit>?>(null)
    val signupState: StateFlow<Resource<Unit>?> = _signupState

    private val _verifyOtpState = MutableStateFlow<Resource<Unit>?>(null)
    val verifyOtpState: StateFlow<Resource<Unit>?> = _verifyOtpState

    // Login flow
    private val _loginState = MutableStateFlow<Resource<AuthResponseDto>?>(null)
    val loginState: StateFlow<Resource<AuthResponseDto>?> = _loginState

    // Forgot password flow
    private val _forgotPassState = MutableStateFlow<Resource<Unit>?>(null)
    val forgotPassState: StateFlow<Resource<Unit>?> = _forgotPassState

    private val _verifyResetOtpState = MutableStateFlow<Resource<ResetTokenDto>?>(null)
    val verifyResetOtpState: StateFlow<Resource<ResetTokenDto>?> = _verifyResetOtpState

    private val _resetPasswordState = MutableStateFlow<Resource<Unit>?>(null)
    val resetPasswordState: StateFlow<Resource<Unit>?> = _resetPasswordState

    // ---- Functions ----
    fun signup(req: SignupRequestDto) = launchResource(_signupState) { repo.signup(req) }
    fun verifyOtp(email: String, otp: String) =
        launchResource(_verifyOtpState) { repo.verifyOtp(VerifyOtpRequestDto(email, otp)) }
//    fun login(email: String, password: String) =
//        launchResource(_loginState) { repo.login(LoginRequestDto(email, password)) }
    fun forgotPassword(email: String) =
        launchResource(_forgotPassState) { repo.forgotPassword(email) }
    fun verifyOtpForReset(email: String, otp: String) =
        launchResource(_verifyResetOtpState) { repo.verifyOtpForReset(email, otp) }
    fun resetPassword(token: String, newPassword: String) =
        launchResource(_resetPasswordState) { repo.resetPassword(token, newPassword) }

    // Generic launcher
    private fun <T> launchResource(
        state: MutableStateFlow<Resource<T>?>,
        block: suspend () -> Resource<T>
    ) {
        viewModelScope.launch {
            state.value = Resource.Loading()
            state.value = block()
        }
    }

    //chặn đăng nhập nhiều thiết bị với role sai
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()

            val result = repo.login(LoginRequestDto(email, password))

            if (result is Resource.Success) {
                val token = result.data?.accessToken
                val role = token?.let { JwtUtils.getRoleFromToken(it) }

                if (role != "customer") {
                    _loginState.value = Resource.Error(
                        "❌ Tài khoản này không được phép đăng nhập ứng dụng mua hàng"
                    )
                    return@launch
                }
            }

            _loginState.value = result
        }
    }

}