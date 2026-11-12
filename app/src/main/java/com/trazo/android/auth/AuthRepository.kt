package com.trazo.android.auth
class AuthRepository(private val api:AuthApi,private val store:SecureTokenStore) {
 suspend fun login(email:String,password:String)=consume(api.login(LoginRequest(email.trim(),password)))
 suspend fun register(email:String,password:String,name:String)=consume(api.register(RegisterRequest(email.trim(),password,name.trim())))
 suspend fun google(idToken:String)=consume(api.google(GoogleLoginRequest(idToken)))
 suspend fun resendVerification(email:String)=api.resendVerification(EmailRequest(email.trim()))
 suspend fun forgotPassword(email:String)=api.forgotPassword(EmailRequest(email.trim()))
 suspend fun verifyEmail(token:String)=api.verifyEmail(EmailTokenRequest(token))
 suspend fun resetPassword(token:String,newPassword:String)=api.resetPassword(ResetPasswordRequest(token,newPassword))
 suspend fun verify(challenge:String,code:String)=consume(api.verify(TwoFactorVerifyRequest(challenge,code)))
 fun hasSession()=store.access()!=null
 fun logout()=store.clear()
 private fun consume(r:AuthResponse):Outcome {
  if(r.emailVerificationRequired)return Outcome.EmailVerificationRequired(r.message ?: "Revisa tu correo para verificar la cuenta.")
  if(r.twoFactorRequired)return r.challengeToken?.let{Outcome.Challenge(it)}?:Outcome.Error("Falta el reto 2FA.")
  if(r.accessToken.isNullOrBlank()||r.refreshToken.isNullOrBlank())return Outcome.Error("Respuesta de autenticación incompleta.")
  store.save(r.accessToken,r.refreshToken);return Outcome.Success
 }
}
sealed interface Outcome { data object Success:Outcome; data class Challenge(val token:String):Outcome; data class EmailVerificationRequired(val message:String):Outcome; data class Error(val message:String):Outcome }
