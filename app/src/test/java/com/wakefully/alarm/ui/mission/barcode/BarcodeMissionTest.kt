package com.wakefully.alarm.ui.mission.barcode

import com.wakefully.alarm.data.InMemoryWakefullyRepository
import com.wakefully.alarm.domain.Alarm
import com.wakefully.alarm.domain.MissionType
import com.wakefully.alarm.domain.RegisteredBarcode
import com.wakefully.alarm.service.AlarmController
import com.wakefully.alarm.service.AlarmDismissalReason
import com.wakefully.alarm.service.DefaultAlarmController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeMissionTest {

    private lateinit var repository: InMemoryWakefullyRepository
    private lateinit var alarmController: DefaultAlarmController
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val shampooBarcode = RegisteredBarcode(
        id = 101L,
        name = "Bathroom Shampoo",
        value = "4006381333931",
        format = 32 // FORMAT_EAN_13
    )

    private val coffeeBarcode = RegisteredBarcode(
        id = 102L,
        name = "Kitchen Coffee Box",
        value = "012345678905",
        format = 32 // FORMAT_EAN_13
    )

    private val alarmShampoo = Alarm(
        id = 1L,
        hourOfDay = 7,
        minute = 0,
        missionType = MissionType.BARCODE,
        barcodeId = 101L,
        label = "Morning Shower"
    )

    private val alarmCoffee = Alarm(
        id = 2L,
        hourOfDay = 7,
        minute = 30,
        missionType = MissionType.BARCODE,
        barcodeId = 102L,
        label = "Coffee Time"
    )

    @Before
    fun setUp() = runTest {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        repository = InMemoryWakefullyRepository()
        alarmController = DefaultAlarmController()

        repository.saveBarcode(shampooBarcode)
        repository.saveBarcode(coffeeBarcode)
        repository.saveAlarm(alarmShampoo)
        repository.saveAlarm(alarmCoffee)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun createViewModel(alarm: Alarm, timeProvider: () -> Long = { 1000L }): BarcodeMissionViewModel {
        alarmController.triggerAlarm(alarm)
        val initialTarget = when (alarm.barcodeId) {
            shampooBarcode.id -> shampooBarcode
            coffeeBarcode.id -> coffeeBarcode
            else -> null
        }
        val vm = BarcodeMissionViewModel(
            alarm = alarm,
            barcodeRepository = repository,
            alarmController = alarmController,
            timeProvider = timeProvider,
            initialTarget = initialTarget,
            dispatcher = testDispatcher
        )
        return vm
    }

    @Test
    fun injectCorrectBarcode_assertDismissCalledOnce() {
        // Inject the correct barcode: assert dismiss is called once.
        val vm = createViewModel(alarmShampoo)
        assertTrue("Alarm must be ringing", alarmController.isRinging.value)

        // Frame 1: Debouncing
        vm.onScanFrame("4006381333931", 32)
        assertFalse("Must not dismiss on 1st frame (needs 2 consecutive frames)", vm.uiState.value.isDismissed)
        assertTrue("Alarm still ringing after frame 1", alarmController.isRinging.value)

        // Frame 2: Confirmed match on 2nd consecutive frame
        vm.onScanFrame("4006381333931", 32)
        assertTrue("Dismiss called after 2nd consecutive frame", vm.uiState.value.isDismissed)
        assertFalse("Alarm stopped ringing", alarmController.isRinging.value)
        assertFalse("Alarm sound stopped", alarmController.soundPlaying.value)
    }

    @Test
    fun injectDifferentBarcodeValue_assertNotDismissedAndErrorShown() {
        // Inject a different barcode value: assert dismiss is NOT called and the error state is shown.
        val vm = createViewModel(alarmShampoo)

        // Frame 1
        vm.onScanFrame("9999999999999", 32)
        // Frame 2
        vm.onScanFrame("9999999999999", 32)

        assertFalse("Alarm must NOT be dismissed on wrong barcode", vm.uiState.value.isDismissed)
        assertTrue("Alarm must still be ringing", alarmController.isRinging.value)
        assertTrue("Error state must be shown", vm.uiState.value.isError)
        assertEquals("That's not the right one", vm.uiState.value.errorMessage)
    }

    @Test
    fun injectSameValueDifferentFormat_assertNotDismissed() {
        // Inject a barcode with the same value but a different format: assert NOT dismissed.
        val vm = createViewModel(alarmShampoo)

        // Format 256 is QR_CODE instead of EAN_13 (32)
        vm.onScanFrame("4006381333931", 256)
        vm.onScanFrame("4006381333931", 256)

        assertFalse("Different format must NOT dismiss alarm", vm.uiState.value.isDismissed)
        assertTrue("Alarm must keep ringing", alarmController.isRinging.value)
        assertTrue("Error state displayed for format mismatch", vm.uiState.value.isError)
    }

    @Test
    fun injectNullEmptyWhitespace_assertIgnored() {
        // Inject null/empty/whitespace-only value: assert ignored.
        val vm = createViewModel(alarmShampoo)

        vm.onScanFrame(null, 32)
        vm.onScanFrame("", 32)
        vm.onScanFrame("   ", 32)
        vm.onScanFrame("\t\n", 32)

        assertFalse("Ignored scans must not dismiss", vm.uiState.value.isDismissed)
        assertFalse("Ignored scans must not trigger error banner", vm.uiState.value.isError)
        assertTrue("Alarm keeps ringing continuously", alarmController.isRinging.value)
    }

    @Test
    fun injectWrongWrongThenCorrect_assertDismissedOnlyAfterCorrect() {
        // Inject wrong, wrong, then correct: assert dismissed only after the correct one.
        val vm = createViewModel(alarmShampoo)

        // Inject wrong barcode twice
        vm.onScanFrame("8888888888888", 32)
        vm.onScanFrame("8888888888888", 32)
        assertTrue("Error shown on wrong barcode", vm.uiState.value.isError)
        assertFalse("Not dismissed on wrong barcode", vm.uiState.value.isDismissed)
        assertTrue("Alarm ringing", alarmController.isRinging.value)

        // Inject another wrong barcode twice
        vm.onScanFrame("7777777777777", 32)
        vm.onScanFrame("7777777777777", 32)
        assertTrue("Error shown on 2nd wrong barcode", vm.uiState.value.isError)
        assertFalse("Still not dismissed", vm.uiState.value.isDismissed)
        assertTrue("Alarm ringing", alarmController.isRinging.value)

        // Now inject the CORRECT barcode twice
        vm.onScanFrame("4006381333931", 32)
        vm.onScanFrame("4006381333931", 32)

        assertTrue("Dismissed only after correct barcode", vm.uiState.value.isDismissed)
        assertFalse("Error cleared", vm.uiState.value.isError)
        assertFalse("Alarm ringing stopped", alarmController.isRinging.value)
    }

    @Test
    fun injectSameWrongBarcode20TimesQuickly_assertNoDismissAndNoCrash() {
        // Inject the same wrong barcode 20 times quickly: assert no dismiss and no crash.
        val vm = createViewModel(alarmShampoo)

        for (frame in 1..20) {
            vm.onScanFrame("9999999999999", 32)
            assertFalse("Frame $frame must not dismiss alarm", vm.uiState.value.isDismissed)
            assertTrue("Alarm still ringing on frame $frame", alarmController.isRinging.value)
        }
        assertTrue("Error state preserved", vm.uiState.value.isError)
    }

    @Test
    fun twoAlarmsWithTwoDifferentBarcodes_assertAlarmAOnlyAcceptsBarcodeA() {
        // Two alarms with two different registered barcodes: assert alarm A only accepts barcode A.
        val vmAlarmA = createViewModel(alarmShampoo)
        val vmAlarmB = createViewModel(alarmCoffee)

        // Try scanning Coffee barcode on Alarm A (shampoo alarm)
        vmAlarmA.onScanFrame(coffeeBarcode.value, coffeeBarcode.format)
        vmAlarmA.onScanFrame(coffeeBarcode.value, coffeeBarcode.format)
        assertFalse("Alarm A must NOT dismiss on Alarm B's barcode", vmAlarmA.uiState.value.isDismissed)

        // Scan Shampoo barcode on Alarm A
        vmAlarmA.onScanFrame(shampooBarcode.value, shampooBarcode.format)
        vmAlarmA.onScanFrame(shampooBarcode.value, shampooBarcode.format)
        assertTrue("Alarm A successfully dismissed with Barcode A", vmAlarmA.uiState.value.isDismissed)

        // Reset alarm controller to ring Alarm B
        alarmController.triggerAlarm(alarmCoffee)
        assertTrue(alarmController.isRinging.value)

        // Try scanning Shampoo barcode on Alarm B (coffee alarm)
        vmAlarmB.onScanFrame(shampooBarcode.value, shampooBarcode.format)
        vmAlarmB.onScanFrame(shampooBarcode.value, shampooBarcode.format)
        assertFalse("Alarm B must NOT dismiss on Alarm A's barcode", vmAlarmB.uiState.value.isDismissed)
        assertTrue("Alarm B still ringing", alarmController.isRinging.value)

        // Scan Coffee barcode on Alarm B
        vmAlarmB.onScanFrame(coffeeBarcode.value, coffeeBarcode.format)
        vmAlarmB.onScanFrame(coffeeBarcode.value, coffeeBarcode.format)
        assertTrue("Alarm B successfully dismissed with Barcode B", vmAlarmB.uiState.value.isDismissed)
        assertFalse("Alarm ringing stopped", alarmController.isRinging.value)
    }

    @Test
    fun processDeathOrRotationDuringScanning_assertAlarmStillRinging() {
        // Process-death/rotation during scanning: assert the alarm is still ringing and still requires the correct barcode.
        val vm1 = createViewModel(alarmShampoo)
        assertTrue(alarmController.isRinging.value)

        // Simulate activity destroy and recreation (ViewModel recreation with same alarm)
        val vm2 = BarcodeMissionViewModel(
            alarm = alarmShampoo,
            barcodeRepository = repository,
            alarmController = alarmController,
            initialTarget = shampooBarcode,
            dispatcher = testDispatcher
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Alarm is still ringing after recreation", alarmController.isRinging.value)
        assertTrue("Alarm sound still active", alarmController.soundPlaying.value)
        assertFalse("New ViewModel not dismissed", vm2.uiState.value.isDismissed)
        assertEquals("Target barcode preserved", shampooBarcode.value, vm2.uiState.value.targetBarcode?.value)

        // Incorrect barcode still fails
        vm2.onScanFrame("0000000000000", 32)
        vm2.onScanFrame("0000000000000", 32)
        assertFalse("Wrong barcode fails on recreated VM", vm2.uiState.value.isDismissed)

        // Correct barcode dismisses
        vm2.onScanFrame(shampooBarcode.value, shampooBarcode.format)
        vm2.onScanFrame(shampooBarcode.value, shampooBarcode.format)
        assertTrue("Correct barcode dismisses recreated VM", vm2.uiState.value.isDismissed)
        assertFalse("Alarm stopped", alarmController.isRinging.value)
    }

    @Test
    fun deleteBarcodeAttachedToAlarm_throwsIllegalStateException() = runTest {
        // Block deleting a barcode that is attached to an active alarm
        var exceptionThrown = false
        try {
            repository.deleteBarcode(shampooBarcode.id)
        } catch (e: IllegalStateException) {
            exceptionThrown = true
            assertTrue(e.message?.contains("attached to Alarm") == true)
        }
        assertTrue("Must block deleting attached barcode", exceptionThrown)
        assertNotNull("Barcode still in repository", repository.getBarcodeById(shampooBarcode.id))
    }
}
