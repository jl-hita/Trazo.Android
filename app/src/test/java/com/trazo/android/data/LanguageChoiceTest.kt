package com.trazo.android.data

import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.api.LanguageRequest
import com.trazo.android.data.local.AppLocale
import com.trazo.android.data.local.LanguageChoice
import com.trazo.android.data.local.LanguageStore
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.LanguageRepository
import com.trazo.android.testing.httpError
import com.trazo.android.testing.problem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.Locale

/** The language is a choice of the user: kept on the device, told to the server, adopted from it on another device. */
class LanguageChoiceTest {
    private class MemoryStore(var value: String = LanguageChoice.AUTO) : LanguageStore {
        override fun choice() = LanguageChoice.normalize(value)
        override fun save(choice: String) { value = choice }
    }

    private class FakeAccountApi(var response: () -> Response<Unit> = { Response.success(Unit) }) : AccountApi {
        val sent = mutableListOf<LanguageRequest>()
        override suspend fun language(request: LanguageRequest): Response<Unit> { sent += request; return response() }
    }

    private lateinit var original: Locale
    private val store = MemoryStore()
    private val api = FakeAccountApi()
    private val repo = LanguageRepository(api, store)

    @Before fun setUp() { original = Locale.getDefault(); AppLocale.systemProvider = { Locale("de") } }
    @After fun tearDown() { Locale.setDefault(original) }

    @Test fun `unknown values are automatic`() {
        assertEquals("auto", LanguageChoice.normalize(null))
        assertEquals("auto", LanguageChoice.normalize("fr"))
        assertEquals("en", LanguageChoice.normalize("EN"))
        assertEquals("es", LanguageChoice.normalize("es"))
    }

    @Test fun `a choice is saved, put into effect and told to the server`() = runTest {
        assertTrue(repo.choose("en") is ApiResult.Success)
        assertEquals("en", store.value)
        assertEquals("en", Locale.getDefault().language)
        assertEquals("en", api.sent.single().language)

        repo.choose("es")
        assertEquals("es", Locale.getDefault().language)
    }

    @Test fun `automatic goes back to the language of the device`() = runTest {
        repo.choose("en")
        repo.choose("auto")
        assertEquals("de", Locale.getDefault().language) // the device's, not the last forced one
        assertEquals("auto", api.sent.last().language)
    }

    @Test fun `the choice stands on the device even if the server cannot be told`() = runTest {
        api.response = { httpError(500, problem(500, "server_error")) }
        assertTrue(repo.choose("en") is ApiResult.Failure)
        assertEquals("en", store.value)
        assertEquals("en", Locale.getDefault().language)
    }

    @Test fun `the choice of the server is adopted when it differs, and only then`() {
        assertFalse(repo.adopt(null)) // never chose: nothing changes
        assertEquals("auto", store.value)

        assertTrue(repo.adopt("en"))
        assertEquals("en", store.value)
        assertEquals("en", Locale.getDefault().language)

        assertFalse(repo.adopt("en")) // already the same: no rebuild loop
        assertTrue(repo.adopt("es"))
        assertEquals("es", store.value)
    }

    @Test fun `a locale is a plain language, not a region`() {
        assertEquals("es", AppLocale.localeFor("es").language)
        assertEquals("en", AppLocale.localeFor("en").language)
        assertEquals("de", AppLocale.localeFor("auto").language)
        assertEquals("de", AppLocale.localeFor("zz").language)
    }
}
