package com.trazo.android.auth
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
class SecureTokenStore(context:Context) {
 private val prefs=context.getSharedPreferences("trazo_session",Context.MODE_PRIVATE)
 private val alias="trazo.session.aes.v1"
 fun save(access:String,refresh:String) { prefs.edit().putString("access",encrypt(access)).putString("refresh",encrypt(refresh)).apply() }
 fun access():String?=prefs.getString("access",null)?.let(::decrypt)
 fun refresh():String?=prefs.getString("refresh",null)?.let(::decrypt)
 fun clear(){prefs.edit().clear().apply()}
 private fun encrypt(value:String):String { val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());return Base64.encodeToString(cipher.iv+cipher.doFinal(value.toByteArray()),Base64.NO_WRAP) }
 private fun decrypt(value:String):String { val bytes=Base64.decode(value,Base64.NO_WRAP);require(bytes.size>12);val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)));return cipher.doFinal(bytes.copyOfRange(12,bytes.size)).toString(Charsets.UTF_8) }
 private fun key():SecretKey {
  val keyStore=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (keyStore.getKey(alias,null) as? SecretKey)?.let{return it}
  val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
  generator.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
  return generator.generateKey()
 }
}
