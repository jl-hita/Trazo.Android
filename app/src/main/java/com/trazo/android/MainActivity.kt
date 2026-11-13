package com.trazo.android
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trazo.android.auth.*
import com.trazo.android.ui.LoginScreen
import com.trazo.android.ui.theme.TrazoTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class MainActivity:ComponentActivity() {
 private var actionUri by mutableStateOf<Uri?>(null)
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  actionUri=intent?.data
  val baseUrl=BuildConfig.API_BASE_URL
  require(if (BuildConfig.DEBUG) baseUrl.startsWith("https://") || baseUrl.startsWith("http://") else baseUrl.startsWith("https://")) {
   "Las compilaciones release requieren HTTPS; HTTP solo está permitido en debug."
  }
  val api=Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
  val repository=AuthRepository(api,SecureTokenStore(applicationContext))
  setContent { TrazoTheme {
   val uri=actionUri
   val path=uri?.path.orEmpty()
   LoginScreen(repository,BuildConfig.GOOGLE_WEB_CLIENT_ID,
    actionType=when{path.endsWith("verify-email")->"verify-email";path.endsWith("reset-password")->"reset-password";else->null},
    actionToken=uri?.getQueryParameter("token"))
  } }
 }
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);actionUri=intent.data}
}
