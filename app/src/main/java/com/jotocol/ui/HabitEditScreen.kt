package com.jotocol.ui

import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.input.RemoteInputIntentHelper
import com.jotocol.R
import com.jotocol.domain.Habit

private const val NAME_KEY = "habit_name"

@Composable
fun HabitEditScreen(
    viewModel: HabitsViewModel,
    onPickTime: () -> Unit,
    onSaved: () -> Unit,
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val habit = draft ?: run {
        EmptyBox { Text("…") }
        return
    }
    val listState = rememberTransformingLazyColumnState()
    val namePrompt = stringResource(R.string.edit_name_prompt)

    val nameLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(NAME_KEY) }
        if (!text.isNullOrBlank()) viewModel.updateDraft { it.copy(name = text.toString().trim()) }
    }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = { viewModel.saveDraft { onSaved() } }) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text(stringResource(R.string.edit_save))
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ListHeader {
                    Text(
                        stringResource(if (habit.id == 0L) R.string.edit_title_new else R.string.edit_title),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        RemoteInputIntentHelper.putRemoteInputsExtra(
                            intent,
                            listOf(RemoteInput.Builder(NAME_KEY).setLabel(namePrompt).build()),
                        )
                        nameLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(stringResource(R.string.edit_name)) },
                    secondaryLabel = { Text(habit.name.ifBlank { "—" }) },
                )
            }
            item {
                Button(
                    onClick = onPickTime,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(stringResource(R.string.edit_time)) },
                    secondaryLabel = { Text(habit.reminderTime.display() + " · " + stringResource(R.string.edit_repeat)) },
                )
            }
            item {
                SwitchButton(
                    checked = habit.enabled,
                    onCheckedChange = { on -> viewModel.updateDraft { it.copy(enabled = on) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.edit_enabled)) },
                )
            }
            item {
                SwitchButton(
                    checked = habit.remindersEnabled,
                    onCheckedChange = { on -> viewModel.updateDraft { it.copy(remindersEnabled = on) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = habit.enabled,
                    label = { Text(stringResource(R.string.edit_reminders)) },
                )
            }
            item { ListSubHeader { Text(stringResource(R.string.edit_interval)) } }
            items(Habit.ESCALATION_CHOICES.size) { index ->
                val minutes = Habit.ESCALATION_CHOICES[index]
                RadioButton(
                    selected = habit.escalationMinutes == minutes,
                    onSelect = { viewModel.updateDraft { it.copy(escalationMinutes = minutes) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = habit.enabled && habit.remindersEnabled,
                    label = { Text(stringResource(R.string.edit_interval_value, minutes)) },
                )
            }
        }
    }
}

@Composable
fun TimePickerScreen(viewModel: HabitsViewModel, onDone: () -> Unit) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val habit = draft ?: run {
        EmptyBox { Text("…") }
        return
    }
    TimePicker(
        initialTime = habit.reminderTime,
        timePickerType = TimePickerType.HoursMinutes24H,
        onTimePicked = { time ->
            viewModel.updateDraft { it.copy(reminderTime = time) }
            onDone()
        },
    )
}
