package com.messageatlas.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.messageatlas.app.MainViewModel

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("home", "收件箱", Icons.Outlined.Inbox),
    Destination("history", "日报", Icons.Outlined.AutoAwesome),
    Destination("settings", "设置", Icons.Outlined.Tune)
)

@Composable
fun MessageAtlasRoot(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    val topLevel = current in destinations.map { it.route }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            vm.consumeNotice()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (topLevel) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                        destinations.forEach { d ->
                            NavigationBarItem(
                                selected = current == d.route,
                                onClick = {
                                    nav.navigate(d.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(d.icon, null) },
                                label = { Text(d.label) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            NavHost(
                navController = nav, startDestination = "home",
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                enterTransition = { if (state.settings.animationEnabled) fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 16 } else EnterTransition.None },
                exitTransition = { if (state.settings.animationEnabled) fadeOut(tween(100)) else ExitTransition.None },
                popEnterTransition = { if (state.settings.animationEnabled) fadeIn(tween(160)) else EnterTransition.None },
                popExitTransition = { if (state.settings.animationEnabled) fadeOut(tween(100)) else ExitTransition.None }
            ) {
                composable("home") { HomeScreen(state, vm, { nav.navigate("ai") }, { nav.navigate("history") }) }
                composable("history") { HistoryScreen(state, vm) { nav.navigate("home") } }
                composable("settings") { SettingsScreen(state, vm, { nav.navigate("ai") }, { nav.navigate("rules") }) }
                composable("rules") { RulesScreen(state, vm) { nav.popBackStack() } }
                composable("ai") { AiScreen(state, vm) { nav.popBackStack() } }
            }
        }
    }
    if (state.loaded && !state.settings.onboardingSeen) PermissionIntro(vm)
}
