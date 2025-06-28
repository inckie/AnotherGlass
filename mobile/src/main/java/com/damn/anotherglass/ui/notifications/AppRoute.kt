package com.damn.anotherglass.ui.notifications

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.damn.anotherglass.ui.notifications.editfilter.FilterEditScreen
import com.damn.anotherglass.ui.notifications.editfilter.FilterEditViewModel
import com.damn.anotherglass.ui.notifications.filters.FilterListScreen
import com.damn.anotherglass.ui.notifications.filters.FilterListViewModel
import com.damn.anotherglass.ui.notifications.history.NotificationHistoryScreen
import com.damn.anotherglass.ui.notifications.history.NotificationHistoryViewModel

sealed class AppRoute(val route: String) {
    data object FilterList : AppRoute("filter_list")
    data object NotificationHistory : AppRoute("notification_history")
    data object FilterEditScreen : AppRoute("filter_edit_screen") {
        // For FilterEditScreen, define arguments
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
            val route = StringBuilder(route)
            val params = mutableListOf<String>()
            title?.let { params.add("$FILTER_EDIT_ARG_TITLE=${it.urlEncode()}") }
            text?.let { params.add("$FILTER_EDIT_ARG_TEXT=${it.urlEncode()}") }
            packageName?.let { params.add("$FILTER_EDIT_ARG_PACKAGE_NAME=${it.urlEncode()}") }
            tickerText?.let { params.add("$FILTER_EDIT_ARG_TICKER_TEXT=${it.urlEncode()}") }
            isOngoing?.let { params.add("$FILTER_EDIT_ARG_IS_ONGOING=$it") }
            filterId?.let { params.add("$FILTER_EDIT_ARG_FILTER_ID=${it.urlEncode()}") }

            if (params.isNotEmpty()) {
                route.append("?").append(params.joinToString("&"))
            }
            return route.toString()
        }
    }
}

fun navGraph(builder: NavGraphBuilder, navController: NavHostController) {
    builder.apply {
        composable(AppRoute.NotificationHistory.route) {
            val viewModel: NotificationHistoryViewModel = viewModel(factory = NotificationHistoryViewModel.Companion.Factory(LocalContext.current.applicationContext as Application))

            NotificationHistoryScreen(navController = navController, viewModel)
        }
        composable(
            route = AppRoute.FilterEditScreen.routeTemplate,
            arguments = listOf(
                navArgument(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TITLE) {
                    type = NavType.StringType; nullable = true
                },
                navArgument(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TEXT) {
                    type = NavType.StringType; nullable = true
                },
                navArgument(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_PACKAGE_NAME) {
                    type = NavType.StringType; nullable = true
                },
                navArgument(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TICKER_TEXT) {
                    type = NavType.StringType; nullable = true
                },
                navArgument(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING) {
                    type = NavType.BoolType; defaultValue =
                    false // Provide a default if not passed
                },
                navArgument(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_FILTER_ID) {
                    type = NavType.StringType; nullable = true
                }
            )
        ) {
            val viewModel: FilterEditViewModel = viewModel(
                factory = FilterEditViewModel.Companion.Factory(
                    application = LocalContext.current.applicationContext as Application,
                    savedStateHandle = SavedStateHandle()
                )
            )
            FilterEditScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(AppRoute.FilterList.route) {
            val viewModel: FilterListViewModel = viewModel(
                factory = FilterListViewModel.Companion.Factory(LocalContext.current.applicationContext as Application))
            FilterListScreen(navController = navController, viewModel = viewModel)
        }
    }
}