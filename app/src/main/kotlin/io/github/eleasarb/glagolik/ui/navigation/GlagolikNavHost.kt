package io.github.eleasarb.glagolik.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.eleasarb.glagolik.ui.about.AboutScreen
import io.github.eleasarb.glagolik.ui.settings.SettingsRoute
import io.github.eleasarb.glagolik.ui.training.TrainingRoute
import io.github.eleasarb.glagolik.ui.verbs.VerbDetailRoute
import io.github.eleasarb.glagolik.ui.verbs.VerbListRoute

const val VERB_ID_ARG = "verbId"

private const val VERBS_ROUTE = "verbs"
private const val VERB_DETAIL_ROUTE = "verbs/{$VERB_ID_ARG}"
private const val ABOUT_ROUTE = "about"
private const val TRAINING_ROUTE = "training"
private const val SETTINGS_ROUTE = "settings"

private fun verbDetailRoute(id: Int): String = "verbs/$id"

@Composable
fun GlagolikNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = VERBS_ROUTE,
        modifier = modifier,
    ) {
        composable(VERBS_ROUTE) {
            VerbListRoute(
                onVerbClick = { id -> navController.navigate(verbDetailRoute(id)) },
                onTrainingClick = { navController.navigate(TRAINING_ROUTE) },
                onSettingsClick = { navController.navigate(SETTINGS_ROUTE) },
                onAboutClick = { navController.navigate(ABOUT_ROUTE) },
            )
        }
        composable(
            route = VERB_DETAIL_ROUTE,
            arguments = listOf(navArgument(VERB_ID_ARG) { type = NavType.IntType }),
        ) {
            VerbDetailRoute(
                onBack = { navController.popBackStack() },
                onPartnerClick = { id -> navController.navigate(verbDetailRoute(id)) },
            )
        }
        composable(ABOUT_ROUTE) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(TRAINING_ROUTE) {
            TrainingRoute(onClose = { navController.popBackStack() })
        }
        composable(SETTINGS_ROUTE) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
