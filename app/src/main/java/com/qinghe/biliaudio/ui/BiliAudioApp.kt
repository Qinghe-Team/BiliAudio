package com.qinghe.biliaudio.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.qinghe.biliaudio.core.AppContainer

@Composable
fun BiliAudioApp(appContainer: AppContainer) {
    val navController = rememberSwipeDismissableNavController()
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(appContainer))

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = AppDestination.HOME.route
    ) {
        composable(AppDestination.HOME.route) {
            HomeRoute(
                viewModel = viewModel,
                navigate = navController
            )
        }
        composable(AppDestination.LOGIN.route) {
            LoginScreen(viewModel = viewModel)
        }
        composable(AppDestination.SEARCH.route) {
            SearchScreen(
                viewModel = viewModel,
                openDetail = { navController.navigate(AppDestination.DETAIL.route) }
            )
        }
        composable(AppDestination.DETAIL.route) {
            VideoDetailScreen(
                viewModel = viewModel,
                openPlayer = { navController.navigate(AppDestination.PLAYER.route) },
                openInteractions = { navController.navigate(AppDestination.INTERACTIONS.route) },
                openComments = { navController.navigate(AppDestination.COMMENTS.route) }
            )
        }
        composable(AppDestination.PLAYER.route) {
            PlayerScreen(
                viewModel = viewModel,
                openSpeed = {
                    viewModel.resetCustomSpeedDraft()
                    navController.navigate(AppDestination.CUSTOM_SPEED.route)
                },
                openTimer = {
                    viewModel.resetCustomTimerDraft()
                    navController.navigate(AppDestination.SLEEP_TIMER.route)
                }
            )
        }
        composable(AppDestination.INTERACTIONS.route) {
            InteractionsScreen(viewModel = viewModel)
        }
        composable(AppDestination.COMMENTS.route) {
            CommentsScreen(viewModel = viewModel)
        }
        composable(AppDestination.FAVORITES.route) {
            FavoritesScreen(viewModel = viewModel)
        }
        composable(AppDestination.PROFILE.route) {
            ProfileScreen(viewModel = viewModel)
        }
        composable(AppDestination.CUSTOM_SPEED.route) {
            CustomSpeedScreen(
                viewModel = viewModel,
                close = { navController.popBackStack() }
            )
        }
        composable(AppDestination.SLEEP_TIMER.route) {
            SleepTimerScreen(
                viewModel = viewModel,
                close = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun HomeRoute(viewModel: AppViewModel, navigate: NavHostController) {
    HomeScreen(
        viewModel = viewModel,
        onLogin = { navigate.navigate(AppDestination.LOGIN.route) },
        onSearch = { navigate.navigate(AppDestination.SEARCH.route) },
        onFavorites = { navigate.navigate(AppDestination.FAVORITES.route) },
        onProfile = { navigate.navigate(AppDestination.PROFILE.route) },
        onPlayer = { navigate.navigate(AppDestination.PLAYER.route) },
        onOpenVideo = {
            viewModel.selectVideo(it)
            navigate.navigate(AppDestination.DETAIL.route)
        }
    )
}
