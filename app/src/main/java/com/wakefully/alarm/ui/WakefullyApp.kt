package com.wakefully.alarm.ui

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wakefully.alarm.data.StreakStats
import com.wakefully.alarm.data.VerveDatabaseRepository
import com.wakefully.alarm.data.VerveRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.AlarmController
import com.wakefully.alarm.service.AlarmDismissalReason
import com.wakefully.alarm.service.AlarmReceiver
import com.wakefully.alarm.service.AlarmService
import com.wakefully.alarm.service.AndroidAlarmScheduler
import com.wakefully.alarm.service.DefaultAlarmController
import com.wakefully.alarm.ui.create.CreateEditAlarmScreen
import com.wakefully.alarm.ui.home.AlarmListScreen
import com.wakefully.alarm.ui.mission.barcode.BarcodeMissionScreen
import com.wakefully.alarm.ui.mission.barcode.BarcodeMissionViewModel
import com.wakefully.alarm.ui.mission.math.MathMissionScreen
import com.wakefully.alarm.ui.mission.math.MathMissionViewModel
import com.wakefully.alarm.ui.ring.AlarmRingingActivity
import com.wakefully.alarm.ui.ring.RingingScreen
import com.wakefully.alarm.ui.success.SuccessScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

sealed interface AppScreen {
    data object AlarmList : AppScreen
    data class CreateEdit(val alarm: Alarm? = null) : AppScreen
    data class Ringing(val alarm: Alarm) : AppScreen
    data class MathMission(val alarm: Alarm) : AppScreen
    data class BarcodeMission(val alarm: Alarm, val targetBarcode: RegisteredBarcode?) : AppScreen
    data class Success(val summary: String) : AppScreen
}

@Composable
fun WakefullyApp(
    repository: VerveRepository? = null,
    alarmController: DefaultAlarmController = remember { DefaultAlarmController() },
    initialRingingAlarm: Alarm? = null
) {
    val context = LocalContext.current
    val currentRepo = repository ?: remember { VerveDatabaseRepository.getInstance(context) }
    val scheduler = remember { AndroidAlarmScheduler(context) }
    val scope = rememberCoroutineScope()

    var currentScreen by remember {
        mutableStateOf<AppScreen>(
            if (initialRingingAlarm != null) AppScreen.Ringing(initialRingingAlarm) else AppScreen.AlarmList
        )
    }

    val alarms by currentRepo.getAllAlarms().collectAsState(initial = emptyList())
    val barcodes by currentRepo.getAllBarcodes().collectAsState(initial = emptyList())
    val streakStats by currentRepo.getStreakStats().collectAsState(initial = StreakStats())

    // Ensure all enabled alarms are scheduled in Android AlarmManager on launch
    LaunchedEffect(alarms) {
        alarms.filter { it.enabled }.forEach { scheduler.schedule(it) }
    }

    // React to incoming ringing alarms launched from intent
    LaunchedEffect(initialRingingAlarm) {
        if (initialRingingAlarm != null) {
            currentScreen = AppScreen.Ringing(initialRingingAlarm)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val screen = currentScreen) {
            AppScreen.AlarmList -> {
                AlarmListScreen(
                    alarms = alarms,
                    barcodes = barcodes,
                    streakStats = streakStats,
                    onToggleAlarm = { id, enabled ->
                        scope.launch {
                            currentRepo.setAlarmEnabled(id, enabled)
                            val alarm = alarms.firstOrNull { it.id == id }
                            if (alarm != null) {
                                if (enabled) {
                                    scheduler.schedule(alarm.copy(enabled = true))
                                } else {
                                    scheduler.cancel(id)
                                }
                            }
                        }
                    },
                    onDeleteAlarm = { id ->
                        scope.launch {
                            scheduler.cancel(id)
                            currentRepo.deleteAlarm(id)
                        }
                    },
                    onEditAlarm = { alarm ->
                        currentScreen = AppScreen.CreateEdit(alarm)
                    },
                    onCreateAlarm = {
                        currentScreen = AppScreen.CreateEdit(null)
                    },
                    onTriggerTestAlarm = { alarm ->
                        // Start foreground ringing service
                        AlarmService.startAlarm(context, alarm)

                        // Launch full-screen AlarmRingingActivity
                        val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                            action = AlarmReceiver.ACTION_RING
                            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
                            putExtra(AlarmReceiver.EXTRA_ALARM_TONE, alarm.toneName)
                            putExtra(AlarmReceiver.EXTRA_ALARM_URI, alarm.toneUri)
                            putExtra(AlarmReceiver.EXTRA_MISSION_TYPE, alarm.missionType.name)
                            putExtra(AlarmReceiver.EXTRA_BARCODE_ID, alarm.barcodeId ?: -1L)
                            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hourOfDay)
                            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(ringIntent)
                        } catch (_: Exception) {
                            alarmController.triggerAlarm(alarm)
                            currentScreen = AppScreen.Ringing(alarm)
                        }
                    }
                )
            }

            is AppScreen.CreateEdit -> {
                CreateEditAlarmScreen(
                    initialAlarm = screen.alarm,
                    barcodes = barcodes,
                    onSaveAlarm = { savedAlarm ->
                        scope.launch {
                            val id = currentRepo.saveAlarm(savedAlarm)
                            val persistedAlarm = savedAlarm.copy(id = id)
                            scheduler.schedule(persistedAlarm)
                            currentScreen = AppScreen.AlarmList
                        }
                    },
                    onRegisterBarcode = { name, code ->
                        if (currentRepo is VerveDatabaseRepository) {
                            currentRepo.saveBarcodeDirect(
                                RegisteredBarcode(id = 0L, name = name, value = code, format = 32)
                            )
                        } else {
                            var newId = 0L
                            runBlocking {
                                newId = currentRepo.saveBarcode(
                                    RegisteredBarcode(id = 0L, name = name, value = code, format = 32)
                                )
                            }
                            newId
                        }
                    },
                    onDeleteBarcode = { id ->
                        if (currentRepo is VerveDatabaseRepository) {
                            currentRepo.deleteBarcodeDirect(id)
                        } else {
                            scope.launch { currentRepo.deleteBarcode(id) }
                        }
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.AlarmList
                    }
                )
            }

            is AppScreen.Ringing -> {
                RingingScreen(
                    alarm = screen.alarm,
                    onStartMission = {
                        when (screen.alarm.missionType) {
                            MissionType.BARCODE -> {
                                val target = barcodes.firstOrNull { it.id == screen.alarm.barcodeId }
                                    ?: barcodes.firstOrNull()
                                currentScreen = AppScreen.BarcodeMission(screen.alarm, target)
                            }
                            MissionType.MATH -> {
                                currentScreen = AppScreen.MathMission(screen.alarm)
                            }
                            MissionType.SHAKE, MissionType.NONE -> {
                                currentRepo.recordSuccessfulWakeup(screen.alarm.missionType)
                                alarmController.dismissAlarm(
                                    AlarmDismissalReason.MissionVerified(screen.alarm.missionType)
                                )
                                AlarmService.stopAlarm(context)
                                currentScreen = AppScreen.Success("Standard Dismissal Completed")
                            }
                        }
                    },
                    onEmergencyDismiss = {
                        alarmController.dismissAlarm(AlarmDismissalReason.EmergencyHoldCompleted)
                        AlarmService.stopAlarm(context)
                        currentScreen = AppScreen.Success("Emergency 5-Second Override Engaged")
                    }
                )
            }

            is AppScreen.MathMission -> {
                val mathVm = remember(screen.alarm.id) {
                    MathMissionViewModel()
                }
                MathMissionScreen(
                    viewModel = mathVm,
                    onDismissAlarm = {
                        currentRepo.recordSuccessfulWakeup(MissionType.MATH)
                        alarmController.dismissAlarm(
                            AlarmDismissalReason.MissionVerified(MissionType.MATH, "3 Problems Solved")
                        )
                        AlarmService.stopAlarm(context)
                        currentScreen = AppScreen.Success("Mental Math Verified // 3 Problems Solved")
                    }
                )
            }

            is AppScreen.BarcodeMission -> {
                val barcodeVm = remember(screen.alarm.id) {
                    BarcodeMissionViewModel(
                        alarm = screen.alarm,
                        barcodeRepository = currentRepo,
                        alarmController = alarmController,
                        initialTarget = screen.targetBarcode
                    )
                }
                BarcodeMissionScreen(
                    viewModel = barcodeVm,
                    onFallbackEmergency = {
                        currentScreen = AppScreen.MathMission(screen.alarm)
                    }
                )

                // If alarm was dismissed, navigate to success
                val vmState by barcodeVm.uiState.collectAsState()
                if (vmState.isDismissed) {
                    currentRepo.recordSuccessfulWakeup(MissionType.BARCODE)
                    AlarmService.stopAlarm(context)
                    currentScreen = AppScreen.Success(
                        "Optical Signature Verified // ${screen.targetBarcode?.name ?: "Barcoded Item"}"
                    )
                }
            }

            is AppScreen.Success -> {
                SuccessScreen(
                    missionSummary = screen.summary,
                    onReturnHome = {
                        currentScreen = AppScreen.AlarmList
                    }
                )
            }
        }
    }
}

/**
 * Production Verve root composable application entry point.
 */
@Composable
fun VerveApp(
    repository: VerveRepository? = null,
    alarmController: DefaultAlarmController = remember { DefaultAlarmController() },
    initialRingingAlarm: Alarm? = null
) {
    WakefullyApp(
        repository = repository,
        alarmController = alarmController,
        initialRingingAlarm = initialRingingAlarm
    )
}
