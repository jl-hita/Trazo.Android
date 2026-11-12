package com.trazo.android.auth
import retrofit2.http.Body
import retrofit2.http.POST
data class LoginRequest(val email:String,val password:String,val deviceName:String="Android")
data class RegisterRequest(val email:String,val password:String,val displayName:String,val deviceName:String="Android")
data class GoogleLoginRequest(val idToken:String,val deviceName:String="Android")
data class TwoFactorVerifyRequest(val challengeToken:String,val code:String,val deviceName:String="Android")
data class AuthResponse(val accessToken:String?=null,val refreshToken:String?=null,val twoFactorRequired:Boolean=false,val challengeToken:String?=null)
interface AuthApi {
 @POST("api/v1/auth/register") suspend fun register(@Body r:RegisterRequest):AuthResponse
 @POST("api/v1/auth/login") suspend fun login(@Body r:LoginRequest):AuthResponse
 @POST("api/v1/auth/google") suspend fun google(@Body r:GoogleLoginRequest):AuthResponse
 @POST("api/v1/auth/2fa/verify") suspend fun verify(@Body r:TwoFactorVerifyRequest):AuthResponse
}
