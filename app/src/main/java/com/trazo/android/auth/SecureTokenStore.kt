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
 fun save(access:String,refresh:String) { prefs.edit().putString("access",enc(access)).putString("refresh",enc(refresh)).apply() }
 fun access()=prefs.getString("access",null)?.let(::dec)
 fun clear()=prefs.edit().clear().apply()
 private fun enc(s:String):String { val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key()); return Base64.encodeToString(c.iv+c.doFinal(s.toByteArray()),Base64.NO_WRAP) }
 private fun dec(s:String):String { val b=Base64.decode(s,Base64.NO_WRAP); val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,b.copyOfRange(0,12))); return c.doFinal(b.copyOfRange(12,b.size)).toString(Charsets.UTF_8) }
 private fun key():SecretKey {
  val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (ks.getKey(alias,null) as? SecretKey)?.let{return it}
  val gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
  gen.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
  return gen.generateKey()
 }
}
