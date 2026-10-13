package com.jotocol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.jotocol.R
import com.jotocol.domain.HabitState
import com.jotocol.domain.HabitToday

@Composable
fun HomeScreen(
    viewModel: HabitsViewModel,
    onOpenHabit: (Long) -> Unit,
    onAddHabit: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val habits by viewModel.today.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberTransformingLazyColumnState()

    val outstanding = habits.filter { it.isOutstanding }
    val completed = habits.filter { it.state == HabitState.COMPLETED }
    val disabled = habits.filter { it.state == HabitState.DISABLED }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ListHeader {
                    Text(date.display(), textAlign = TextAlign.Center)
                }
            }

            if (!permissions.notificationsGranted) {
                item {
                    WarningRow(
                        text = stringResource(R.string.home_notifications_off),
                        onClick = onRequestNotifications,
                    )
                }
            }
            if (!permissions.exactAlarms) {
                item {
                    WarningRow(
                        text = stringResource(R.string.home_inexact_alarms),
                        onClick = { openExactAlarmSettings(context) },
                    )
                }
            }

            if (habits.isEmpty()) {
                item { ListSubHeader { Text(stringResource(R.string.home_no_habits)) } }
            }

            if (outstanding.isNotEmpty()) {
                item { ListSubHeader { Text(stringResource(R.string.home_outstanding)) } }
                items(outstanding.size, key = { "o-${outstanding[it].habit.id}" }) { index ->
                    OutstandingRow(
                        item = outstanding[index],
                        onOpen = { onOpenHabit(outstanding[index].habit.id) },
                        onComplete = { viewModel.complete(outstanding[index].habit.id) },
                    )
                }
            } else if (habits.isNotEmpty()) {
                item {
                    ListSubHeader {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_all_done))
                    }
                }
            }

            if (completed.isNotEmpty()) {
                item { ListSubHeader { Text(stringResource(R.string.home_completed)) } }
                items(completed.size, key = { "c-${completed[it].habit.id}" }) { index ->
                    CompletedRow(item = completed[index], onOpen = { onOpenHabit(completed[index].habit.id) })
                }
            }

            if (disabled.isNotEmpty()) {
                items(disabled.size, key = { "d-${disabled[it].habit.id}" }) { index ->
                    CompletedRow(item = disabled[index], onOpen = { onOpenHabit(disabled[index].habit.id) })
                }
            }

            item {
                Button(
                    onClick = onAddHabit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_add_habit)) },
                )
            }
        }
    }
}

@Composable
private fun OutstandingRow(item: HabitToday, onOpen: () -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val overdue = item.state == HabitState.OVERDUE
    val colors = when (item.state) {
        HabitState.OVERDUE -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            secondaryContentColor = MaterialTheme.colorScheme.onErrorContainer,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        HabitState.DUE -> ButtonDefaults.filledVariantButtonColors()
        else -> ButtonDefaults.filledTonalButtonColors()
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            colors = colors,
            icon = if (overdue) {
                { Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.state_overdue)) }
            } else {
                null
            },
            label = { Text(item.habit.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = { Text(item.stateLabel(context), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        Spacer(Modifier.width(4.dp))
        FilledIconButton(
            onClick = onComplete,
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.detail_mark_done))
        }
    }
}

@Composable
private fun CompletedRow(item: HabitToday, onOpen: () -> Unit) {
    val context = LocalContext.current
    Button(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        icon = if (item.state == HabitState.COMPLETED) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
        label = { Text(item.habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = { Text(item.stateLabel(context), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun WarningRow(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
        label = { Text(text, maxLines = 3) },
    )
}

@Composable
fun EmptyBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
