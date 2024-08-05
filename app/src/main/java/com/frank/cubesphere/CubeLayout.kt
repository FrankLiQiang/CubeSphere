package com.frank.cubesphere

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import com.frank.cubesphere.Common.getDistance
import com.frank.cubesphere.Common.getNewPoint
import com.frank.cubesphere.Common.getNewSizePoint
import com.frank.cubesphere.Common.resetPointF
import java.nio.ByteBuffer

lateinit var newCubeByteArray: ByteArray
var CubeCenter: Common.PointXYZ = Common.PointXYZ()
var StartX = 0f
var StartY = 0f
var EndX = 0f
var EndY = 0f
var cubeWidth = 0
val FaceCount = 6
var isSingle = true
val PointCount = 8
var edgeLength = 0f
lateinit var pX: FloatArray
lateinit var pY: FloatArray
lateinit var pZ: FloatArray
lateinit var P: Array<Common.PointXYZ?>
lateinit var oldP: Array<Common.PointXYZ?>
lateinit var p: Array<Common.PointF?>
val face = arrayOf(
    intArrayOf(0, 3, 2, 1),
    intArrayOf(3, 7, 6, 2),
    intArrayOf(1, 2, 6, 5),
    intArrayOf(0, 1, 5, 4),
    intArrayOf(4, 5, 6, 7),
    intArrayOf(0, 4, 7, 3)
)
val originalCubeByteArray = arrayOfNulls<ByteArray>(6)
var newCubeBMP: Bitmap? = null
var diff0 = 0f
var diff1 = 0f
var diff2 = 0f
var diff3 = 0f
var diff5 = 0f
var facePointsX = Array(3) { FloatArray(4) }
var facePointsY = Array(3) { FloatArray(4) }
var firstDistance = 0f
var lastDistance = 0f
var indexCube by mutableStateOf(0L)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun drawCube(modifier: Modifier = Modifier) {
    if (indexCube < -1) return
    Image(
        bitmap = newCubeBMP!!.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        stopDrawTimer()
                        StartX = it.x
                        StartY = it.y
                        saveOldPoints()
                        firstDistance = 0f
                        isSingle = it.pointerCount == 1
                    }
                    MotionEvent.ACTION_MOVE -> {
                        EndX = it.x
                        EndY = it.y
                        if (it.pointerCount == 2) {
                            if (firstDistance == 0f) {
                                var tmp: Float
                                var i = 0
                                while (i < PointCount) {
                                    tmp = getDistance(
                                        p[i]!!.x,
                                        p[i]!!.y,
                                        CubeCenter.x,
                                        CubeCenter.y
                                    ).toFloat()
                                    if (tmp > firstDistance) {
                                        firstDistance = tmp
                                    }
                                    i++
                                }
                            }
                            isSingle = false
                            lastDistance = getDistance(
                                it.getX(0),
                                it.getY(0),
                                it.getX(1),
                                it.getY(1)
                            ).toFloat()
                            lastDistance /= 2f
                            if (convert2()) {
                                drawSolid()
                                indexCube = 1 - indexCube
                            }
                        } else if (it.pointerCount == 1 && isSingle) {
                            convert()
                            drawSolid()
                            indexCube = 1 - indexCube
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        setDelay(5000)
                        startDrawTimer()
                    }
                }
                true
            }
    )
}

fun resetCube() {
    resetCube0()
    for (i in 0 until PointCount) {
        oldP[i]!!.reset(pX[i], pY[i], pZ[i])
    }
    StartX = 0f
    StartY = 0f
    EndX = 0f
    EndY = 0f
    convert()
}

private fun resetCube0() {
    pX[0] = CubeCenter.x
    pX[2] = pX[0]
    pX[4] = pX[0]
    pX[6] = pX[0]
    pX[3] = CubeCenter.x - diff5
    pX[7] = CubeCenter.x - diff5
    pX[1] = CubeCenter.x + diff5
    pX[5] = CubeCenter.x + diff5
    pY[0] = CubeCenter.y - diff1
    pY[6] = CubeCenter.y + diff1
    pY[2] = CubeCenter.y + diff0
    pY[5] = CubeCenter.y + diff0
    pY[7] = CubeCenter.y + diff0
    pY[1] = CubeCenter.y - diff0
    pY[3] = CubeCenter.y - diff0
    pY[4] = CubeCenter.y - diff0
    pZ[0] = CubeCenter.z
    pZ[6] = CubeCenter.z
    pZ[2] = CubeCenter.z + diff3
    pZ[4] = CubeCenter.z - diff3
    pZ[1] = CubeCenter.z + diff2
    pZ[3] = CubeCenter.z + diff2
    pZ[5] = CubeCenter.z - diff2
    pZ[7] = CubeCenter.z - diff2
}

fun createCube() {
    originalCubeBMP[0] = (mainActivity.resources.getDrawable(R.drawable.dage) as BitmapDrawable).bitmap
    originalCubeBMP[1] = (mainActivity.resources.getDrawable(R.drawable.erge) as BitmapDrawable).bitmap
    originalCubeBMP[2] = (mainActivity.resources.getDrawable(R.drawable.dajie) as BitmapDrawable).bitmap
    originalCubeBMP[3] = (mainActivity.resources.getDrawable(R.drawable.erjie) as BitmapDrawable).bitmap
    originalCubeBMP[4] = (mainActivity.resources.getDrawable(R.drawable.me) as BitmapDrawable).bitmap
    originalCubeBMP[5] = (mainActivity.resources.getDrawable(R.drawable.juan) as BitmapDrawable).bitmap
    cubeWidth = originalCubeBMP[0]!!.width
    pX = FloatArray(PointCount)
    pY = FloatArray(PointCount)
    pZ = FloatArray(PointCount)
    P = arrayOfNulls(PointCount)
    oldP = arrayOfNulls(PointCount)
    p = arrayOfNulls(PointCount)
    edgeLength = Common._screenWidth / 2f
    CubeCenter.reset(Common._screenWidth / 2f, Common._screenHeight / 4f, -edgeLength / 2)
    diff0 = 0.289f * edgeLength
    diff1 = 0.866f * edgeLength
    diff3 = 0.816f * edgeLength
    diff2 = 0.408f * edgeLength
    diff5 = 0.707f * edgeLength
    resetCube0()
    newCubeBMP = Bitmap.createBitmap(
        Common._screenWidth,
        Common._screenHeight / 2,
        Bitmap.Config.ARGB_8888
    )
    bmp2byteCube()
    for (i in 0 until PointCount) {
        P[i] = Common.PointXYZ(pX[i], pY[i], pZ[i])
        oldP[i] = Common.PointXYZ(pX[i], pY[i], pZ[i])
        p[i] = Common.PointF()
    }
    convert()
}

fun bmp2byteCube() {
    var bytes: Int
    var buf: ByteBuffer
    for (i in 0 until FaceCount) {
        bytes = originalCubeBMP[i]!!.byteCount
        buf = ByteBuffer.allocate(bytes)
        originalCubeBMP[i]!!.copyPixelsToBuffer(buf)
        originalCubeByteArray[i] = buf.array()
    }
    bytes = newCubeBMP!!.byteCount
    buf = ByteBuffer.allocate(bytes)
    newCubeBMP!!.copyPixelsToBuffer(buf)
    newCubeByteArray = buf.array()
}

fun saveOldPoints() {
    for (i in 0 until PointCount) {
        oldP[i]!!.reset(P[i]!!.x, P[i]!!.y, P[i]!!.z)
    }
}

fun convert() {
    for (i in 0 until PointCount) {
        getNewPoint(StartX, StartY, EndX, EndY, CubeCenter, oldP[i]!!, P[i]!!)
        resetPointF(p[i]!!, P[i]!!)
    }
}

fun convert2(): Boolean {
    if (firstDistance == 0f) return false
    for (i in 0 until PointCount) {
        getNewSizePoint(
            firstDistance, lastDistance,
            StartX, StartY, EndX, EndY, CubeCenter, oldP[i]!!, P[i]!!, true
        )
        resetPointF(p[i]!!, P[i]!!)
    }
    return true
}

fun drawSolid() {
    val index = intArrayOf(0, 0, 0)
    val bmpIndex = intArrayOf(0, 0, 0)
    var count = 0
    for (i in 0 until FaceCount) {
        if (mainActivity.isCubeVisible(
                P[face[i][1]]!!.x, P[face[i][1]]!!.y, P[face[i][1]]!!.z,
                P[face[i][2]]!!.x, P[face[i][2]]!!.y, P[face[i][2]]!!.z,
                P[face[i][3]]!!.x, P[face[i][3]]!!.y, P[face[i][3]]!!.z
            ) > 0
        ) {
            index[count] = i
            bmpIndex[count++] = i
        }
    }
    for (i in 0 until count) {
        for (j in 0..3) {
            facePointsX[i][j] = p[face[index[i]][j]]!!.x
            facePointsY[i][j] = p[face[index[i]][j]]!!.y
        }
    }
    try {
        mainActivity.transformsCube(count, index, facePointsX, facePointsY)
        newCubeBMP!!.copyPixelsFromBuffer(ByteBuffer.wrap(newCubeByteArray))
    } catch (e: Exception) {
        e.toString()
    }
}

fun turnCube() {
    StartX = 100f
    StartY = CubeCenter.y
    saveOldPoints()
    firstDistance = 0f
    isSingle = true
    EndX = 50f
    EndY = CubeCenter.y
    convert()
    drawSolid()
    indexCube = 1 - indexCube
}
