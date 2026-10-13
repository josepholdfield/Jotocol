package com.jotocol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.jotocol.R
import com.jotocol.nfc.NfcController
import com.jotocol.nfc.NfcEvent
import kotlinx.coroutines.delay

@Composable
fun NfcScreen(habitId: Long, viewModel: HabitsViewModel, nfc: NfcController) {
    val habits by viewModel.today.collectAsStateWithLifecycle()
    val readerModeSupported by nfc.readerModeSupported.collectAsStateWithLifecycle()
    val item = habits.firstOrNull { it.habit.id == habitId }
    val listState = rememberTransformingLazyColumnState()

    // While this screen is open, scanned tags are linked to this habit instead of completing one.
    DisposableEffect(habitId) {
        nfc.registrationTarget.value = habitId
        onDispose { nfc.registrationTarget.value = null }
    }

    if (item == null) {
        EmptyBox { Text("…") }
        return
    }
    val habit = item.habit

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item { ListHeader { Text(stringResource(R.string.nfc_title), textAlign = TextAlign.Center) } }
            item {
                val message = when {
                    !readerModeSupported || !nfc.isSupported -> stringResource(R.string.nfc_unsupported)
                    !nfc.isEnabled -> stringResource(R.string.nfc_disabled)
                    habit.nfcTagId != null -> stringResource(R.string.nfc_registered)
                    else -> stringResource(R.string.nfc_hold_tag, habit.name)
                }
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            if (habit.nfcTagId != null) {
                item {
                    Button(
                        onClick = { viewModel.forgetTag(habit) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        label = { Text(stringResource(R.string.nfc_forget)) },
                    )
                }
            }
        }
    }
}

/** Brief full-screen toast for the result of a tag scan, shown over whatever screen is open. */
@Composable
fun NfcEventOverlay(nfc: NfcController) {
    val event by nfc.events.collectAsStateWithLifecycle()
    val current = event ?: return

    LaunchedEffect(current) {
        delay(2_500)
        nfc.consumeEvent()
    }

    val text = when (current) {
        is NfcEvent.Registered -> stringResource(R.string.nfc_registered)
        is NfcEvent.Completed -> stringResource(R.string.nfc_completed, current.habit.name)
        is NfcEvent.AlreadyDone -> stringResource(R.string.nfc_already_done, current.habit.name)
        is NfcEvent.TagInUse -> stringResource(R.string.nfc_tag_in_use, current.owner.name)
        is NfcEvent.UnknownTag -> stringResource(R.string.nfc_unknown_tag)
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
