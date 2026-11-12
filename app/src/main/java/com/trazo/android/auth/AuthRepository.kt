package com.trazo.android.auth
class AuthRepository(private val api:AuthApi,private val store:SecureTokenStore) {
 suspend fun login(email:String,password:String)=consume(api.login(LoginRequest(email.trim(),password)))
 suspend fun register(email:String,password:String,name:String)=consume(api.register(RegisterRequest(email.trim(),password,name.trim())))
 suspend fun google(idToken:String)=consume(api.google(GoogleLoginRequest(idToken)))
 suspend fun verify(challenge:String,code:String)=consume(api.verify(TwoFactorVerifyRequest(challenge,code)))
 fun hasSession()=store.access()!=null
 fun logout()=store.clear()
 private fun consume(r:AuthResponse):Outcome {
  if(r.twoFactorRequired)return r.challengeToken?.let{Outcome.Challenge(it)}?:Outcome.Error("Falta el reto 2FA.")
  if(r.accessToken.isNullOrBlank()||r.refreshToken.isNullOrBlank())return Outcome.Error("Respuesta de autenticación incompleta.")
  store.save(r.accessToken,r.refreshToken);return Outcome.Success
 }
}
sealed interface Outcome { data object Success:Outcome; data class Challenge(val token:String):Outcome; data class Error(val message:String):Outcome }
