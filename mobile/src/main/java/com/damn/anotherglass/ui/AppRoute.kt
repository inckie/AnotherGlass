package com.damn.anotherglass.ui

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.damn.anotherglass.extensions.notifications.filter.IImportExportController
import com.damn.anotherglass.ui.mainscreen.MainScreen
import com.damn.anotherglass.ui.mainscreen.ServiceController
import com.damn.anotherglass.ui.notifications.editfilter.FilterEditScreen
import com.damn.anotherglass.ui.notifications.editfilter.FilterEditViewModel
import com.damn.anotherglass.ui.notifications.editfilter.FilterEditViewModel.Companion.urlEncode
import com.damn.anotherglass.ui.notifications.filters.FilterListScreen
import com.damn.anotherglass.ui.notifications.filters.FilterListViewModel
import com.damn.anotherglass.ui.notifications.history.NotificationHistoryScreen
import com.damn.anotherglass.ui.notifications.history.NotificationHistoryViewModel

sealed class AppRoute(val route: String) {
    data object MainScreen : AppRoute("main_screen")
    data object FilterList : AppRoute("filter_list")
    data object NotificationHistory : AppRoute("notification_history")
    data object FilterEditScreen : AppRoute("filter_edit_screen") {

        const val FILTER_EDIT_ARG_TITLE = "title"
        const val FILTER_EDIT_ARG_TEXT = "text"
        const val FILTER_EDIT_ARG_PACKAGE_NAME = "packageName"
        const val FILTER_EDIT_ARG_TICKER_TEXT = "tickerText"
        const val FILTER_EDIT_ARG_IS_ONGOING = "isOngoing"

        // Optional: Argument for existing filter ID if editing
        const val FILTER_EDIT_ARG_FILTER_ID = "filterId"

        // Construct route with optional and required arguments
        val routeTemplate: String = "${super.route}?" +
                "${FILTER_EDIT_ARG_TITLE}={${FILTER_EDIT_ARG_TITLE}}&" +
                "${FILTER_EDIT_ARG_TEXT}={${FILTER_EDIT_ARG_TEXT}}&" +
                "${FILTER_EDIT_ARG_PACKAGE_NAME}={${FILTER_EDIT_ARG_PACKAGE_NAME}}&" +
                "${FILTER_EDIT_ARG_TICKER_TEXT}={${FILTER_EDIT_ARG_TICKER_TEXT}}&" +
                "${FILTER_EDIT_ARG_IS_ONGOING}={${FILTER_EDIT_ARG_IS_ONGOING}}&" +
                "${FILTER_EDIT_ARG_FILTER_ID}={${FILTER_EDIT_ARG_FILTER_ID}}"

        // Helper to navigate to filter edit screen
        fun buildFilterEditRoute(
            title: String? = null,
            text: String? = null,
            packageName: String? = null,
            tickerText: String? = null,
            isOngoing: Boolean? = null,
            filterId: String? = null
        ): String {
            val params = mutableListOf<String>()
            title?.let { params.add("$FILTER_EDIT_ARG_TITLE=${it.urlEncode()}") }
            text?.let { params.add("$FILTER_EDIT_ARG_TEXT=${it.urlEncode()}") }
            packageName?.let { params.add("$FILTER_EDIT_ARG_PACKAGE_NAME=${it.urlEncode()}") }
            tickerText?.let { params.add("$FILTER_EDIT_ARG_TICKER_TEXT=${it.urlEncode()}") }
            isOngoing?.let { params.add("$FILTER_EDIT_ARG_IS_ONGOING=$it") }
            filterId?.let { params.add("$FILTER_EDIT_ARG_FILTER_ID=${it.urlEncode()}") }
            return when {
                params.isEmpty() -> route
                else -> route + "?" + params.joinToString("&")
            }
        }

        val arguments = listOf(
            navArgument(FILTER_EDIT_ARG_TITLE) {
                type = NavType.StringType; nullable = true
            },
            navArgument(FILTER_EDIT_ARG_TEXT) {
                type = NavType.StringType; nullable = true
            },
            navArgument(FILTER_EDIT_ARG_PACKAGE_NAME) {
                type = NavType.StringType; nullable = true
            },
            navArgument(FILTER_EDIT_ARG_TICKER_TEXT) {
                type = NavType.StringType; nullable = true
            },
            navArgument(FILTER_EDIT_ARG_IS_ONGOING) {
                type = NavType.BoolType; defaultValue =
                false // Provide a default if not passed
            },
            navArgument(FILTER_EDIT_ARG_FILTER_ID) {
                type = NavType.StringType; nullable = true
            }
        )
    }
}

fun navGraph(
    builder: NavGraphBuilder,
    navController: NavHostController,
    importExportController: IImportExportController,
    coreController: ServiceController
) {
    builder.apply {
        composable(AppRoute.MainScreen.route) {
            MainScreen(
                navController = navController,
                settings = coreController.controller,
                serviceController = coreController
            )
        }
        composable(AppRoute.NotificationHistory.route) {
            val viewModel: NotificationHistoryViewModel = viewModel(
                factory = NotificationHistoryViewModel.Companion.Factory(LocalContext.current)
            )
            NotificationHistoryScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            route = AppRoute.FilterEditScreen.routeTemplate,
            arguments = AppRoute.FilterEditScreen.arguments
        ) {
            val viewModel: FilterEditViewModel = viewModel(
                factory = FilterEditViewModel.Companion.Factory(
                    context = LocalContext.current,
                    savedStateHandle = it.savedStateHandle)
            )
            FilterEditScreen(navController = navController, viewModel = viewModel)
        }
        composable(AppRoute.FilterList.route) {
            val viewModel: FilterListViewModel = viewModel(
                factory = FilterListViewModel.Companion.Factory(LocalContext.current)
            )
            FilterListScreen(
                navController = navController,
                viewModel = viewModel,
                importExportController = importExportController
            )
        }
    }
}