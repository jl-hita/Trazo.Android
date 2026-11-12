package com.trazo.android
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trazo.android.auth.*
import com.trazo.android.ui.LoginScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class MainActivity:ComponentActivity() {
 private var actionUri by mutableStateOf<Uri?>(null)
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  actionUri=intent?.data
  val baseUrl=BuildConfig.API_BASE_URL
  require(baseUrl.startsWith("https://")||baseUrl.startsWith("http://10.0.2.2:")||baseUrl.startsWith("http://192.168.")||baseUrl.startsWith("http://127.0.0.1:")) {
   "API_BASE_URL debe ser HTTPS o una dirección local explícita de desarrollo."
  }
  val api=Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
  val repository=AuthRepository(api,SecureTokenStore(applicationContext))
  setContent { MaterialTheme(colorScheme=darkColorScheme()) {
   val uri=actionUri
   val path=uri?.path.orEmpty()
   LoginScreen(repository,BuildConfig.GOOGLE_WEB_CLIENT_ID,
    actionType=when{path.endsWith("verify-email")->"verify-email";path.endsWith("reset-password")->"reset-password";else->null},
    actionToken=uri?.getQueryParameter("token"))
  } }
 }
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);actionUri=intent.data}
}
