package com.trazo.android

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * The signed-in app against the mock API of `tools/mock-api` (start `server.py --lang en` and build with
 * `-PapiBaseUrl=http://10.0.2.2:8099/`). Skipped when the build points anywhere else, so it never signs in on a real server.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
class SignedInScreensTest {
    @get:Rule val app = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = app.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) =
        app.waitUntil(20_000) { app.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun tab(id: Int) = app.onNode(hasText(text(id)) and isSelectable())

    @After fun signOut() {
        val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TrazoApp).container
        container.tokens.clear()
        container.clearAccountData()
    }

    @Test fun theMainTabsOpenAndPassTheAccessibilityChecks() {
        val api = URI(BuildConfig.API_BASE_URL)
        assumeTrue("Needs the mock API", api.host == "10.0.2.2" && reachable(api))

        waitFor(hasText(text(R.string.login_email)))
        app.onNode(hasText(text(R.string.login_email)) and hasSetTextAction()).performScrollTo().performTextInput("demo@example.com")
        app.onNode(hasText(text(R.string.login_password)) and hasSetTextAction()).performScrollTo().performTextInput("any-password-here")
        app.onNode(hasText(text(R.string.login_sign_in)) and hasClickAction()).performScrollTo().performClick()
        waitFor(hasText(text(R.string.nav_plan)) and isSelectable())

        app.enableAccessibilityChecks()
        // The catalogue is left out: the mock forwards it to the real server.
        for (screen in listOf(R.string.nav_plan, R.string.nav_progress, R.string.nav_account)) {
            tab(screen).performClick()
            app.waitForIdle()
            waitForLoading()
            app.onRoot().tryPerformAccessibilityChecks()
        }
    }

    private fun waitForLoading() = app.waitUntil(20_000) {
        app.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            .fetchSemanticsNodes().isEmpty()
    }

    private fun reachable(api: URI) = runCatching {
        Socket().use { it.connect(InetSocketAddress(api.host, api.port), 2_000) }
    }.isSuccess
}
