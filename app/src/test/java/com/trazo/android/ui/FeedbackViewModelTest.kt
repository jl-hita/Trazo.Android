package com.trazo.android.ui

import com.trazo.android.data.api.FeedbackApi
import com.trazo.android.data.model.FeedbackRequest
import com.trazo.android.ui.account.FeedbackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {
    private class Api : FeedbackApi {
        var offline = false
        val sent = mutableListOf<FeedbackRequest>()
        override suspend fun send(request: FeedbackRequest): Response<Unit> {
            sent += request
            if (offline) throw IOException("offline")
            return Response.success(Unit)
        }
    }

    private val api = Api()
    private var ids = 0
    private fun viewModel() = FeedbackViewModel(api, "workout/{id}", "Pixel 9 · Android 16") { "id-${++ids}" }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a comment is sent trimmed, with its context`() {
        val vm = viewModel()
        vm.category("idea")
        vm.message("  Add a calendar  ")

        vm.send()

        assertEquals(FeedbackRequest("idea", "Add a calendar", "workout/{id}", "Pixel 9 · Android 16", "id-1"), api.sent.single())
        assertTrue(vm.state.value.sent)
    }

    @Test
    fun `an empty message cannot be sent`() {
        val vm = viewModel()
        vm.message("   ")

        vm.send()

        assertFalse(vm.state.value.canSend)
        assertTrue(api.sent.isEmpty())
    }

    @Test
    fun `a failed send keeps the text, and trying again repeats the same id`() {
        val vm = viewModel()
        vm.message("It crashed")
        api.offline = true
        vm.send()
        assertNotNull(vm.state.value.error)
        assertEquals("It crashed", vm.state.value.message)

        api.offline = false
        vm.send()

        assertEquals(api.sent[0].clientRequestId, api.sent[1].clientRequestId)
        assertTrue(vm.state.value.sent)
    }

    @Test
    fun `editing after a failure makes it another comment`() {
        val vm = viewModel()
        vm.message("It crashed")
        api.offline = true
        vm.send()
        api.offline = false

        vm.message("It crashed twice")
        vm.send()

        assertNotEquals(api.sent[0].clientRequestId, api.sent[1].clientRequestId)
    }

    @Test
    fun `nothing is sent twice after success, and the text is limited`() {
        val vm = viewModel()
        vm.message("a".repeat(3000))
        assertEquals(2000, vm.state.value.message.length)

        vm.send()
        vm.send()

        assertEquals(1, api.sent.size)
    }
}
