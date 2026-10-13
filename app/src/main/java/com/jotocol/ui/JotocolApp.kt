package com.jotocol.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.jotocol.nfc.NfcController
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val HOME = "home"
    const val HABIT = "habit/{id}"
    const val EDIT = "edit/{id}"
    const val EDIT_TIME = "edit/time"
    const val NFC = "nfc/{id}"

    fun habit(id: Long) = "habit/$id"
    fun edit(id: Long) = "edit/$id"
    fun nfc(id: Long) = "nfc/$id"
}

@Composable
fun JotocolApp(
    viewModel: HabitsViewModel,
    nfc: NfcController,
    pendingOpen: MutableStateFlow<Long?>,
    onRequestNotifications: () -> Unit,
) {
    val navController = rememberSwipeDismissableNavController()
    val toOpen by pendingOpen.collectAsState()

    LaunchedEffect(toOpen) {
        val id = toOpen ?: return@LaunchedEffect
        pendingOpen.value = null
        navController.navigate(Routes.habit(id)) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
    }

    MaterialTheme {
        AppScaffold {
            NfcEventOverlay(nfc = nfc)
            SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenHabit = { navController.navigate(Routes.habit(it)) },
                        onAddHabit = {
                            viewModel.startEdit(null)
                            navController.navigate(Routes.edit(0))
                        },
                        onRequestNotifications = onRequestNotifications,
                    )
                }
                composable(
                    Routes.HABIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    HabitDetailScreen(
                        habitId = id,
                        viewModel = viewModel,
                        onEdit = { habit ->
                            viewModel.startEdit(habit)
                            navController.navigate(Routes.edit(habit.id))
                        },
                        onNfc = { navController.navigate(Routes.nfc(id)) },
                        onDeleted = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    )
                }
                composable(
                    Routes.EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) {
                    HabitEditScreen(
                        viewModel = viewModel,
                        onPickTime = { navController.navigate(Routes.EDIT_TIME) },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(Routes.EDIT_TIME) {
                    TimePickerScreen(
                        viewModel = viewModel,
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(
                    Routes.NFC,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    NfcScreen(habitId = id, viewModel = viewModel, nfc = nfc)
                }
            }
        }
    }
}
