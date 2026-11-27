package com.trazo.android.data.model

/** Version codes set by the server; 0 means that notice is off. */
data class AppVersionPolicy(val minimumAndroidVersionCode: Int = 0, val recommendedAndroidVersionCode: Int = 0)
