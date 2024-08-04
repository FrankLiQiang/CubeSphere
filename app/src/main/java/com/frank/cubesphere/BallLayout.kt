package com.frank.cubesphere

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.frank.cubesphere.Common.PointXYZ
import com.frank.cubesphere.Common.getDistance
import com.frank.cubesphere.Common.getNewPoint
import com.frank.cubesphere.Common.getNewSizePoint
import com.frank.cubesphere.Common.resetPointF
import java.nio.ByteBuffer
import kotlin.properties.Delegates

lateinit var originalBallBMP: Bitmap
lateinit var newBallBMP: Bitmap
//var newCubeBMP: Bitmap? = null
lateinit var fatherBMP: Bitmap
lateinit var motherBMP: Bitmap
lateinit var ballByteArray: ByteArray
lateinit var newBallByteArray: ByteArray
lateinit var fatherByteArray: ByteArray
lateinit var motherByteArray: ByteArray
val Arctic = PointXYZ()
val Meridian = PointXYZ()
val ArcticF = Common.PointF()
val MeridianF = Common.PointF()
val Meridian0F = Common.PointF()
val OldArctic = PointXYZ()
val OldMeridian = PointXYZ()
var BallCenter = PointXYZ()
var ballR by Delegates.notNull<Float>()
var r = 0f
var indexBall by mutableStateOf(0L)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun drawBall(modifier: Modifier = Modifier) {
    if (indexBall < -1) return
    Image(
        bitmap = newBallBMP!!.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        stopDrawTimer()
                        StartX = it.x
                        StartY = it.y
                        saveOldPointsBall()
                        if (it.pointerCount == 2) {
                            firstDistance = getDistance(
                                it.getX(0), it.getY(0),
                                it.getX(1), it.getY(1)
                            ).toFloat()
                        }
                        isSingle = it.pointerCount == 1
                    }
                    MotionEvent.ACTION_MOVE -> {
                        EndX = it.x
                        EndY = it.y
                        if (it.pointerCount == 2) {
                            isSingle = false
                            lastDistance = getDistance(
                                it.getX(0), it.getY(0),
                                it.getX(1), it.getY(1)
                            ).toFloat()
                            convertBall2()
                        } else if (it.pointerCount == 1 && isSingle) {
                            convertBall()
                        }
                        drawPicBall()
                        indexBall = 1 - indexBall
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

fun ballInit() {
    fatherBMP = (mainActivity.resources.getDrawable(R.drawable.father) as BitmapDrawable).bitmap
    motherBMP = (mainActivity.resources.getDrawable(R.drawable.mother) as BitmapDrawable).bitmap
    originalBallBMP = (mainActivity.resources.getDrawable(R.drawable.all) as BitmapDrawable).bitmap
    newBallBMP = Bitmap.createBitmap(
        Common._screenWidth,
        Common._screenHeight / 2,
        Bitmap.Config.ARGB_8888
    )
    createBall()
}

fun createBall() {
    resetBall()
    bmp2byteBall()
}

fun resetBall() {
    ballR = Common._screenWidth / 2f
    BallCenter.reset(Common._screenWidth / 2f, Common._screenHeight / 4f, -ballR)
    Arctic.reset(BallCenter.x, BallCenter.y - ballR, BallCenter.z)
    Meridian.reset(BallCenter.x - ballR, BallCenter.y, BallCenter.z)
    resetPointF(ArcticF, Arctic)
    resetPointF(MeridianF, Meridian)
    val Meridian0 = PointXYZ()
    Meridian0.reset(BallCenter.x - ballR, BallCenter.y, BallCenter.z)
    resetPointF(Meridian0F, Meridian0)
    r = BallCenter.x - Meridian0F.x
}

fun bmp2byteBall() {
    var bytes: Int
    var buf: ByteBuffer
    bytes = originalBallBMP.byteCount
    buf = ByteBuffer.allocate(bytes)
    originalBallBMP.copyPixelsToBuffer(buf)
    ballByteArray = buf.array()
    bytes = fatherBMP.byteCount
    buf = ByteBuffer.allocate(bytes)
    fatherBMP.copyPixelsToBuffer(buf)
    fatherByteArray = buf.array()
    bytes = motherBMP.byteCount
    buf = ByteBuffer.allocate(bytes)
    motherBMP.copyPixelsToBuffer(buf)
    motherByteArray = buf.array()

    newBallBMP =
        Bitmap.createBitmap(Common._screenWidth, Common._screenHeight / 2, Bitmap.Config.ARGB_8888)
    bytes = newBallBMP.byteCount
    buf = ByteBuffer.allocate(bytes)
    newBallBMP.copyPixelsToBuffer(buf)
    newBallByteArray = buf.array()
}

fun drawPicBall() {
//    if (Looper.myLooper() == Looper.getMainLooper()) {
//        Log.i("5678", "Main Thread")
//    } else {
//        Log.i("5678", "Main Thread !!!!!!!!!!!!!!")
//    }

    try {
        mainActivity.transformsBall(Arctic.x, Arctic.y, Arctic.z, Meridian.x, Meridian.y, Meridian.z, r)
        newBallBMP.copyPixelsFromBuffer(ByteBuffer.wrap(newBallByteArray))
    } catch (e: Exception) {
        val a = e.toString()
    }
}

fun saveOldPointsBall() {
    OldArctic.reset(Arctic.x, Arctic.y, Arctic.z)
    OldMeridian.reset(Meridian.x, Meridian.y, Meridian.z)
}

fun convertBall() {
    getNewPoint(StartX, StartY, EndX, EndY, BallCenter, OldArctic, Arctic)
    getNewPoint(StartX, StartY, EndX, EndY, BallCenter, OldMeridian, Meridian)
    resetPointF(ArcticF, Arctic)
    resetPointF(MeridianF, Meridian)
}

fun convertBall2() {
    getNewSizePoint(
        firstDistance,
        lastDistance,
        StartX,
        StartY,
        EndX,
        EndY,
        BallCenter,
        OldArctic,
        Arctic,
        false
    )
    r = lastDistance / 2
    getNewSizePoint(
        firstDistance,
        lastDistance,
        StartX,
        StartY,
        EndX,
        EndY,
        BallCenter,
        OldMeridian,
        Meridian,
        false
    )
}

fun turnBall() {
    StartX = 30f
    StartY = BallCenter.y
    saveOldPointsBall()
    isSingle = true
    EndX = 100f
    EndY = BallCenter.y
    convertBall()
    drawPicBall()
    indexBall = 1 - indexBall
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposableSurfaceView(modifier: Modifier= Modifier){
    AndroidView(factory = {context ->
        SurfaceView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (indexBall >= -1) {
                holder.addCallback(MySurfaceCallback())//添加回调
            }
        }

    }, modifier = modifier)
}

@Preview
@Composable
fun MainSurfaceView(){
    ComposableSurfaceView()
}

class MySurfaceCallback: SurfaceHolder.Callback {
    private var _canvas: Canvas?= null
    override fun surfaceCreated(p0: SurfaceHolder) {
        _canvas =p0.lockCanvas()
        _canvas?.drawColor(Color.WHITE)//设置背景颜色
        drawPicBall()
//        _canvas?.drawCircle(600f, 600f, 650f, Paint().apply { color=Color.RED})
        _canvas?.drawBitmap(newCubeBMP!!, 0f, 0f, Paint().apply { color=Color.RED})
        _canvas?.drawBitmap(newBallBMP, 0f, Common._screenHeight / 2.0f, Paint().apply { color=Color.RED})
//        _canvas?.drawBitmap(backgroundBMP!!, 0f, 0f, Paint().apply { color=Color.RED})
        p0.unlockCanvasAndPost(_canvas)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        // 在这里处理Surface尺寸改变
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        // 在这里处理Surface销毁
    }
}
