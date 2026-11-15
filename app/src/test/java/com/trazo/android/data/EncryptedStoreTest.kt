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
