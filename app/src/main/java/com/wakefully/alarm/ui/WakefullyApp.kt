package com.wakefully.alarm.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wakefully.alarm.data.InMemoryWakefullyRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.AlarmDismissalReason
import com.wakefully.alarm.service.DefaultAlarmController
import com.wakefully.alarm.theme.WakefullyTheme
import com.wakefully.alarm.ui.create.CreateEditAlarmScreen
import com.wakefully.alarm.ui.home.AlarmListScreen
import com.wakefully.alarm.ui.mission.barcode.BarcodeMissionScreen
import com.wakefully.alarm.ui.mission.barcode.BarcodeMissionViewModel
import com.wakefully.alarm.ui.mission.math.MathMissionScreen
import com.wakefully.alarm.ui.mission.math.MathMissionViewModel
import com.wakefully.alarm.ui.ring.RingingScreen
import com.wakefully.alarm.ui.success.SuccessScreen
import kotlinx.coroutines.launch

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
    repository: InMemoryWakefullyRepository = remember { InMemoryWakefullyRepository() },
    alarmController: DefaultAlarmController = remember { DefaultAlarmController() }
) {
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.AlarmList) }

    val alarms by repository.getAllAlarms().collectAsState(initial = emptyList())
    val barcodes by repository.getAllBarcodes().collectAsState(initial = emptyList())

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            AppScreen.AlarmList -> {
                AlarmListScreen(
                    alarms = alarms,
                    barcodes = barcodes,
                    onToggleAlarm = { id, enabled ->
                        scope.launch { repository.setAlarmEnabled(id, enabled) }
                    },
                    onDeleteAlarm = { id ->
                        scope.launch { repository.deleteAlarm(id) }
                    },
                    onEditAlarm = { alarm ->
                        currentScreen = AppScreen.CreateEdit(alarm)
                    },
                    onCreateAlarm = {
                        currentScreen = AppScreen.CreateEdit(null)
                    },
                    onTriggerTestAlarm = { alarm ->
                        alarmController.triggerAlarm(alarm)
                        currentScreen = AppScreen.Ringing(alarm)
                    }
                )
            }

            is AppScreen.CreateEdit -> {
                CreateEditAlarmScreen(
                    initialAlarm = screen.alarm,
                    barcodes = barcodes,
                    onSaveAlarm = { savedAlarm ->
                        scope.launch {
                            repository.saveAlarm(savedAlarm)
                            currentScreen = AppScreen.AlarmList
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
                                currentScreen = AppScreen.BarcodeMission(screen.alarm, target)
                            }
                            MissionType.MATH -> {
                                currentScreen = AppScreen.MathMission(screen.alarm)
                            }
                            MissionType.SHAKE, MissionType.NONE -> {
                                alarmController.dismissAlarm(
                                    AlarmDismissalReason.MissionVerified(screen.alarm.missionType)
                                )
                                currentScreen = AppScreen.Success("Standard Dismissal Completed")
                            }
                        }
                    },
                    onEmergencyDismiss = {
                        alarmController.dismissAlarm(AlarmDismissalReason.EmergencyHoldCompleted)
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
                        alarmController.dismissAlarm(
                            AlarmDismissalReason.MissionVerified(MissionType.MATH, "3 Problems Solved")
                        )
                        currentScreen = AppScreen.Success("Mental Math Verified // 3 Problems Solved")
                    }
                )
            }

            is AppScreen.BarcodeMission -> {
                val barcodeVm = remember(screen.alarm.id) {
                    BarcodeMissionViewModel(
                        alarm = screen.alarm,
                        barcodeRepository = repository,
                        alarmController = alarmController,
                        initialTarget = screen.targetBarcode
                    )
                }
                BarcodeMissionScreen(
                    viewModel = barcodeVm,
                    onFallbackEmergency = {
                        // Safe fallback option
                        currentScreen = AppScreen.MathMission(screen.alarm)
                    }
                )

                // If alarm was dismissed, navigate to success
                val vmState by barcodeVm.uiState.collectAsState()
                if (vmState.isDismissed) {
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
