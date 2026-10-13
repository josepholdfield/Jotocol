package com.jotocol.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.jotocol.R
import com.jotocol.domain.Habit
import com.jotocol.domain.HabitState

@Composable
fun HabitDetailScreen(
    habitId: Long,
    viewModel: HabitsViewModel,
    onEdit: (Habit) -> Unit,
    onNfc: () -> Unit,
    onDeleted: () -> Unit,
) {
    val habits by viewModel.today.collectAsStateWithLifecycle()
    val item = habits.firstOrNull { it.habit.id == habitId }
    val context = LocalContext.current
    val listState = rememberTransformingLazyColumnState()
    var confirmDelete by remember { mutableStateOf(false) }

    if (item == null) {
        EmptyBox { Text("…") }
        return
    }
    val completed = item.state == HabitState.COMPLETED

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = {
                    if (completed) viewModel.uncomplete(habitId) else viewModel.complete(habitId)
                },
                colors = if (completed) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(),
                border = if (completed) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text(stringResource(if (completed) R.string.detail_undo else R.string.detail_mark_done))
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ListHeader { Text(item.habit.name, textAlign = TextAlign.Center) }
            }
            item {
                val color = when (item.state) {
                    HabitState.OVERDUE -> MaterialTheme.colorScheme.error
                    HabitState.COMPLETED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    item.stateLabel(context),
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (item.remindersSentToday > 0) {
                item {
                    Text(
                        stringResource(R.string.detail_reminders_sent, item.remindersSentToday),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Button(
                    onClick = { onEdit(item.habit) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    label = { Text(stringResource(R.string.detail_edit)) },
                    secondaryLabel = {
                        Text(
                            item.habit.reminderTime.display() + " · " +
                                stringResource(R.string.edit_interval_value, item.habit.escalationMinutes),
                        )
                    },
                )
            }
            item {
                Button(
                    onClick = onNfc,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(stringResource(R.string.detail_nfc)) },
                    secondaryLabel = if (item.habit.nfcTagId != null) {
                        { Text(stringResource(R.string.detail_nfc_registered)) }
                    } else {
                        null
                    },
                )
            }
            item {
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    label = { Text(stringResource(R.string.detail_delete)) },
                )
            }
        }
    }

    AlertDialog(
        visible = confirmDelete,
        onDismissRequest = { confirmDelete = false },
        icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
        title = { Text(stringResource(R.string.detail_delete_confirm, item.habit.name)) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(onClick = {
                confirmDelete = false
                viewModel.delete(item.habit)
                onDeleted()
            })
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = { confirmDelete = false })
        },
    )
}
