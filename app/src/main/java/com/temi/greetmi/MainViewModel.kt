package com.temi.greetmi

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robotemi.sdk.TtsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random
import kotlin.math.*
import kotlin.system.exitProcess

@HiltViewModel
class MainViewModel @Inject constructor(
    private val robotController: RobotController
) : ViewModel() {
    private val detectionState =
        robotController.detectionState //This stores information on the detected state
    private val ttsStatus = robotController.ttsStatus // Current speech state
    private val movementStatus: StateFlow<MovementStatus> = robotController.movementStatus
    private val sequencePlayStatus = robotController.sequencePlayStatus

    private var initialDegree: Int = 0
    private var turned: Boolean = false

    init {
        viewModelScope.launch {
            detectionState.collect { detectionState: DetectionState ->
                val calendar = Calendar.getInstance()
//                val hour = calendar.get(Calendar.HOUR_OF_DAY) // Will get the hour in 24h format

                var hour = 7
                val responseRefreshRate = 2000L
                when {
                    hour in 7..9 -> {
                        Log.i("detectionState", "state: ${detectionState.state}")
                        if (detectionState == DetectionState.DETECTED && ttsStatus.value.status == TtsRequest.Status.COMPLETED) { // If Temi detects someone
                            Log.i("check", "hi")
                            textModelChoice(hour) // Say a text line using the made model
                            while (ttsStatus.value.status != TtsRequest.Status.COMPLETED) // Wait until Temi done talking
                                delay(100L)
                        }
                        delay(responseRefreshRate)
                    }

                    hour in 10..11 -> {
                        Log.i("detectionState", "state: ${detectionState.state}")
                        if (detectionState == DetectionState.DETECTED) { // If Temi detects someone
                            if (ttsStatus.value.status == TtsRequest.Status.COMPLETED) { // If Temi not talking )
                                textModelChoice(hour) // Say a text line using the made model
                                delay(100)
                                while (ttsStatus.value.status != TtsRequest.Status.COMPLETED) {
                                    delay(100)
                                }// Wait until Temi done talking
                                while (true) {
                                    if (!(movementStatus.value.status == "start")) {
                                        robotController.turnBy(-60, 1f)
                                    } // On do this if system not started
                                    delay(100) // Add a delay for updating of listener
                                    Log.i(
                                        "movementStatus",
                                        "1current: ${movementStatus.value.status}"
                                    )
                                    if (!(movementStatus.value.status in listOf(
                                            "start",
                                            "abort",
                                            ""
                                        )) && movementStatus.value.type == "turnBy"
                                    ) {
                                        break
                                    } // If system is not moving then do not break
                                }
                                while (ttsStatus.value.status != TtsRequest.Status.COMPLETED) {
                                    delay(100)
                                }// Wait until Temi done talking
                                robotController.robotSpeak(speech = "Please go to the counter to your left to check in")
                            }
                            while (movementStatus.value.status != "complete") {
                                delay(100)
                            } // wait until the Temi has finished Turning
                            Log.i("movementStatus", "3current: ${movementStatus.value.status}")
                            delay(3900)
                            while (true) {
                                if (!(movementStatus.value.status == "start")) {
                                    robotController.turnBy(60, 1f)
                                }
                                delay(100)
                                Log.i("movementStatus", "2current: ${movementStatus.value.status}")
                                if (!(movementStatus.value.status in listOf(
                                        "start",
                                        "abort",
                                        ""
                                    )) && movementStatus.value.type == "turnBy"
                                ) {
                                    break
                                }
                            }

                            while (movementStatus.value.status != "complete") {
                                delay(100)
                            } // wait until the Temi has finished Turning
                            delay(responseRefreshRate)
                        }
                    }
                    hour in 12..16 -> {
                        if (detectionState == DetectionState.DETECTED && ttsStatus.value.status == TtsRequest.Status.COMPLETED) { // If Temi detects someone
                            textModelChoice(hour) // Say a text line using the made model
                            while (ttsStatus.value.status != TtsRequest.Status.COMPLETED) // Wait until Temi done talking
                                delay(100L)
                        }
                        delay(responseRefreshRate)
                    }
                    else -> {
                        if (detectionState == DetectionState.DETECTED && ttsStatus.value.status == TtsRequest.Status.COMPLETED) { // If Temi detects someone
                            textModelChoice(hour) // Say a text line using the made model
                            while (ttsStatus.value.status != TtsRequest.Status.COMPLETED) // Wait until Temi done talking
                                delay(100L)
                        }
                        delay(responseRefreshRate)
                    }
                }
            }
        }

        viewModelScope.launch {
            sequencePlayStatus.collect { sequencePlayStatus: SequencePlayStatus ->
                if(sequencePlayStatus == SequencePlayStatus.PLAYING || sequencePlayStatus == SequencePlayStatus.PREPARING) {
                    Log.i ("CHECK", sequencePlayStatus.toString())
                    // robotController.closeApplication()
                    System.exit(0)
                    exitProcess(0)
                }
            }
        }

        viewModelScope.launch {
            ttsStatus.collect { ttsStatus: TtsStatus ->
                if (ttsStatus.status in listOf(TtsRequest.Status.PENDING, TtsRequest.Status.PROCESSING, TtsRequest.Status.STARTED)) {
                    robotController.tiltAngle(degree = 40);
                }
                else {
                    robotController.tiltAngle(degree = 5);
                }
            }
        }
    }

    private fun textModelChoice(hour: Int) {
        // Log.d("Time", "$hour") // Used this to check the format of the time

        var companyName = "the Gear"
        var choice = Random.nextInt(1, 5 + 1)

        when {
            hour in 8..9 -> { // Morning
                Log.d("Time", "Morn")
                when (choice) {
                    1 -> robotController.robotSpeak(speech = "Good morning! I hope you have a wonderful day at work.")
                    2 -> robotController.robotSpeak(speech = "It's a beautiful morning. Please enjoy your day!")
                    3 -> robotController.robotSpeak(speech = "Rise and shine! Let's make today great!")
                    4 -> robotController.robotSpeak(speech = "Good morning! Don't forget to smile!")
                    5 -> robotController.robotSpeak(speech = "Early birds get the worm! Let's get going!")
                }
            }

            hour in 10..11 -> { // Late Morning
                when (choice) {
                    1 -> robotController.robotSpeak(speech = "Welcome to $companyName! We're excited to have you here!")
                    2 -> robotController.robotSpeak(speech = "Good day! Hope you're having a great morning at $companyName!")
                    3 -> robotController.robotSpeak(speech = "Hi, I am Temi and welcome to $companyName. Nice to see you!")
                    4 -> robotController.robotSpeak(speech = "Greetings! I hope you enjoy your time at $companyName!")
                    5 -> robotController.robotSpeak(speech = "Hello and welcome to $companyName! We’re glad to see you!")
                }
            }

            hour in 12..16 -> { // Afternoon
                when (choice) {
                    1 -> robotController.robotSpeak(speech = "Hello! It's great to see you!")
                    2 -> robotController.robotSpeak(speech = "Hi there! Nice to meet you!")
                    3 -> robotController.robotSpeak(speech = "Greetings! I hope you're enjoying your day!")
                    4 -> robotController.robotSpeak(speech = "Welcome! So glad you're here!")
                    5 -> robotController.robotSpeak(speech = "Good to see you! Let's have a great time!")
                }
            }

            else -> {
                when (choice) {
                    1 -> robotController.robotSpeak(speech = "Good night, everyone! Sleep well!")
                    2 -> robotController.robotSpeak(speech = "Wishing you a restful night!")
                    3 -> robotController.robotSpeak(speech = "Time to wind down. Good night!")
                    4 -> robotController.robotSpeak(speech = "As the day ends, good night to all!")
                    5 -> robotController.robotSpeak(speech = "Good night! Looking forward to tomorrow!")
                }

            }
        }
    }

//    fun onClickTurnButton() {
//        // TODO: robotController.turn()
//    }

}


//*******************************************
//                if (detectionState == DetectionState.DETECTED && ttsStatus.value.status == TtsRequest.Status.COMPLETED) {
//                    textModelChoice()
//                    if (turned) {
//                        turned = !turned
////                        var currentDegree = 180 + Math.toDegrees(robotController.getPosition().toDouble()).toInt()
////                        var differentInDegree = currentDegree - initialDegree
//
////                        Log.i("turnedStatus", turned.toString())
////                        Log.i("initialDegree", initialDegree.toString())
////                        Log.i("currentDegree", currentDegree.toString())
////                        Log.i("differentInDegree", differentInDegree.toString())
////                        robotController.turnBy(differentInDegree, 1f)
//                    }
//                    else{
//                        turned = !turned
//
////                        Log.i("turnedStatus", turned.toString())
////
////                        robotController.turnBy(179, 1f)
//
//
//                    }
//                }