package com.linghualive.flamekit.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.linghualive.flamekit.feature.bookshelf.ui.BookshelfScreen
import com.linghualive.flamekit.feature.reader.ui.ReaderScreen
import com.linghualive.flamekit.feature.settings.ui.SettingsScreen
import com.linghualive.flamekit.feature.source.ui.BookDetailScreen
import com.linghualive.flamekit.feature.source.ui.SearchScreen
import com.linghualive.flamekit.feature.stats.ui.StatsScreen
import com.linghualive.flamekit.feature.sync.ui.SyncScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Bookshelf,
        modifier = modifier,
    ) {
        composable<Screen.Bookshelf> {
            BookshelfScreen(
                onBookClick = { bookId -> navController.navigate(Screen.Reader(bookId)) },
                onSettingsClick = { navController.navigate(Screen.Settings) },
                onSearchClick = { keyword -> navController.navigate(Screen.Search(keyword)) },
            )
        }
        composable<Screen.Reader> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.Reader>()
            ReaderScreen(
                bookId = route.bookId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onStatsClick = { navController.navigate(Screen.Stats) },
                onSyncClick = { navController.navigate(Screen.Sync) },
            )
        }
        composable<Screen.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onBookClick = { sourceUrl, bookUrl ->
                    navController.navigate(Screen.OnlineBookDetail(sourceUrl, bookUrl))
                },
            )
        }
        composable<Screen.OnlineBookDetail> {
            BookDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.Stats> {
            StatsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.Sync> {
            SyncScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
