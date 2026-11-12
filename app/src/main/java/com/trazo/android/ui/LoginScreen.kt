package com.trazo.android.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.*
import com.trazo.android.auth.*
import kotlinx.coroutines.launch
@Composable fun LoginScreen(repo:AuthRepository,googleClientId:String) {
 val context= LocalContext.current; val scope= rememberCoroutineScope()
 var email by remember{mutableStateOf("")}; var password by remember{mutableStateOf("")}; var name by remember{mutableStateOf("")}
 var code by remember{mutableStateOf("")}; var challenge by remember{mutableStateOf<String?>(null)}
 var register by remember{mutableStateOf(false)}; var busy by remember{mutableStateOf(false)}
 var signed by remember{mutableStateOf(repo.hasSession())}; var message by remember{mutableStateOf<String?>(null)}
 fun handle(o:Outcome){ when(o){ Outcome.Success->{signed=true;challenge=null;message="Sesión iniciada."};is Outcome.Challenge->{challenge=o.token;message="Introduce tu código de autenticación."};is Outcome.Error->message=o.message } }
 Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center) {
  Text("TRAZO",style=MaterialTheme.typography.headlineLarge); Text("STRENGTH, ENGINEERED.")
  Spacer(Modifier.height(24.dp))
  if(signed){Text("Sesión iniciada.");TextButton(onClick={repo.logout();signed=false}){Text("Cerrar sesión")}}
  else if(challenge!=null){
   OutlinedTextField(code,{code=it},label={Text("Código 2FA o recuperación")},modifier=Modifier.fillMaxWidth())
   Button(enabled=!busy&&code.isNotBlank(),onClick={busy=true;scope.launch{try{handle(repo.verify(challenge!!,code))}catch(e:Exception){message=e.message};busy=false}},modifier=Modifier.fillMaxWidth()){Text("Verificar")}
  } else {
   if(register)OutlinedTextField(name,{name=it},label={Text("Nombre")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(email,{email=it},label={Text("Correo")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth())
   OutlinedTextField(password,{password=it},label={Text("Contraseña")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
   Spacer(Modifier.height(12.dp))
   Button(enabled=!busy&&email.isNotBlank()&&password.isNotBlank(),onClick={busy=true;scope.launch{try{handle(if(register)repo.register(email,password,name) else repo.login(email,password))}catch(e:Exception){message=e.message?: "Error de conexión"};busy=false}},modifier=Modifier.fillMaxWidth()){Text(if(register)"Crear cuenta" else "Entrar")}
   TextButton(onClick={register=!register}){Text(if(register)"Ya tengo cuenta" else "Crear cuenta")}
   if(googleClientId.isNotBlank())Button(enabled=!busy,onClick={busy=true;scope.launch{try{
    val option=GetGoogleIdOption.Builder().setServerClientId(googleClientId).setFilterByAuthorizedAccounts(false).build()
    val req=GetCredentialRequest.Builder().addCredentialOption(option).build()
    val result=CredentialManager.create(context).getCredential(context,req)
    val token=GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    handle(repo.google(token))
   }catch(e:Exception){message=e.message?: "Google Sign-In no disponible"};busy=false}},modifier=Modifier.fillMaxWidth()){Text("Continuar con Google")}
  }
  message?.let{Spacer(Modifier.height(12.dp));Text(it)}
 }
}
