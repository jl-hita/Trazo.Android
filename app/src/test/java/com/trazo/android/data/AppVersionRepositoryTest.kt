package com.trazo.android.data

import com.trazo.android.data.api.AppVersionApi
import com.trazo.android.data.model.AppVersionPolicy
import com.trazo.android.data.repo.AppVersionRepository
import com.trazo.android.data.repo.AppVersionStore
import com.trazo.android.data.repo.UpdateNotice
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AppVersionRepositoryTest {
    private class Store(var saved: AppVersionPolicy = AppVersionPolicy()) : AppVersionStore {
        var dismissedCode = 0
        override fun policy() = saved
        override fun save(policy: AppVersionPolicy) { saved = policy }
        override fun dismissed() = dismissedCode
        override fun dismiss(versionCode: Int) { dismissedCode = versionCode }
    }

    private class Api(var answer: () -> Response<AppVersionPolicy>) : AppVersionApi {
        var calls = 0
        override suspend fun versionPolicy(): Response<AppVersionPolicy> { calls++; return answer() }
    }

    private fun answering(minimum: Int, recommended: Int) = Api { Response.success(AppVersionPolicy(minimum, recommended)) }

    @Test
    fun `nothing is shown while the policy is off`() = runBlocking {
        assertEquals(UpdateNotice.None, AppVersionRepository(answering(0, 0), Store(), 100).refresh())
    }

    @Test
    fun `an older version than the minimum has to update, the minimum itself does not`() = runBlocking {
        assertEquals(UpdateNotice.Required, AppVersionRepository(answering(101, 0), Store(), 100).refresh())
        assertEquals(UpdateNotice.None, AppVersionRepository(answering(100, 0), Store(), 100).refresh())
    }

    @Test
    fun `a suggestion is dismissed until a newer one is recommended`() = runBlocking {
        val store = Store()
        val repository = AppVersionRepository(answering(0, 110), store, 100)
        assertEquals(UpdateNotice.Recommended(110), repository.refresh())

        repository.dismiss(110)
        assertEquals(UpdateNotice.None, repository.notice())

        assertEquals(UpdateNotice.Recommended(120), AppVersionRepository(answering(0, 120), store, 100).refresh())
    }

    @Test
    fun `dismissing never hides a required update`() = runBlocking {
        val store = Store().apply { dismissedCode = 200 }

        assertEquals(UpdateNotice.Required, AppVersionRepository(answering(150, 200), store, 100).refresh())
    }

    @Test
    fun `without a connection the last answer still applies`() = runBlocking {
        val store = Store(AppVersionPolicy(minimumAndroidVersionCode = 101))
        val offline = Api { throw IOException("offline") }

        assertEquals(UpdateNotice.Required, AppVersionRepository(offline, store, 100).refresh())
        assertEquals(AppVersionPolicy(101, 0), store.saved)
    }

    @Test
    fun `an error answer changes nothing`() = runBlocking {
        val failing = Api { Response.error(404, "".toResponseBody(null)) }
        val store = Store()

        assertEquals(UpdateNotice.None, AppVersionRepository(failing, store, 100).refresh())
        assertEquals(AppVersionPolicy(), store.saved)
    }

    @Test
    fun `the server is asked once per process, and again only after a failure`() = runBlocking {
        val api = Api { throw IOException("offline") }
        val repository = AppVersionRepository(api, Store(), 100)
        repository.refresh()

        api.answer = { Response.success(AppVersionPolicy()) }
        repository.refresh()
        repository.refresh()

        assertEquals(2, api.calls)
    }

    @Test
    fun `lowering the minimum lifts the block`() = runBlocking {
        val store = Store(AppVersionPolicy(minimumAndroidVersionCode = 101))

        assertEquals(UpdateNotice.None, AppVersionRepository(answering(0, 0), store, 100).refresh())
    }
}
