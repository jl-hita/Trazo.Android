package com.trazo.android.data

import com.trazo.android.data.local.OwnedTokenStore
import com.trazo.android.data.local.OwnerStore
import com.trazo.android.data.local.subjectOf
import com.trazo.android.testing.MemoryTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnedTokenStoreTest {
    private class MemoryOwner(var id: String? = null) : OwnerStore {
        override fun get() = id
        override fun set(id: String?) { this.id = id }
    }

    private val tokens = MemoryTokenStore()
    private val owner = MemoryOwner()
    private var wipes = 0
    // The access token of these tests is simply the account id.
    private val store = OwnedTokenStore(tokens, owner, identify = { it }) { wipes++; owner.id = null }

    @Test fun `the same account finds its data after the session expired`() {
        store.save("ana", "r1")
        wipes = 0
        store.clear()
        store.save("ana", "r2")
        assertEquals(0, wipes)
    }

    @Test fun `another account never receives it`() {
        store.save("ana", "r1")
        wipes = 0
        store.clear()
        store.save("ben", "r2")
        assertEquals(1, wipes)
        assertEquals("ben", owner.id)
    }

    @Test fun `a session of an earlier version is adopted when renewed or when it ends`() {
        tokens.save("ana", "r1")
        store.save("ana", "r2")
        assertEquals(0, wipes)
        owner.id = null
        store.clear()
        assertEquals("ana", owner.id)
    }

    @Test fun `signed out data of unknown origin is wiped`() {
        store.save("ana", "r1")
        assertEquals(1, wipes)
    }

    @Test fun `the account is read from the token`() {
        val payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"u-1"}""".toByteArray())
        assertEquals("u-1", subjectOf("header.$payload.signature"))
        assertNull(subjectOf("not a token"))
    }
}
