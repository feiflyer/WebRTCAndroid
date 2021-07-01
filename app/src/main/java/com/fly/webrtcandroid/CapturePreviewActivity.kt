package com.fly.webrtcandroid

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.webrtc.*

/**
 * 摄像头预览
 */
class CapturePreviewActivity : AppCompatActivity() {

    val VIDEO_TRACK_ID = "1" //"ARDAMSv0"

    private val VIDEO_RESOLUTION_WIDTH = 1280
    private val VIDEO_RESOLUTION_HEIGHT = 720
    private val VIDEO_FPS = 30


    //    绘制全局的上下文
    private var rootEglBase: EglBase? = null
    private var mVideoTrack: VideoTrack? = null

    private var mPeerConnectionFactory: PeerConnectionFactory? = null

    //纹理渲染
    private var mSurfaceTextureHelper: SurfaceTextureHelper? = null

    private var mVideoCapture: VideoCapturer? = null

    private val camera_preview by lazy {
        findViewById<SurfaceViewRenderer>(R.id.camera_preview)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_preview)

        rootEglBase = EglBase.create()

        camera_preview.init(rootEglBase?.eglBaseContext, null)

        //悬浮顶端
        camera_preview.setZOrderMediaOverlay(true)
        //硬件加速
        camera_preview.setEnableHardwareScaler(true)
        camera_preview.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)

        mPeerConnectionFactory = createPeerConnectionFactory(this)

        mVideoCapture = createVideoCapture()

        // 初始化
        mSurfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
        // 创建VideoSource
        val videoSource = mPeerConnectionFactory!!.createVideoSource(false)
        mVideoCapture?.initialize(
            mSurfaceTextureHelper,
            applicationContext,
            videoSource.capturerObserver
        )

        mVideoTrack = mPeerConnectionFactory!!.createVideoTrack(VIDEO_TRACK_ID,
            videoSource
        )
        mVideoTrack?.setEnabled(true)
        mVideoTrack?.addSink(camera_preview)

    }

    /**
     * 创建PeerConnectionFactory
     */
    private fun createPeerConnectionFactory(context: Context?): PeerConnectionFactory? {
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase?.eglBaseContext,
            false /* enableIntelVp8Encoder */,
            true
        )
        decoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
        builder.setOptions(null)
        return builder.createPeerConnectionFactory()
    }

    private fun createVideoCapture(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(this)) {
            createCameraCapture(Camera2Enumerator(this))
        } else {
            createCameraCapture(Camera1Enumerator(true))
        }
    }

    private fun createCameraCapture(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {

                val videoCapture: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapture != null) {
                    return videoCapture
                }
            }
        }

        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapture: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapture != null) {
                    return videoCapture
                }
            }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        // 开启摄像头预览
        mVideoCapture?.startCapture(
            VIDEO_RESOLUTION_WIDTH,
            VIDEO_RESOLUTION_HEIGHT,
            VIDEO_FPS
        )
    }

    override fun onStop() {
        super.onStop()
        mVideoCapture?.stopCapture()
    }

}