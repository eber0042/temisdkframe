package com.temi.greetmi

import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnConversationStatusChangedListener
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Singleton
import kotlin.system.exitProcess

enum class DetectionState(val state: Int) { // Why is it like this?
    DETECTED(state = 2),
    LOST(state = 1),
    IDLE(state = 0);

    companion object {
        fun fromState(state: Int): DetectionState? = entries.find { it.state == state }
    }
}

data class TtsStatus(val status: TtsRequest.Status, val speech: String)

data class MovementStatus(val type: String, val status: String)

enum class SequencePlayStatus(val state: Int) {
    PLAYING(state = 2),
    PREPARING(state = 1),
    IDLE(state = 0),
    ERROR(state = -1);

    companion object {
        fun fromState(state: Int): SequencePlayStatus? = entries.find { it.state == state }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RobotModule {
    @Provides
    @Singleton
    fun provideRobotController() = RobotController()
}

class RobotController():
    OnRobotReadyListener,
    OnDetectionStateChangedListener,
    Robot.TtsListener,
    // Adding a listener to check if the robot stops rotating
    OnMovementStatusChangedListener,
    OnSequencePlayStatusChangedListener
{
    private val robot = Robot.getInstance() //This is needed to reference the data coming from Temi
    var initialYaw: Float = 0f

    // Need to figure out how this stuff work better
    private val _detectionState = MutableStateFlow(DetectionState.IDLE) // This will create a variable and add it to the stateFlow with default values
    val detectionState = _detectionState.asStateFlow() // This will assign the values from stateflow and add it to the variable

    private val _ttsStatus = MutableStateFlow( TtsStatus(status = TtsRequest.Status.COMPLETED, speech = "") )
    val ttsStatus = _ttsStatus.asStateFlow()

    private val _movementStatus = MutableStateFlow( MovementStatus(type = "", status = "") )
    val movementStatus = _movementStatus.asStateFlow()

    private val _sequencePlayStatus = MutableStateFlow( SequencePlayStatus.IDLE )
    val sequencePlayStatus = _sequencePlayStatus.asStateFlow()


    init {
        robot.addOnRobotReadyListener(this)
        robot.addOnDetectionStateChangedListener(this)
        robot.addTtsListener(this)
        // Added my new listener here
        robot.addOnMovementStatusChangedListener(this)
        robot.addOnSequencePlayStatusChangedListener(this)
    }

    fun robotSpeak(speech: String) { // Function that will get the robot to speak
        val ttsRequest = TtsRequest.create(speech = speech, isShowOnConversationLayer = true, showAnimationOnly = true) // Set values to be used

        robot.speak(ttsRequest) // Send the set values to the Temi
    }

    fun turnBy(degrees: Int, speed: Float) {
        robot.turnBy(degrees, speed)
    }

    fun tiltAngle(degree: Int) {
        robot.tiltAngle(degree)
    }


    fun getPosition(): Float
    {
        return robot.getPosition().yaw
    }

    /**
     * Called when connection with robot was established.
     *
     * @param isReady `true` when connection is open. `false` otherwise.
     */
    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) return

//        initialYaw = robot.getPosition().yaw

        robot.setDetectionModeOn(on = true, distance = 1.0f) // Set how far it can detect stuff
        robot.setKioskModeOn(on = false)
    }

    /**
     * Available status:
     *  * [OnDetectionStateChangedListener.IDLE]
     *  * [OnDetectionStateChangedListener.LOST]
     *  * [OnDetectionStateChangedListener.DETECTED]
     *
     *
     * @param state Current state.
     */
    override fun onDetectionStateChanged(state: Int) {
        _detectionState.update {
            DetectionState.fromState(state = state) ?: return@update it
        }
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        Log.i("onTtsStatusChanged", "status: ${ttsRequest.status} speech: ${ttsRequest.speech}")
        _ttsStatus.update {
            TtsStatus(status = ttsRequest.status, speech = ttsRequest.speech)
        }
    }

    override fun onMovementStatusChanged(type: String, status: String) {
        _movementStatus.update {
            MovementStatus(type = type, status = status)
        }
    }

    override fun onSequencePlayStatusChanged(state: Int) {
        _sequencePlayStatus.update {
            SequencePlayStatus.fromState(state = state) ?: return@update it
        }
    }
}