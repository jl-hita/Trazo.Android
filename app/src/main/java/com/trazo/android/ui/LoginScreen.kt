package com.trazo.android.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.*
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.auth.*
import com.trazo.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(repo:AuthRepository,googleClientId:String,actionType:String?=null,actionToken:String?=null) {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var email by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")}
 var name by remember{mutableStateOf("")}
 var code by remember{mutableStateOf("")}
 var challenge by remember{mutableStateOf<String?>(null)}
 var newPassword by remember{mutableStateOf("")}
 var register by remember{mutableStateOf(false)}
 var busy by remember{mutableStateOf(false)}
 var signed by remember{mutableStateOf(false)}
 var user by remember{mutableStateOf<UserSummary?>(null)}
 var setup by remember{mutableStateOf<TwoFactorSetupResponse?>(null)}
 var recoveryCodes by remember{mutableStateOf<List<String>>(emptyList())}
 var message by remember{mutableStateOf<String?>(null)}

 LaunchedEffect(Unit) {
  signed=repo.restoreSession()
  if(signed) user=repo.currentUser()
  if(signed&&user==null) signed=false
 }
 LaunchedEffect(actionType,actionToken) {
  if(actionType=="verify-email"&&!actionToken.isNullOrBlank()) {
   try { val response=repo.verifyEmail(actionToken);message=if(response.isSuccessful)"Correo verificado. Ya puedes iniciar sesión." else "El enlace no es válido o ha caducado." }
   catch(_:Exception){message="No se pudo verificar el correo. Comprueba la conexión."}
  }
 }
 fun handle(result:Outcome) {
  when(result) {
   Outcome.Success->{signed=true;challenge=null;message="Sesión iniciada.";scope.launch{user=repo.currentUser()}}
   is Outcome.Challenge->{challenge=result.token;message="Introduce el código de tu aplicación autenticadora."}
   is Outcome.EmailVerificationRequired->message=result.message
   is Outcome.Error->message=result.message
  }
 }

 Surface(Modifier.fillMaxSize(),color=TrazoPalette.Background,contentColor=TrazoPalette.Text) {
 Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
 Column(Modifier.widthIn(max=440.dp).fillMaxWidth().border(1.dp,TrazoPalette.Border,RoundedCornerShape(10.dp)).background(TrazoPalette.Card,RoundedCornerShape(10.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Image(painterResource(R.drawable.trazo_wordmark_dark),contentDescription="TRAZO",modifier=Modifier.height(64.dp))
  Eyebrow("ENTRENA · MEJORA · AVANZA")
  Spacer(Modifier.height(24.dp))
  if(actionType=="reset-password"&&!actionToken.isNullOrBlank()) {
   Text("Restablecer contraseña",style=MaterialTheme.typography.titleLarge)
   TrazoField(newPassword,{newPassword=it},label={Text("Nueva contraseña")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
   TrazoButton(enabled=!busy&&newPassword.length>=12,onClick={
    busy=true
    scope.launch { try { val response=repo.resetPassword(actionToken,newPassword);message=if(response.isSuccessful)"Contraseña actualizada. Ya puedes iniciar sesión." else "El enlace no es válido o ha caducado." } catch(_:Exception){message="No se pudo restablecer la contraseña."} finally{busy=false} }
   },modifier=Modifier.fillMaxWidth()){Text("Guardar contraseña")}
  } else if(signed) {
   Text("Sesión activa",style=MaterialTheme.typography.titleLarge)
   user?.let {
    Text(it.displayName)
    Text(it.email,style=MaterialTheme.typography.bodySmall)
    Text("Rol: ${it.role}")
    Text(if(it.twoFactorEnabled)"Autenticación en dos pasos: activada" else "Autenticación en dos pasos: desactivada")
   }
   Spacer(Modifier.height(12.dp))
   if(user?.twoFactorEnabled==true) {
    TrazoField(code,{code=it},label={Text("Código para desactivar 2FA")},modifier=Modifier.fillMaxWidth())
    TrazoButton(enabled=!busy&&code.isNotBlank(),onClick={
     busy=true
     scope.launch { try { repo.disableTwoFactor(code);code="";user=repo.currentUser();message="Autenticación en dos pasos desactivada." } catch(e:Exception){message=e.message?: "No se pudo desactivar 2FA."} finally{busy=false} }
    },modifier=Modifier.fillMaxWidth()){Text("Desactivar 2FA")}
   } else {
    TrazoButton(enabled=!busy,onClick={
     busy=true
     scope.launch { try { setup=repo.setupTwoFactor();message="Añade la clave o URI a tu aplicación autenticadora y confirma el código." } catch(e:Exception){message=e.message?: "No se pudo iniciar 2FA."} finally{busy=false} }
    },modifier=Modifier.fillMaxWidth()){Text("Configurar 2FA")}
    setup?.let { details->
     Text("Clave secreta (guárdala de forma segura):",style=MaterialTheme.typography.titleSmall)
     Text(details.secret)
     Text("URI de configuración:",style=MaterialTheme.typography.titleSmall)
     Text(details.provisioningUri,style=MaterialTheme.typography.bodySmall)
     TrazoField(code,{code=it},label={Text("Código de 6 dígitos")},modifier=Modifier.fillMaxWidth())
     TrazoButton(enabled=!busy&&code.isNotBlank(),onClick={
      busy=true
      scope.launch { try { recoveryCodes=repo.enableTwoFactor(code).recoveryCodes;setup=null;code="";user=repo.currentUser();message="2FA activado. Guarda los códigos de recuperación: solo se muestran ahora." } catch(e:Exception){message=e.message?: "El código no es válido."} finally{busy=false} }
     },modifier=Modifier.fillMaxWidth()){Text("Activar 2FA")}
    }
   }
   if(recoveryCodes.isNotEmpty()) {
    Text("Códigos de recuperación — guárdalos ahora",style=MaterialTheme.typography.titleSmall)
    recoveryCodes.forEach{Text(it)}
   }
   TrazoLink(onClick={
    busy=true
    scope.launch { try { repo.logout();signed=false;user=null;setup=null;recoveryCodes=emptyList();message="Sesión cerrada." } catch(_:Exception){message="No se pudo cerrar la sesión."} finally{busy=false} }
   }){Text("Cerrar sesión")}
  } else if(challenge!=null) {
   TrazoField(code,{code=it},label={Text("Código 2FA o recuperación")},modifier=Modifier.fillMaxWidth())
   TrazoButton(enabled=!busy&&code.isNotBlank(),onClick={
    busy=true
    scope.launch { try{handle(repo.verify(challenge!!,code))}catch(_:Exception){message="No se pudo verificar el código."}finally{busy=false} }
   },modifier=Modifier.fillMaxWidth()){Text("Verificar")}
  } else {
   if(register)TrazoField(name,{name=it},label={Text("Nombre")},modifier=Modifier.fillMaxWidth())
   TrazoField(email,{email=it},label={Text("Correo electrónico")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth())
   TrazoField(password,{password=it},label={Text("Contraseña")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(12.dp))
   TrazoButton(enabled=!busy&&email.isNotBlank()&&password.isNotBlank()&&(!register||name.isNotBlank()),onClick={
    busy=true
    scope.launch { try{handle(if(register)repo.register(email,password,name) else repo.login(email,password))}catch(_:Exception){message="No se pudo conectar con TRAZO. Comprueba la URL de la API."}finally{busy=false} }
   },modifier=Modifier.fillMaxWidth()){Text(if(register)"Crear cuenta" else "Entrar")}
   TrazoLink(onClick={register=!register;message=null}){Text(if(register)"Ya tengo cuenta" else "Crear cuenta")}
   TrazoLink(enabled=email.isNotBlank(),onClick={
    scope.launch { try{repo.resendVerification(email);message="Si la cuenta existe y falta verificarla, recibirás un correo."}catch(_:Exception){message="No se pudo solicitar la verificación."} }
   }){Text("Reenviar verificación")}
   TrazoLink(enabled=email.isNotBlank(),onClick={
    scope.launch { try{repo.forgotPassword(email);message="Si existe una cuenta compatible, recibirás instrucciones."}catch(_:Exception){message="No se pudo solicitar el restablecimiento."} }
   }){Text("He olvidado mi contraseña")}
   if(googleClientId.isNotBlank()) {
    TrazoButton(enabled=!busy,onClick={
     busy=true
     scope.launch {
      try {
       val option=GetSignInWithGoogleOption.Builder(googleClientId).build()
       val request=GetCredentialRequest.Builder().addCredentialOption(option).build()
       val credential=CredentialManager.create(context).getCredential(context,request).credential
       if(credential is CustomCredential&&credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        handle(repo.google(GoogleIdTokenCredential.createFrom(credential.data).idToken))
       } else message="Google devolvió un tipo de credencial inesperado."
      } catch(_:GetCredentialCancellationException){message=null}
      catch(e:NoCredentialException){message="No hay ninguna cuenta de Google disponible en este dispositivo."}
      catch(e:GetCredentialException){message=if(BuildConfig.DEBUG)"Google Sign-In falló (${e.type}): ${e.message}" else "Google Sign-In no está disponible en este momento."}
      catch(_:Exception){message="No se pudo conectar con TRAZO. Comprueba la URL de la API."}
      finally{busy=false}
     }
    },secondary=true,modifier=Modifier.fillMaxWidth()){Text("Continuar con Google")}
   } else Text("Google Sign-In requiere configurar googleWebClientId.",style=MaterialTheme.typography.bodySmall)
  }
  message?.let{Spacer(Modifier.height(12.dp));Text(it)}
 }
 }
 }
}
