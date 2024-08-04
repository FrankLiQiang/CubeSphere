package com.frank.cubesphere

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.frank.cubesphere.ui.theme.CubeSphereTheme
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.system.exitProcess

var backgroundBMP: Bitmap? = null
lateinit var backgroundByteArray: ByteArray
val originalCubeBMP = arrayOfNulls<Bitmap>(6)
lateinit var mainActivity: MainActivity

class MainActivity : ComponentActivity() {
    private external fun initialization(
        screenWidth: Int,
        screenHeight: Int,
        bgW: Int,
        bgH: Int,
        cubeW: Int,
        ballRawWidth: Int,
        ballRawHeight: Int,
        widthP: Int,
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        z: Float,
        cx: Float,
        cy: Float,
        cz: Float,
        R: Float,
        r: Float,
    )

    private external fun ReadPics(
        pBGData: ByteArray?,
        pSrcData1: ByteArray?,
        pSrcData2: ByteArray?,
        pSrcData3: ByteArray?,
        pSrcData4: ByteArray?,
        pSrcData5: ByteArray?,
        pSrcData6: ByteArray?,
        pBallData: ByteArray?,
        pBallDataF: ByteArray?,
        pBallDataM: ByteArray?,
        pOutBall: ByteArray?,
        pOutCube: ByteArray?,
    )

    external fun isCubeVisible(
        xE: Float, yE: Float, zE: Float,
        xF: Float, yF: Float, zF: Float,
        xG: Float, yG: Float, zG: Float
    ): Int

    external fun transformsCube(
        count: Int, index: IntArray?, fPointsX: Array<FloatArray>?, fPointsY: Array<FloatArray>?
    )

    external fun transformsBall(
        ArcticX: Float,
        ArcticY: Float,
        ArcticZ: Float,
        MeridianX: Float,
        MeridianY: Float,
        MeridianZ: Float,
        r: Float
    ): Int

    companion object {
        // Used to load the 'cubesphere' library on application startup.
        init {
            System.loadLibrary("CubeSphere")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        mainActivity = this
        // 设置页面全屏 刘海屏 显示
        val window = window
        val lp = window.attributes
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = lp
        val decorView = window.decorView
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        // 设置页面全屏 刘海屏 显示

        initBeepSound()

        Common.getScreenSize(windowManager)
        Common.DepthZ = Common._screenWidth / 2f
        Common.Eye.reset(
            Common._screenWidth / 2f, Common._screenHeight / 4f,
            (Common._screenHeight * 2).toFloat()
        )
        backgroundBMP = (resources.getDrawable(R.drawable.bg) as BitmapDrawable).bitmap
        backgroundBmp2byte()

        createCube()
        ballInit()

        initialization(
            Common._screenWidth,
            Common._screenHeight / 2,
            backgroundBMP!!.width,
            backgroundBMP!!.height,
            originalBallBMP.width,
            originalBallBMP.height,
            cubeWidth,
            fatherBMP.width,
            Common.Eye.x,
            Common.Eye.y,
            Common.Eye.z,
            Common.DepthZ,
            BallCenter.x,
            BallCenter.y,
            BallCenter.z,
            ballR,
            r,
        )
        ReadPics(
            backgroundByteArray,
            originalCubeByteArray[0],
            originalCubeByteArray[1],
            originalCubeByteArray[2],
            originalCubeByteArray[3],
            originalCubeByteArray[4],
            originalCubeByteArray[5],
            ballByteArray,
            fatherByteArray,
            motherByteArray,
            newBallByteArray,
            newCubeByteArray,
        )

//        drawSolid()
//        drawPicBall()
        startDrawTimer()
        setContent {
            CubeSphereTheme {
                // A surface container using the 'background' color from the theme
                Column(Modifier.fillMaxSize()) {
//                    if (indexBall >= -1) {
//                        ComposableSurfaceView()
//                    }
                    drawCube(Modifier.weight(1.0f))
                    drawBall(Modifier.weight(1.0f))
                }
            }
        }
    }

    private val BEEP_VOLUME = 9.10f
    private var mediaPlayer: MediaPlayer? = null

    private val beepListener =
        OnCompletionListener { mediaPlayer ->
            // 声音
            mediaPlayer.seekTo(0)
        }

    private fun initBeepSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setOnCompletionListener(beepListener)
            val file = resources.openRawResourceFd(R.raw.music)
            try {
                mediaPlayer!!.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                file.close()
                mediaPlayer!!.setVolume(BEEP_VOLUME, BEEP_VOLUME)
                mediaPlayer!!.isLooping = true
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
            } catch (e: IOException) {
                mediaPlayer = null
            }
        }
    }

    override fun onBackPressed() {
        stopDrawTimer()
        mediaPlayer?.release()
        finish()
        exitProcess(0)
    }
}


private fun backgroundBmp2byte() {
    val buf: ByteBuffer
    val bytes: Int = backgroundBMP!!.byteCount
    buf = ByteBuffer.allocate(bytes)
    backgroundBMP!!.copyPixelsToBuffer(buf)
    backgroundByteArray = buf.array()
}
