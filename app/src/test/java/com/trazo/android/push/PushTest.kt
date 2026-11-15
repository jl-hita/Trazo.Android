package com.trazo.android.push

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/** The push contract of docs/push-notifications.md, as the server writes it, and the rules for what the app shows. */
class PushTest {
    private val gson = Gson()

    @Test fun `config of a server with push on is usable`() {
        val json = """{"enabled":true,"projectId":"trazo-app","applicationId":"1:123456789012:android:0123456789abcdef","apiKey":"AIzaKey","senderId":"123456789012"}"""
        val config = gson.fromJson(json, PushClientConfig::class.java)
        assertTrue(config.usable)
        assertEquals("trazo-app", config.projectId)
    }

    @Test fun `config of a server with push off is not usable`() {
        val config = gson.fromJson("""{"enabled":false,"projectId":null,"applicationId":null,"apiKey":null,"senderId":null}""", PushClientConfig::class.java)
        assertFalse(config.usable)
    }

    @Test fun `a config missing any required value is not usable even when enabled`() {
        assertFalse(PushClientConfig(true, "p", "", "key").usable)
        assertFalse(PushClientConfig(true, null, "app", "key").usable)
        assertFalse(PushClientConfig(true, "p", "app", " ").usable)
        assertTrue(PushClientConfig(true, "p", "app", "key").usable)
    }

    @Test fun `known kinds use the app's own text and unknown ones the server's`() {
        for (kind in listOf("review-reply", "review-closed", "review-new", "review-message", "test")) assertNotNull(kind, PushKinds.bodyRes(kind))
        assertNull(PushKinds.bodyRes("announcement"))
        assertNull(PushKinds.bodyRes("something-new"))
        assertNull(PushKinds.bodyRes(null))
    }

    @Test fun `only review notifications open the account screen`() {
        assertTrue(PushKinds.opensAccount("review-reply"))
        assertTrue(PushKinds.opensAccount("review-closed"))
        assertFalse(PushKinds.opensAccount("announcement"))
        assertFalse(PushKinds.opensAccount(null))
    }

    @Test fun `text from the network is bounded and stripped of control characters`() {
        assertEquals("Hola", PushKinds.clean("  Ho\u0000la\u0007 ", 65))
        assertEquals("abc", PushKinds.clean("abcdef", 3))
        assertEquals("", PushKinds.clean(null, 65))
    }
}
