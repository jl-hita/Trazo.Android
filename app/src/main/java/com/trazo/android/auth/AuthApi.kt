package com.trazo.android.auth
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
data class LoginRequest(val email:String,val password:String,val deviceName:String="Android")
data class RegisterRequest(val email:String,val password:String,val displayName:String,val deviceName:String="Android")
data class GoogleLoginRequest(val idToken:String,val deviceName:String="Android")
data class TwoFactorVerifyRequest(val challengeToken:String,val code:String,val deviceName:String="Android")
data class RefreshRequest(val refreshToken:String,val deviceName:String="Android")
data class AuthResponse(val accessToken:String?=null,val refreshToken:String?=null,val twoFactorRequired:Boolean=false,val challengeToken:String?=null,val emailVerificationRequired:Boolean=false,val message:String?=null)
data class GoogleLinkRequest(val idToken:String)
data class EmailRequest(val email:String)
data class EmailTokenRequest(val token:String)
data class ResetPasswordRequest(val token:String,val newPassword:String)
data class TwoFactorCodeRequest(val code:String)
data class UserSummary(val id:String,val email:String,val displayName:String,val role:String,val twoFactorEnabled:Boolean)
data class TwoFactorSetupResponse(val secret:String,val provisioningUri:String)
data class TwoFactorEnableResponse(val enabled:Boolean,val recoveryCodes:List<String>)
interface AuthApi {
 @POST("api/v1/auth/register") suspend fun register(@Body request:RegisterRequest):Response<AuthResponse>
 @POST("api/v1/auth/login") suspend fun login(@Body request:LoginRequest):Response<AuthResponse>
 @POST("api/v1/auth/google") suspend fun google(@Body request:GoogleLoginRequest):Response<AuthResponse>
 @POST("api/v1/auth/2fa/verify") suspend fun verifyTwoFactor(@Body request:TwoFactorVerifyRequest):Response<AuthResponse>
 @POST("api/v1/auth/refresh") suspend fun refresh(@Body request:RefreshRequest):Response<AuthResponse>
 @POST("api/v1/auth/logout") suspend fun logout(@Header("Authorization") authorization:String,@Body request:RefreshRequest):Response<Unit>
 @GET("api/v1/auth/me") suspend fun me(@Header("Authorization") authorization:String):Response<UserSummary>
 @POST("api/v1/auth/logout-all") suspend fun logoutAll(@Header("Authorization") authorization:String):Response<Unit>
 @POST("api/v1/auth/google/link") suspend fun linkGoogle(@Header("Authorization") authorization:String,@Body request:GoogleLinkRequest):Response<Unit>
 @POST("api/v1/auth/2fa/setup") suspend fun setupTwoFactor(@Header("Authorization") authorization:String):Response<TwoFactorSetupResponse>
 @POST("api/v1/auth/2fa/enable") suspend fun enableTwoFactor(@Header("Authorization") authorization:String,@Body request:TwoFactorCodeRequest):Response<TwoFactorEnableResponse>
 @POST("api/v1/auth/2fa/disable") suspend fun disableTwoFactor(@Header("Authorization") authorization:String,@Body request:TwoFactorCodeRequest):Response<Unit>
 @POST("api/v1/auth/email/resend") suspend fun resendVerification(@Body request:EmailRequest):Response<Unit>
 @POST("api/v1/auth/password/forgot") suspend fun forgotPassword(@Body request:EmailRequest):Response<Unit>
 @POST("api/v1/auth/email/verify") suspend fun verifyEmail(@Body request:EmailTokenRequest):Response<Unit>
 @POST("api/v1/auth/password/reset") suspend fun resetPassword(@Body request:ResetPasswordRequest):Response<Unit>
}
