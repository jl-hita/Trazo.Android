package com.trazo.android.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trazo.android.AppContainer
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.TrazoApp
import com.trazo.android.auth.UserSummary
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
import com.trazo.android.ui.theme.TrazoPalette
import com.trazo.android.ui.workout.WorkoutScreen
import com.trazo.android.ui.workout.WorkoutViewModel

object Routes {
    const val PLAN = "plan"
    const val CATALOG = "catalog"
    const val ACCOUNT = "account"
    const val ONBOARDING = "onboarding"
    const val OVERVIEW = "overview"
    const val HISTORY = "history"
    const val SESSION = "session/{id}"
    const val WORKOUT = "workout/{id}"
    const val EXERCISE = "exercise/{id}"
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

/** Top of the UI: chooses between loading, sign-in, the plans screen (only if the backend requires payment) and the app. */
@Composable
fun AppRoot(actionType: String?, actionToken: String?) {
    val c = container()
    val app: AppViewModel = containerViewModel { AppViewModel(it.auth, it.subscription, it.sessionEvents, it::signOut, it.plans::clearLocal) }
    val state by app.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = TrazoPalette.Background, contentColor = TrazoPalette.Text) {
        when (val s = state) {
            AppState.Loading -> LoadingBox()
            AppState.SignedOut -> LoginScreen(c.auth, BuildConfig.GOOGLE_WEB_CLIENT_ID, actionType, actionToken, onSignedIn = app::onSignedIn)
            is AppState.Paywall -> {
                val paywall: PaywallViewModel = containerViewModel { PaywallViewModel(it.billing, s.user?.id, app::onAccessGranted) }
                PaywallScreen(paywall, onLogout = app::signOut)
            }
            is AppState.Ready -> MainNavigation(s.user, onLogout = app::signOut)
        }
    }
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.PLAN, R.string.nav_plan, Icons.Filled.Home),
    Tab(Routes.CATALOG, R.string.nav_catalog, Icons.Filled.Search),
    Tab(Routes.ACCOUNT, R.string.nav_account, Icons.Filled.Person)
)

@Composable
private fun MainNavigation(user: UserSummary?, onLogout: () -> Unit) {
    val c = container()
    val nav = rememberNavController()
    val planVm: PlanViewModel = containerViewModel { PlanViewModel(it.plans) }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
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
                        onCreatePlan = { nav.navigate(Routes.ONBOARDING) },
                        onSendPending = { planVm.sendPending() }
                    )
                }
                composable(Routes.CATALOG) {
                    val vm: CatalogViewModel = containerViewModel { CatalogViewModel(it.exercises) }
                    CatalogScreen(vm, onOpen = { nav.navigate(Routes.exercise(it)) })
                }
                composable(Routes.ACCOUNT) {
                    val vm: AccountViewModel = containerViewModel { AccountViewModel(it.subscription, it.plans) }
                    AccountScreen(vm, c.auth, user, onLogout = onLogout, onPlanArchived = { planVm.load(); nav.navigateTab(Routes.PLAN) })
                }
                composable(Routes.ONBOARDING) {
                    val vm: OnboardingViewModel = containerViewModel { OnboardingViewModel(it.plans) }
                    OnboardingScreen(vm, onBack = { nav.popBackStack() }, onCreated = { planVm.load(); nav.popBackStack() })
                }
                composable(Routes.OVERVIEW) { PlanOverviewScreen(planVm, onBack = { nav.popBackStack() }) }
                composable(Routes.HISTORY) {
                    val vm: HistoryViewModel = containerViewModel { HistoryViewModel(it.plans) }
                    HistoryScreen(vm, onBack = { nav.popBackStack() })
                }
                composable(Routes.SESSION, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    SessionScreen(c.plans.week, id, onBack = { nav.popBackStack() }, onStart = { nav.navigate(Routes.workout(id)) }, onExercise = { nav.navigate(Routes.exercise(it)) })
                }
                composable(Routes.WORKOUT, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val vm: WorkoutViewModel = containerViewModel(key = "workout-$id") { WorkoutViewModel(it.plans, it.drafts, id) }
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
