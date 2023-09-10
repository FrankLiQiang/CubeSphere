package com.frank.cubesphere

import android.os.Message
import java.util.Timer
import java.util.TimerTask

private var taskA: A? = null
private var taskB: B? = null
private var delayA = 0L
private var delayB = 0L
var isStopTimer = true
var _timer: Timer? = null
var isDrawCube = true

internal class A : TimerTask() {
    override fun run() {
        if (!isStopTimer) {
            isDrawCube = true
            draw()
        }
    }
}

internal class B : TimerTask() {
    override fun run() {
        if (!isStopTimer) {
            isDrawCube = false
            draw()
        }
    }
}

fun draw() {
    if (!isStopTimer) {
        if (isDrawCube) {
            if (delayA > 0) {
                resetCube()
            }
            delayA = 0
            turnCube()
            taskB = B()
            Timer().schedule(taskB, 0)
        } else {
            if (delayB > 0) {
                createBall()
            }
            delayB = 0
            turnBall()
            taskA = A()
            Timer().schedule(taskA, 0)
        }
    }
}

fun setDelay(iDelay: Long) {
    delayA = iDelay
    delayB = iDelay
}

fun startDrawTimer() {
    _timer = Timer()
    isStopTimer = false
    taskA = A()
    _timer!!.schedule(taskA, delayA)
}

fun stopDrawTimer() {
    isStopTimer = true
    if (taskA != null) {
        taskA!!.cancel()
        taskA = null
    }
    if (taskB != null) {
        taskB!!.cancel()
        taskB = null
    }
}

