package com.trazo.android.ui

import com.google.gson.Gson
import com.trazo.android.auth.AuthRepository
import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.api.LanguageRequest
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.repo.AccountRepository
import com.trazo.android.data.repo.SubscriptionRepository
import com.trazo.android.testing.*
import com.trazo.android.ui.account.DeleteAccountViewModel
import com.trazo.android.ui.common.legalUrls
import com.trazo.android.ui.nav.AppState
import com.trazo.android.ui.nav.AppViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

private val passwordAccount = AccountInfo(canDelete = true, deletion = DeletionRequirements(requiresPassword = true))

private class FakeDeletionApi(
    var infoResponse: () -> Response<AccountInfo> = { Response.success(passwordAccount) },
    var deleteResponse: () -> Response<DeleteAccountResult> = { Response.success(DeleteAccountResult(deleted = true)) }
) : AccountApi {
    val sent = mutableListOf<DeleteAccountRequest>()
    override suspend fun language(request: LanguageRequest): Response<Unit> = Response.success(Unit)
    override suspend fun info() = infoResponse()
    override suspend fun delete(request: DeleteAccountRequest): Response<DeleteAccountResult> { sent += request; return deleteResponse() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountTest : ViewModelTestBase() {
    private val api = FakeDeletionApi()
    private fun viewModel() = DeleteAccountViewModel(AccountRepository(api))

    @Test fun `the requirements of the account are read from the server answer`() {
        val json = """{"id":"1","email":"a@b.c","canDelete":true,"cannotDeleteReason":null,"deletion":{"requiresPassword":false,
            "requiresGoogleIdToken":true,"requiresTwoFactorCode":true,"playSubscriptionNotCancelled":true,"warning":"cancel it",
            "manageSubscriptionUrl":"https://play.google.com/store/account/subscriptions"}}"""
        val info = Gson().fromJson(json, AccountInfo::class.java)
        assertTrue(info.deletion.requiresGoogleIdToken)
        assertTrue(info.deletion.requiresTwoFactorCode)
        assertEquals("cancel it", info.deletion.warning)
        assertNotNull(info.deletion.manageSubscriptionUrl)
    }

    @Test fun `the password and the code are sent and the deletion is reported`() = runTest(dispatcher) {
        val vm = viewModel(); advanceUntilIdle()
        assertSame(passwordAccount, vm.state.value.info)
        vm.delete(password = " secret ", code = " 123456 "); advanceUntilIdle()
        // A password is sent as typed; the code without the spaces around it.
        assertEquals(DeleteAccountRequest(password = " secret ", code = "123456"), api.sent.single())
        assertTrue(vm.state.value.done!!.deleted)
    }

    @Test fun `a refused password keeps the account and lets the person try again`() = runTest(dispatcher) {
        api.deleteResponse = { httpError(403, problem(403, "reauthentication_failed")) }
        val vm = viewModel(); advanceUntilIdle()
        vm.delete(password = "wrong"); advanceUntilIdle()
        assertTrue(vm.state.value.rejected)
        assertNull(vm.state.value.done)
        assertNull(vm.state.value.deleteError)
        assertFalse(vm.state.value.deleting)

        api.deleteResponse = { Response.success(DeleteAccountResult(deleted = true)) }
        vm.delete(password = "right"); advanceUntilIdle()
        assertFalse(vm.state.value.rejected)
        assertNotNull(vm.state.value.done)
    }

    @Test fun `any other failure is shown as an error`() = runTest(dispatcher) {
        api.deleteResponse = { networkFailure() }
        val vm = viewModel(); advanceUntilIdle()
        vm.delete(idToken = "token"); advanceUntilIdle()
        assertEquals(ErrorKind.Network, vm.state.value.deleteError!!.kind)
        assertFalse(vm.state.value.rejected)
        assertNull(vm.state.value.done)
    }

    @Test fun `a second tap does not send a second request`() = runTest(dispatcher) {
        val vm = viewModel(); advanceUntilIdle()
        vm.delete(password = "secret"); vm.delete(password = "secret"); advanceUntilIdle()
        vm.delete(password = "secret"); advanceUntilIdle()
        assertEquals(1, api.sent.size)
    }

    @Test fun `without the account information it offers to retry`() = runTest(dispatcher) {
        api.infoResponse = { networkFailure() }
        val vm = viewModel(); advanceUntilIdle()
        assertNotNull(vm.state.value.loadError)
        assertNull(vm.state.value.info)
        api.infoResponse = { Response.success(passwordAccount) }
        vm.load(); advanceUntilIdle()
        assertNull(vm.state.value.loadError)
        assertNotNull(vm.state.value.info)
    }

    @Test fun `after deleting the phone is cleaned without signing out on the server`() = runTest(dispatcher) {
        var signedOut = false
        var forgotten = false
        val app = AppViewModel(
            AuthRepository(FakeAuthApi(), MemoryTokenStore("a", "r")), SubscriptionRepository(FakeSubscriptionApi()), SessionEvents(),
            { signedOut = true }, {}, dispatcher, forgetAccount = { forgotten = true }
        )
        advanceUntilIdle()
        app.onAccountDeleted()
        assertEquals(AppState.SignedOut, app.state.value)
        assertTrue(forgotten)
        assertFalse(signedOut)
    }

    @Test fun `legal pages are Spanish or English`() {
        assertEquals("https://example.test/privacidad", legalUrls("example.test", "es").privacy)
        assertEquals("https://example.test/condiciones", legalUrls("example.test", "es").terms)
        assertEquals("https://example.test/privacy-policy", legalUrls("example.test", "de").privacy)
        assertEquals("https://example.test/terms", legalUrls("example.test", "ca").terms)
    }
}
