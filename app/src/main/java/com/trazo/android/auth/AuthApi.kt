package com.trazo.android.auth
import retrofit2.http.Body
import retrofit2.http.POST
data class LoginRequest(val email:String,val password:String,val deviceName:String="Android")
data class RegisterRequest(val email:String,val password:String,val displayName:String,val deviceName:String="Android")
data class GoogleLoginRequest(val idToken:String,val deviceName:String="Android")
data class TwoFactorVerifyRequest(val challengeToken:String,val code:String,val deviceName:String="Android")
data class AuthResponse(val accessToken:String?=null,val refreshToken:String?=null,val twoFactorRequired:Boolean=false,val challengeToken:String?=null,val emailVerificationRequired:Boolean=false,val message:String?=null)
data class EmailRequest(val email:String)
data class EmailTokenRequest(val token:String)
data class ResetPasswordRequest(val token:String,val newPassword:String)
interface AuthApi {
 @POST("api/v1/auth/register") suspend fun register(@Body r:RegisterRequest):AuthResponse
 @POST("api/v1/auth/login") suspend fun login(@Body r:LoginRequest):AuthResponse
 @POST("api/v1/auth/google") suspend fun google(@Body r:GoogleLoginRequest):AuthResponse
 @POST("api/v1/auth/2fa/verify") suspend fun verify(@Body r:TwoFactorVerifyRequest):AuthResponse
 @POST("api/v1/auth/email/resend") suspend fun resendVerification(@Body r:EmailRequest):retrofit2.Response<Unit>
 @POST("api/v1/auth/password/forgot") suspend fun forgotPassword(@Body r:EmailRequest):retrofit2.Response<Unit>
 @POST("api/v1/auth/email/verify") suspend fun verifyEmail(@Body r:EmailTokenRequest):retrofit2.Response<Unit>
 @POST("api/v1/auth/password/reset") suspend fun resetPassword(@Body r:ResetPasswordRequest):retrofit2.Response<Unit>
}
