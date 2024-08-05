package com.frank.cubesphere

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
                resetBall()
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

//https://developer.android.com/reference/kotlin/android/view/SurfaceView
//https://qiita.com/ymshun/items/a1447bdfcea8ef24d765
//https://stackoverflow.com/questions/19019825/android-setpixels-explanation-and-example
//https://weiwangqiang.github.io/2020/12/07/surfaceview-profile-animator/
//https://qiita.com/yoshihiro-kato/items/7ad1202891e183df4e86
//https://www.bing.com/search?q=kotlin+bitmap.setPixels+%E5%87%BD%E6%95%B0&qs=n&form=QBRE&sp=-1&lq=0&pq=kotlin+bitmap.setpixels+%E5%87%BD%E6%95%B0&sc=11-26&sk=&cvid=53D52BD46F7D4D378849CFEBCCA1EFD0&ghsh=0&ghacc=0&ghpl=
//https://www.bing.com/search?pglt=41&q=Kotlin+%E5%88%A9%E7%94%A8Canvas+%E5%92%8C+SurfaceView+%E5%B9%B3%E6%BB%91%E6%98%BE%E7%A4%BA%E5%8A%A8%E7%94%BB&cvid=829c16feb2d0431faeb84164d5dffcf5&gs_lcrp=EgZjaHJvbWUqBggAEEUYOzIGCAAQRRg7MggIARDpBxj8VdIBCDI0NzZqMGoxqAIAsAIA&FORM=ANNAB1&adppc=EDGEDBB&PC=EDGEDBB
//https://yskskt.hatenablog.com/entry/2023/10/03/215430
