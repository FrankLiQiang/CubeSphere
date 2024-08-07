package com.frank.cubesphere

import android.graphics.Point
import android.view.WindowManager

object Common {
    var _screenWidth = 0
    var _screenHeight = 0
    var Eye = PointXYZ()
    var square = 0.0
    var times = 0f
    var angleY = 0.0
    var DepthZ = 0f
    fun getScreenSize(windowManager: WindowManager) {
        val outSize = Point()
        windowManager.defaultDisplay.getRealSize(outSize)
        _screenWidth = outSize.x
        _screenHeight = outSize.y
    }

    fun resetPointF(thisP: PointF, P: PointXYZ) {
        thisP.x = (P.x - Eye.x) / (Eye.z - P.z) * (Eye.z - DepthZ) + Eye.x
        thisP.y = (P.y - Eye.y) / (Eye.z - P.z) * (Eye.z - DepthZ) + Eye.y
    }

    fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        square = ((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble()
        return Math.sqrt(square)
    }

    fun getNewSizePoint(
        firstDistance: Float, lastDistance: Float,
        StartX: Float, StartY: Float, EndX: Float, EndY: Float,
        ObjCenter: PointXYZ, pOld: PointXYZ, pNew: PointXYZ, isChange: Boolean
    ) {
        pNew.reset(pOld.x - ObjCenter.x, pOld.y - ObjCenter.y, pOld.z - ObjCenter.z)
        times = lastDistance / firstDistance
        if (isChange) {
            pNew.reset(pNew.x * times, pNew.y * times, pNew.z * times)
        }
        getDistance(StartX, StartY, EndX, EndY)
        val a2 = square
        val b = getDistance(ObjCenter.x, ObjCenter.y, StartX, StartY)
        val b2 = square
        val c = getDistance(ObjCenter.x, ObjCenter.y, EndX, EndY)
        val c2 = square
        val Acos = (b2 + c2 - a2) / (2 * b * c)
        var angleA: Double
        angleA = if (Math.abs(Acos) > 1) {
            0.0
        } else {
            Math.acos(Acos)
        }
        val dir = getDir(ObjCenter.x, ObjCenter.y, StartX, StartY, EndX, EndY)
        angleA *= dir.toDouble()
        val x = pNew.x * Math.cos(angleA) - pNew.y * Math.sin(angleA)
        val y = pNew.y * Math.cos(angleA) + pNew.x * Math.sin(angleA)
        pNew.reset(x.toFloat(), y.toFloat(), pNew.z)
        pNew.reset(pNew.x + ObjCenter.x, pNew.y + ObjCenter.y, pNew.z + ObjCenter.z)
    }

    fun getDir(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Int {
        return if (x1 * y2 + x2 * y3 + x3 * y1 - x1 * y3 - x2 * y1 - x3 * y2 > 0) 1 else -1
    }

    fun getNewPoint(
        StartX: Float, StartY: Float, EndX: Float, EndY: Float,
        ObjCenter: PointXYZ, pOld: PointXYZ, pNew: PointXYZ
    ) {
        pNew.reset(pOld.x - ObjCenter.x, pOld.y - ObjCenter.y, pOld.z - ObjCenter.z)
        var a2 = ((StartX - EndX) * (StartX - EndX)).toDouble()
        var b = getDistance(ObjCenter.x, ObjCenter.z, StartX, DepthZ)
        var b2 = square
        var c = getDistance(ObjCenter.x, ObjCenter.z, EndX, DepthZ)
        var c2 = square
        var Acos = (b2 + c2 - a2) / (2 * b * c)
        var angleA: Double
        angleA = if (Math.abs(Acos) > 1) {
            0.0
        } else {
            Math.acos(Acos) * 2
        }
        if (EndX > StartX) angleA *= -1.0
        angleY = angleA
        val x = pNew.x * Math.cos(angleA) - pNew.z * Math.sin(angleA)
        var z = pNew.z * Math.cos(angleA) + pNew.x * Math.sin(angleA)
        pNew.reset(x.toFloat(), pNew.y, z.toFloat())

        //---------
        a2 = ((StartY - EndY) * (StartY - EndY)).toDouble()
        b = getDistance(ObjCenter.y, ObjCenter.z, StartY, DepthZ)
        b2 = square
        c = getDistance(ObjCenter.y, ObjCenter.z, EndY, DepthZ)
        c2 = square
        Acos = (b2 + c2 - a2) / (2 * b * c)
        angleA = if (Math.abs(Acos) > 1) {
            0.0
        } else {
            Math.acos(Acos) * 2
        }
        if (EndY > StartY) angleA *= -1.0
        val y = pNew.y * Math.cos(angleA) - pNew.z * Math.sin(angleA)
        z = pNew.z * Math.cos(angleA) + pNew.y * Math.sin(angleA)
        pNew.reset(pNew.x, y.toFloat(), z.toFloat())
        pNew.reset(pNew.x + ObjCenter.x, pNew.y + ObjCenter.y, pNew.z + ObjCenter.z)
    }

    class PointXYZ {
        var x = 0f
        var y = 0f
        var z = 0f

        constructor()
        constructor(X: Float, Y: Float, Z: Float) {
            x = X
            y = Y
            z = Z
        }

        fun reset(X: Float, Y: Float, Z: Float) {
            x = X
            y = Y
            z = Z
        }
    }

    class PointF internal constructor() {
        var x = 0f
        var y = 0f
    }
}