package com.trazo.android.data

import com.trazo.android.data.local.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptedStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val key = newKey()
    private fun cipher(k: SecretKey = key) = AesGcmCipher { k }
    private data class Note(val text: String)

    @Test fun `an encrypted file does not contain the plain text and reads back`() {
        val store = JsonFileStore(tmp.root, cipher = cipher())
        store.write("a.json", Note("press de banca 80 kg"))
        val raw = File(tmp.root, "a.json").readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(raw.contains("banca"))
        assertEquals("press de banca 80 kg", store.read<Note>("a.json", Note::class.java)!!.text)
    }

    @Test fun `each write uses a new IV`() {
        val store = JsonFileStore(tmp.root, cipher = cipher())
        store.write("a.json", Note("x"))
        val first = File(tmp.root, "a.json").readBytes()
        store.write("a.json", Note("x"))
        assertFalse(first.contentEquals(File(tmp.root, "a.json").readBytes()))
    }

    @Test fun `a plain file from an older version is read and encrypted on the next write`() {
        File(tmp.root, "a.json").writeText("""{"text":"antiguo"}""")
        val store = JsonFileStore(tmp.root, cipher = cipher())
        assertEquals("antiguo", store.read<Note>("a.json", Note::class.java)!!.text)
        store.write("a.json", Note("nuevo"))
        assertFalse(File(tmp.root, "a.json").readText(Charsets.ISO_8859_1).contains("nuevo"))
    }

    @Test fun `a file that cannot be decrypted is dropped instead of blocking`() {
        JsonFileStore(tmp.root, cipher = cipher()).write("a.json", Note("secreto"))
        val otherKey = JsonFileStore(tmp.root, cipher = cipher(newKey()))
        assertNull(otherKey.read<Note>("a.json", Note::class.java))
        assertFalse(File(tmp.root, "a.json").exists())
    }

    @Test fun `the queue survives a passing decryption failure, with the logs added meanwhile`() {
        var broken = false
        val real = cipher()
        val flaky = object : LocalCipher {
            override fun encrypt(plain: ByteArray) = real.encrypt(plain)
            override fun decrypt(sealed: ByteArray) = if (broken) throw IllegalStateException("keystore busy") else real.decrypt(sealed)
        }
        val queue = PendingLogQueue(JsonFileStore(tmp.root, cipher = flaky, keepUnreadable = true))
        queue.add(PendingLog("s1", com.trazo.android.testing.logRequest(), 1))
        broken = true
        assertTrue(queue.all().isEmpty())
        assertTrue(File(tmp.root, "pending_logs.json").exists())
        queue.add(PendingLog("s2", com.trazo.android.testing.logRequest(), 2))
        broken = false
        assertEquals(setOf("s1", "s2"), queue.all().map { it.sessionId }.toSet())
        assertEquals(listOf("pending_logs.json"), tmp.root.list()!!.toList())
    }

    @Test fun `clearing the queue also removes what was set aside`() {
        PendingLogQueue(JsonFileStore(tmp.root, cipher = cipher(), keepUnreadable = true)).add(PendingLog("s1", com.trazo.android.testing.logRequest(), 1))
        val other = PendingLogQueue(JsonFileStore(tmp.root, cipher = cipher(newKey()), keepUnreadable = true))
        other.add(PendingLog("s2", com.trazo.android.testing.logRequest(), 2))
        assertEquals(2, tmp.root.list()!!.size)
        other.clear()
        assertEquals(0, tmp.root.list()!!.size)
    }

    @Test fun `tampered data is rejected`() {
        val store = JsonFileStore(tmp.root, cipher = cipher())
        store.write("a.json", Note("secreto"))
        val file = File(tmp.root, "a.json")
        val bytes = file.readBytes().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        file.writeBytes(bytes)
        assertNull(store.read<Note>("a.json", Note::class.java))
    }

    @Test fun `an encrypted file is not read by a store without a cipher`() {
        JsonFileStore(tmp.root, cipher = cipher()).write("a.json", Note("secreto"))
        assertNull(JsonFileStore(tmp.root).read<Note>("a.json", Note::class.java))
    }
}
