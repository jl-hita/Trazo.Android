package com.trazo.android.ui.nav

import android.Manifest
import android.widget.Toast
import com.trazo.android.diagnostics.guarded
import com.trazo.android.diagnostics.Notices
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.trazo.android.push.PushKinds
import com.trazo.android.push.PushNotifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trazo.android.AppContainer
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.TrazoApp
import com.trazo.android.auth.UserSummary
import com.trazo.android.data.model.SubscriptionStatus
import com.trazo.android.ui.billing.PremiumRequiredDialog
import com.trazo.android.ui.LoginScreen
import com.trazo.android.ui.account.AccountScreen
import com.trazo.android.ui.account.AccountViewModel
import com.trazo.android.ui.account.PaywallViewModel
import com.trazo.android.ui.billing.PaywallScreen
import com.trazo.android.ui.catalog.CatalogScreen
import com.trazo.android.ui.catalog.CatalogViewModel
import com.trazo.android.ui.catalog.ExerciseDetailScreen
import com.trazo.android.ui.catalog.ExerciseDetailViewModel
import com.trazo.android.ui.common.LoadingBox
import com.trazo.android.ui.onboarding.OnboardingScreen
import com.trazo.android.ui.onboarding.OnboardingViewModel
import com.trazo.android.ui.plan.*
import com.trazo.android.ui.manual.ManualPlanScreen
import com.trazo.android.ui.manual.ManualPlanViewModel
import com.trazo.android.ui.theme.TrazoPalette
import com.trazo.android.ui.workout.WorkoutScreen
import com.trazo.android.ui.workout.WorkoutViewModel

object Routes {
    const val PLAN = "plan"
    const val CATALOG = "catalog"
    const val ACCOUNT = "account"
    const val ONBOARDING = "onboarding"
    const val MANUAL_PLAN = "manual-plan"
    const val MANUAL_EDIT = "manual-plan/edit"
    const val PREMIUM = "premium?source={source}"
    const val OVERVIEW = "overview"
    const val HISTORY = "history"
    const val ADJUSTMENTS = "adjustments"
    const val BLOCK = "block"
    const val PROGRESS = "progress"
    const val REFERRALS = "referrals"
    const val SESSION = "session/{id}"
    const val WORKOUT = "workout/{id}"
    const val EXERCISE = "exercise/{id}"
    /** [source] says where the offer was opened from (account, plan, session, onboarding); it only feeds an anonymous counter. */
    fun premium(source: String) = "premium?source=$source"
    fun session(id: String) = "session/$id"
    fun workout(id: String) = "workout/$id"
    fun exercise(id: String) = "exercise/$id"
}

@Composable
private fun container(): AppContainer = (LocalContext.current.applicationContext as TrazoApp).container

@Composable
private inline fun <reified VM : ViewModel> containerViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val c = container()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(c) } })
}

/** Top of the UI: chooses between loading, sign-in and the app. Premium is an optional screen inside the app. */
@Composable
fun AppRoot(actionType: String?, actionToken: String?, pushKind: String? = null, onPushHandled: () -> Unit = {}) {
    val c = container()
    val app: AppViewModel = containerViewModel { AppViewModel(it.auth, it.subscription, it.sessionEvents, it::signOut, it.plans::clearLocal) }
    val state by app.state.collectAsStateWithLifecycle()
    // A failure the app survived says which function is not available (and has already been reported as a ticket).
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Notices.events.collect { feature ->
            val text = if (feature == Notices.GENERIC) context.getString(R.string.notice_unavailable_generic)
            else context.getString(R.string.notice_unavailable, context.getString(feature))
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
    Surface(Modifier.fillMaxSize(), color = TrazoPalette.Background, contentColor = TrazoPalette.Text) {
        when (val s = state) {
            AppState.Loading -> LoadingBox()
            AppState.SignedOut -> LoginScreen(c.auth, BuildConfig.GOOGLE_WEB_CLIENT_ID, actionType, actionToken, onSignedIn = app::onSignedIn)
            is AppState.Ready -> MainNavigation(s.user, s.status, onLogout = app::signOut, pushKind = pushKind, onPushHandled = onPushHandled)
        }
    }
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.PLAN, R.string.nav_plan, Icons.Filled.Home),
    Tab(Routes.PROGRESS, R.string.nav_progress, Icons.Filled.Star),
    Tab(Routes.CATALOG, R.string.nav_catalog, Icons.Filled.Search),
    Tab(Routes.ACCOUNT, R.string.nav_account, Icons.Filled.Person)
)

@Composable
private fun MainNavigation(user: UserSummary?, status: SubscriptionStatus?, onLogout: () -> Unit, pushKind: String? = null, onPushHandled: () -> Unit = {}) {
    val c = container()
    val nav = rememberNavController()
    val planVm: PlanViewModel = containerViewModel { PlanViewModel(it.plans) }
    // Bumped when a purchase is confirmed, so the account tab asks the backend again instead of showing the state it loaded earlier.
    var subscriptionVersion by rememberSaveable { mutableIntStateOf(0) }
    // What the backend said about the account when the app opened, refreshed when a purchase is confirmed. Null (unknown, e.g.
    // offline start) never blocks anything: the server has the last word and the onboarding handles its answer.
    var access by remember(status) { mutableStateOf(status) }
    // A free account that taps "automatic plan" is told what it can do instead of filling in the form to get a refusal.
    var premiumPrompt by rememberSaveable { mutableStateOf(false) }
    val route =nav.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    // Signing in on another device: the language the user chose there is adopted here, and the screen is rebuilt in it.
    LaunchedEffect(user?.language) {
        if (c.language.adopt(user?.language)) {
            planVm.load()
            (context as? android.app.Activity)?.recreate()
        }
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // Push is an extra: register this phone once the user is in, and ask for the notification permission (once) only if the server has push on.
    LaunchedEffect(Unit) {
        guarded(R.string.feature_push) {
            val enabled = c.push.register()
            if (enabled && Build.VERSION.SDK_INT >= 33 && !c.pushConfig.permissionAsked && !PushNotifier.canPost(context)) {
                c.pushConfig.permissionAsked = true
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // Tapping a review notification opens the account screen, where the conversation is.
    LaunchedEffect(pushKind) {
        if (pushKind == null) return@LaunchedEffect
        guarded(R.string.feature_push) { if (PushKinds.opensAccount(pushKind)) nav.navigateTab(Routes.ACCOUNT) }
        onPushHandled()
    }
    if (premiumPrompt) PremiumRequiredDialog(
        onPremium = { premiumPrompt = false; nav.navigate(Routes.premium("onboarding")) },
        onManualPlan = { premiumPrompt = false; nav.navigate(Routes.MANUAL_PLAN) },
        onDismiss = { premiumPrompt = false }
    )
    Scaffold(
        containerColor = TrazoPalette.Background,
        bottomBar = {
            if (route in TABS.map { it.route }) NavigationBar(containerColor = TrazoPalette.Sidebar) {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { nav.navigateTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TrazoPalette.Text, selectedTextColor = TrazoPalette.Text,
                            unselectedIconColor = TrazoPalette.TextSecondary, unselectedTextColor = TrazoPalette.TextSecondary,
                            indicatorColor = TrazoPalette.SecondaryButton
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(nav, startDestination = Routes.PLAN) {
                composable(Routes.PLAN) {
                    val state by planVm.state.collectAsStateWithLifecycle()
                    // The week may have changed (a workout was logged, a plan was created): refresh when coming back.
                    LaunchedEffect(Unit) { if (!state.loading && state.week != null) planVm.load() }
                    PlanScreen(
                        planVm,
                        onOpenSession = { nav.navigate(Routes.session(it)) },
                        onOverview = { nav.navigate(Routes.OVERVIEW) },
                        onHistory = { nav.navigate(Routes.HISTORY) },
                        onAdjustments = { nav.navigate(Routes.ADJUSTMENTS) },
                        onBlock = { nav.navigate(Routes.BLOCK) },
                        onEditPlan = { nav.navigate(Routes.MANUAL_EDIT) },
                        onCreatePlan = { if (access?.canUpgrade == true) premiumPrompt = true else nav.navigate(Routes.ONBOARDING) },
                        onCreateManualPlan = { nav.navigate(Routes.MANUAL_PLAN) },
                        onPremium = { nav.navigate(Routes.premium("plan")) },
                        onSendPending = { planVm.sendPending() },
                        autoNeedsPremium = access?.canUpgrade == true
                    )
                }
                composable(Routes.CATALOG) {
                    val vm: CatalogViewModel = containerViewModel { CatalogViewModel(it.exercises) }
                    CatalogScreen(vm, onOpen = { nav.navigate(Routes.exercise(it)) })
                }
                composable(Routes.ACCOUNT) {
                    val vm: AccountViewModel = containerViewModel { AccountViewModel(it.subscription, it.plans) }
                    val planState by planVm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(subscriptionVersion) { if (subscriptionVersion > 0) vm.load() }
                    AccountScreen(vm, c.auth, c.language, user, hasPlan = planState.week != null || planState.plan != null, generatedPlan = planState.plan?.origin == "Generated", onLogout = onLogout, onPremium = { nav.navigate(Routes.premium("account")) }, onPlanArchived = { planVm.load(); nav.navigateTab(Routes.PLAN) }, onLanguageChanged = { planVm.load() }, onReferrals = { nav.navigate(Routes.REFERRALS) })
                }
                composable(Routes.REFERRALS) {
                    val vm: com.trazo.android.ui.referral.ReferralViewModel = containerViewModel { com.trazo.android.ui.referral.ReferralViewModel(it.referrals) }
                    com.trazo.android.ui.referral.ReferralScreen(vm, onBack = { nav.popBackStack() })
                }
                composable(Routes.ONBOARDING) {
                    val milestone = rememberCoroutineScope()
                    val vm: OnboardingViewModel = containerViewModel { OnboardingViewModel(it.plans) }
                    OnboardingScreen(vm, onBack = { nav.popBackStack() }, onPremium = { nav.navigate(Routes.premium("onboarding")) }, onManualPlan = { nav.popBackStack(); nav.navigate(Routes.MANUAL_PLAN) }, onCreated = { milestone.launch { c.referrals.onboardingCompleted() }; planVm.load(); nav.popBackStack() })
                }
                composable(Routes.MANUAL_PLAN) {
                    val vm: ManualPlanViewModel = containerViewModel { ManualPlanViewModel(it.plans, it.exercises) }
                    ManualPlanScreen(vm, onBack = { nav.popBackStack() }, onCreated = { planVm.load(); nav.popBackStack() })
                }
                composable(Routes.MANUAL_EDIT) {
                    val vm: ManualPlanViewModel = containerViewModel(key = "manual-edit") { ManualPlanViewModel(it.plans, it.exercises, editing = true) }
                    ManualPlanScreen(vm, onBack = { nav.popBackStack() }, onCreated = { planVm.load(); nav.popBackStack() })
                }
                composable(Routes.PREMIUM, listOf(navArgument("source") { type = NavType.StringType; defaultValue = "other" })) { entry ->
                    val source = entry.arguments?.getString("source").orEmpty()
                    var counted by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) { if (!counted) { counted = true; guarded(R.string.feature_analytics) { c.analytics.paywallViewed(source) } } }
                    val vm: PaywallViewModel = containerViewModel { PaywallViewModel(it.billing, user?.id, onAccess = { access = it; subscriptionVersion++; planVm.load(); nav.popBackStack() }) }
                    PaywallScreen(vm, onClose = { nav.popBackStack() })
                }
                composable(Routes.OVERVIEW) {
                    val loadVm: LoadViewModel = containerViewModel { LoadViewModel(it.plans) }
                    PlanOverviewScreen(planVm, loadVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.BLOCK) {
                    val vm: BlockViewModel = containerViewModel { BlockViewModel(it.plans) }
                    BlockScreen(vm, onBack = { nav.popBackStack() })
                }
                composable(Routes.PROGRESS) {
                    val vm: com.trazo.android.ui.progress.ProgressViewModel = containerViewModel { com.trazo.android.ui.progress.ProgressViewModel(it.plans) }
                    com.trazo.android.ui.progress.ProgressScreen(vm)
                }
                composable(Routes.ADJUSTMENTS) {
                    val vm: AdaptationsViewModel = containerViewModel { AdaptationsViewModel(it.plans) }
                    AdaptationsScreen(vm, onBack = { nav.popBackStack() })
                }
                composable(Routes.HISTORY) {
                    val vm: HistoryViewModel = containerViewModel { HistoryViewModel(it.plans) }
                    HistoryScreen(vm, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.session(it)) })
                }
                composable(Routes.SESSION, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val vm: SessionViewModel = containerViewModel(key = "session-$id") { SessionViewModel(it.plans, it.exercises, id) }
                    SessionScreen(vm, c.plans.week, id, lookup = c.plans::session, onBack = { nav.popBackStack() }, onPremium = { nav.navigate(Routes.premium("session")) }, onStart = { nav.navigate(Routes.workout(id)) }, onExercise = { nav.navigate(Routes.exercise(it)) })
                }
                composable(Routes.WORKOUT, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val vm: WorkoutViewModel = containerViewModel(key = "workout-$id") { WorkoutViewModel(it.plans, it.drafts, id, exercises = it.exercises) }
                    WorkoutScreen(vm, onExit = { planVm.load(); nav.popBackStack(Routes.PLAN, inclusive = false) }, onExercise = { nav.navigate(Routes.exercise(it)) })
                }
                composable(Routes.EXERCISE, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val vm: ExerciseDetailViewModel = containerViewModel(key = "exercise-$id") { ExerciseDetailViewModel(it.exercises, id) }
                    ExerciseDetailScreen(vm, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(Routes.PLAN) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
