package com.jotocol.ui

import android.app.AlarmManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jotocol.Graph
import com.jotocol.domain.Habit
import com.jotocol.domain.HabitStatus
import com.jotocol.domain.HabitToday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

data class PermissionState(
    val notificationsGranted: Boolean = true,
    val exactAlarms: Boolean = true,
)

/** Single view model shared by every screen; state is derived live from the repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(private val graph: Graph) : ViewModel() {

    /**
     * Bumped on every system TIME_TICK / time change (see MainActivity) and on resume, with a
     * 60 s loop as a fallback, so DUE/OVERDUE transitions and the date roll over promptly.
     */
    private val tick = MutableStateFlow(0L)

    private val _permissions = MutableStateFlow(PermissionState())
    val permissions: StateFlow<PermissionState> = _permissions

    /** In-progress edits for the edit screen and its time picker sub-screen. */
    val draft = MutableStateFlow<Habit?>(null)

    /** Today's local date; rolls over at midnight (or on a clock change) via [tick]. */
    val date: StateFlow<LocalDate> =
        tick.map { LocalDate.now(graph.clock) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(graph.clock))

    val today: StateFlow<List<HabitToday>> =
        date
            .flatMapLatest { date ->
                combine(
                    graph.repository.observeHabits(),
                    graph.repository.observeCompletions(date),
                    graph.repository.observeReminderLogs(date),
                    tick,
                ) { habits, completions, logs, _ ->
                    val now = ZonedDateTime.now(graph.clock)
                    habits.map { habit ->
                        HabitStatus.today(
                            habit = habit,
                            completion = completions.firstOrNull { it.habitId == habit.id },
                            reminderLogs = logs,
                            now = now,
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) { graph.actions.recover() }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                tick.value++
            }
        }
    }

    /** Re-derive everything from the current clock. */
    fun refreshNow() {
        tick.value++
    }

    fun onResumed(context: Context) {
        tick.value++
        refreshPermissions(context)
        viewModelScope.launch(Dispatchers.IO) { graph.scheduler.reconcileAll() }
    }

    fun refreshPermissions(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        _permissions.value = PermissionState(
            notificationsGranted = graph.notifications.hasPermission(),
            exactAlarms = alarmManager.canScheduleExactAlarms(),
        )
    }

    fun habit(id: Long): HabitToday? = today.value.firstOrNull { it.habit.id == id }

    fun complete(habitId: Long) = viewModelScope.launch(Dispatchers.IO) { graph.actions.complete(habitId) }

    fun uncomplete(habitId: Long) = viewModelScope.launch(Dispatchers.IO) { graph.actions.uncomplete(habitId) }

    fun delete(habit: Habit) = viewModelScope.launch(Dispatchers.IO) { graph.actions.delete(habit) }

    fun startEdit(habit: Habit?) {
        draft.value = habit ?: Habit(name = "", reminderTime = java.time.LocalTime.of(9, 0))
    }

    fun updateDraft(transform: (Habit) -> Habit) {
        draft.value = draft.value?.let(transform)
    }

    fun saveDraft(onSaved: (Long) -> Unit) {
        val habit = draft.value ?: return
        viewModelScope.launch {
            val id = kotlinx.coroutines.withContext(Dispatchers.IO) {
                graph.actions.save(habit.copy(name = habit.name.trim().ifEmpty { "Habit" }))
            }
            draft.value = null
            onSaved(id)
        }
    }

    fun forgetTag(habit: Habit) = viewModelScope.launch(Dispatchers.IO) {
        graph.actions.save(habit.copy(nfcTagId = null))
    }

    class Factory(private val graph: Graph) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HabitsViewModel(graph) as T
    }
}
