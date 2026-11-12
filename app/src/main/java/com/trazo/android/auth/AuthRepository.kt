package com.trazo.android.auth
class AuthRepository(private val api:AuthApi,private val store:SecureTokenStore) {
 suspend fun login(email:String,password:String)=consume(api.login(LoginRequest(email.trim(),password)))
 suspend fun register(email:String,password:String,name:String)=consume(api.register(RegisterRequest(email.trim(),password,name.trim())))
 suspend fun google(idToken:String)=consume(api.google(GoogleLoginRequest(idToken)))
 suspend fun verify(challenge:String,code:String)=consume(api.verifyTwoFactor(TwoFactorVerifyRequest(challenge,code.trim())))
 suspend fun restoreSession():Boolean {
  val token=store.refresh()?:run{store.clear();return false}
  return try { val response=api.refresh(RefreshRequest(token));if(!response.isSuccessful||response.body()==null){store.clear();false}else{consume(response);store.access()!=null} }
  catch(_:Exception){false}
 }
 suspend fun currentUser():UserSummary? { val access=store.access()?:return null;val response=api.me("Bearer $access");if(response.code()==401)store.clear();return if(response.isSuccessful)response.body() else null }
 suspend fun logout(){val access=store.access();val refresh=store.refresh();try{if(!access.isNullOrBlank()&&!refresh.isNullOrBlank())api.logout("Bearer $access",RefreshRequest(refresh))}finally{store.clear()}}
 suspend fun setupTwoFactor():TwoFactorSetupResponse { val response=api.setupTwoFactor(bearer());return response.body()?:throw IllegalStateException("No se pudo iniciar la configuración de 2FA (HTTP ${response.code()}).") }
 suspend fun enableTwoFactor(code:String):TwoFactorEnableResponse { val response=api.enableTwoFactor(bearer(),TwoFactorCodeRequest(code.trim()));return response.body()?:throw IllegalStateException("El código 2FA no es válido (HTTP ${response.code()}).") }
 suspend fun disableTwoFactor(code:String){val response=api.disableTwoFactor(bearer(),TwoFactorCodeRequest(code.trim()));if(!response.isSuccessful)throw IllegalStateException("No se pudo desactivar 2FA (HTTP ${response.code()}).")}
 suspend fun resendVerification(email:String)=api.resendVerification(EmailRequest(email.trim()))
 suspend fun forgotPassword(email:String)=api.forgotPassword(EmailRequest(email.trim()))
 suspend fun verifyEmail(token:String)=api.verifyEmail(EmailTokenRequest(token))
 suspend fun resetPassword(token:String,newPassword:String)=api.resetPassword(ResetPasswordRequest(token,newPassword))
 private fun bearer()="Bearer ${store.access()?:throw IllegalStateException("No hay una sesión activa.")}"
 private fun consume(response:retrofit2.Response<AuthResponse>):Outcome {
  val body=response.body()
  if(response.code()==409)return Outcome.EmailVerificationRequired("Verifica tu correo antes de iniciar sesión.")
  if(!response.isSuccessful||body==null)return Outcome.Error("No se pudo autenticar (HTTP ${response.code()}). Comprueba tus datos y la conexión.")
  if(body.emailVerificationRequired)return Outcome.EmailVerificationRequired(body.message?:"Revisa tu correo para verificar la cuenta.")
  if(body.twoFactorRequired)return body.challengeToken?.let{Outcome.Challenge(it)}?:Outcome.Error("La API no ha devuelto el reto 2FA.")
  if(body.accessToken.isNullOrBlank()||body.refreshToken.isNullOrBlank())return Outcome.Error("La respuesta de autenticación está incompleta.")
  store.save(body.accessToken,body.refreshToken);return Outcome.Success
 }
}
sealed interface Outcome { data object Success:Outcome;data class Challenge(val token:String):Outcome;data class EmailVerificationRequired(val message:String):Outcome;data class Error(val message:String):Outcome }
