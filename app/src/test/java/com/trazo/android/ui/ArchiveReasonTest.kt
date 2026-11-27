package com.trazo.android.ui

import com.google.gson.Gson
import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.api.LanguageRequest
import com.trazo.android.data.model.*
import com.trazo.android.data.repo.ArchiveReason
import com.trazo.android.data.repo.ResearchConsentRepository
import com.trazo.android.data.repo.SubscriptionRepository
import com.trazo.android.testing.FakePlansApi
import com.trazo.android.testing.FakeSubscriptionApi
import com.trazo.android.testing.httpError
import com.trazo.android.testing.networkFailure
import com.trazo.android.testing.problem
import com.trazo.android.testing.sampleWeek
import com.trazo.android.ui.account.AccountViewModel
import com.trazo.android.ui.plan.PlanViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/** Why a plan is archived is asked only with the research consent, is never required and never blocks the archive. */
class ArchiveReasonTest : ViewModelTestBase() {
    private class Account : AccountApi {
        var online = true
        var research = true
        var health = true
        var consentChanges = 0
        override suspend fun info(): Response<AccountInfo> {
            if (!online) networkFailure()
            return Response.success(AccountInfo(canDelete = true, deletion = DeletionRequirements(), healthConsent = health, researchConsent = research))
        }
        override suspend fun researchConsent(request: ResearchConsentRequest): Response<Unit> { consentChanges++; research = request.consent; return Response.success(Unit) }
        override suspend fun language(request: LanguageRequest): Response<Unit> = error("not used here")
        override suspend fun healthConsent(request: HealthConsentRequest): Response<Unit> = error("not used here")
        override suspend fun delete(request: DeleteAccountRequest) = error("not used here")
    }

    private val account = Account()
    private val consent = ResearchConsentRepository(account)
    private val plans = FakePlansApi()
    private val planId get() = sampleWeek().planId

    private fun planVm() = PlanViewModel(plansRepo(plans), consent::archiveQuestion)
    private fun accountVm() = AccountViewModel(SubscriptionRepository(FakeSubscriptionApi()), plansRepo(plans), consent::archiveQuestion)

    @Test fun `with the consent the question appears and nothing is archived yet`() = runTest(dispatcher) {
        val vm = planVm(); advanceUntilIdle()
        vm.startNextPlan(); advanceUntilIdle()
        assertEquals(ArchiveReason.entries, vm.state.value.archiveQuestion!!.reasons)
        assertTrue(plans.archived.isEmpty())
    }

    @Test fun `without the consent the plan is archived at once and without a reason`() = runTest(dispatcher) {
        account.research = false
        val vm = planVm(); advanceUntilIdle()
        vm.startNextPlan(); advanceUntilIdle()
        assertNull(vm.state.value.archiveQuestion)
        assertEquals(listOf(ArchivePlanRequest(planId = planId)), plans.archived)
    }

    @Test fun `when the consent cannot be read the plan is archived without asking`() = runTest(dispatcher) {
        account.online = false
        val vm = planVm(); advanceUntilIdle()
        vm.startNextPlan(); advanceUntilIdle()
        assertEquals(listOf(ArchivePlanRequest(planId = planId)), plans.archived)
    }

    @Test fun `closing the question archives nothing`() = runTest(dispatcher) {
        val vm = planVm(); advanceUntilIdle()
        vm.startNextPlan(); advanceUntilIdle()
        vm.cancelArchive()
        // An answer that arrives after closing is ignored.
        vm.archiveBecause(ArchiveReason.NoTime); advanceUntilIdle()
        assertNull(vm.state.value.archiveQuestion)
        assertFalse(vm.state.value.archiving)
        assertTrue(plans.archived.isEmpty())
    }

    @Test fun `the chosen reason is sent once with the plan on screen`() = runTest(dispatcher) {
        val vm = planVm(); advanceUntilIdle()
        vm.startNextPlan(); vm.startNextPlan(); advanceUntilIdle()
        vm.archiveBecause(ArchiveReason.TooDemanding); vm.archiveBecause(ArchiveReason.TooEasy); advanceUntilIdle()
        assertEquals(listOf(ArchivePlanRequest("TooDemanding", planId)), plans.archived)
        assertFalse(vm.state.value.archiving)
    }

    @Test fun `preferring not to say archives without a reason`() = runTest(dispatcher) {
        val vm = planVm(); advanceUntilIdle()
        vm.startNextPlan(); advanceUntilIdle()
        vm.archiveBecause(null); advanceUntilIdle()
        assertEquals(listOf(ArchivePlanRequest(planId = planId)), plans.archived)
    }

    @Test fun `discomfort is only offered with the health data consent`() = runTest(dispatcher) {
        account.health = false
        val reasons = consent.archiveQuestion()!!.reasons
        assertFalse(ArchiveReason.Discomfort in reasons)
        assertEquals(ArchiveReason.entries.size - 1, reasons.size)
    }

    @Test fun `the account screen asks too, and sends the reason with the plan it showed`() = runTest(dispatcher) {
        val vm = accountVm(); advanceUntilIdle()
        vm.archivePlan("p1"); advanceUntilIdle()
        assertNotNull(vm.state.value.archiveQuestion)
        assertTrue(plans.archived.isEmpty())
        vm.archiveBecause(ArchiveReason.GoalsChanged); advanceUntilIdle()
        assertEquals(listOf(ArchivePlanRequest("GoalsChanged", "p1")), plans.archived)
        assertTrue(vm.state.value.archived)
    }

    @Test fun `the account screen archives directly without the consent and can cancel the question`() = runTest(dispatcher) {
        val vm = accountVm(); advanceUntilIdle()
        vm.archivePlan("p1"); advanceUntilIdle()
        vm.cancelArchive()
        assertTrue(plans.archived.isEmpty())
        assertFalse(vm.state.value.archived)
        account.research = false
        vm.archivePlan("p1"); advanceUntilIdle()
        assertEquals(listOf(ArchivePlanRequest(planId = "p1")), plans.archived)
        assertTrue(vm.state.value.archived)
    }

    @Test fun `a refused archive is shown and can be tried again`() = runTest(dispatcher) {
        account.research = false
        plans.archiveResponse = { httpError(409, problem(409, "concurrent_modification")) }
        val vm = accountVm(); advanceUntilIdle()
        vm.archivePlan("p1"); advanceUntilIdle()
        assertFalse(vm.state.value.archived)
        assertFalse(vm.state.value.archiving)
        assertNotNull(vm.state.value.archiveError)
    }

    @Test fun `asking about the plan and updating the account leaves the consent as it was`() = runTest(dispatcher) {
        val vm = accountVm(); advanceUntilIdle()
        vm.archivePlan("p1"); advanceUntilIdle()
        vm.archiveBecause(ArchiveReason.Other); advanceUntilIdle()
        vm.load(); advanceUntilIdle()
        assertTrue(account.research)
        assertEquals(0, account.consentChanges)
        assertNotNull(consent.archiveQuestion())
    }

    @Test fun `what is unknown or not answered is left out of the request`() {
        assertEquals("{}", Gson().toJson(ArchivePlanRequest()))
        assertEquals("""{"reason":"NoTime","planId":"p1"}""", Gson().toJson(ArchivePlanRequest("NoTime", "p1")))
    }
}
