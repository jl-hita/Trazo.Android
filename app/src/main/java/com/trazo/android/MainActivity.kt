package com.trazo.android
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.trazo.android.auth.*
import com.trazo.android.ui.LoginScreen
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
  val api=Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
  val repo=AuthRepository(api,SecureTokenStore(applicationContext))
  setContent { MaterialTheme(colorScheme=darkColorScheme()) { LoginScreen(repo,BuildConfig.GOOGLE_WEB_CLIENT_ID) } }
 }
}
