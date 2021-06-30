package com.fly.webrtcandroid

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.webrtc.*
import pub.devrel.easypermissions.EasyPermissions

class CallVideoActivity : AppCompatActivity(),ConnectRoomListener {

    val VIDEO_TRACK_ID = "1" //"ARDAMSv0";

    val AUDIO_TRACK_ID = "2" //"ARDAMSa0";

    private val VIDEO_RESOLUTION_WIDTH = 1280
    private val VIDEO_RESOLUTION_HEIGHT = 720
    private val VIDEO_FPS = 30

    private val local_renderer by lazy {
        findViewById<SurfaceViewRenderer>(R.id.local_renderer)
    }

    private val remote_renderer by lazy {
        findViewById<SurfaceViewRenderer>(R.id.remote_renderer)
    }

    //纹理渲染
    private var mSurfaceTextureHelper: SurfaceTextureHelper? = null

    //    绘制全局的上下文
    private var rootEglBase: EglBase? = null

    //用于数据传输
    private val mPeerConnection: PeerConnection? = null
    private var mPeerConnectionFactory: PeerConnectionFactory? = null

    private var mVideoTrack: VideoTrack? = null
    private var mAudioTrack: AudioTrack? = null

    private var mVideoCapture: VideoCapturer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_video)
        requestPermission()

        val url = "https://106.12.128.80/join/120031"
//        WebSocketClient.connectRoom(url, this)
//        WebSocketClient.connectRegister("wss://106.12.128.80:8089/ws","","120031","50477544")

        initRenderView()
    }

    /**
     * 动态获取权限
     */
    private fun requestPermission() {
        val perms = arrayOf<String>(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
        } else {
            EasyPermissions.requestPermissions(
                this, "请求录像及麦克风权限",
                100, *perms
            )
        }
    }

    /**
     * 初始化View
     */
    private fun initRenderView() {
        rootEglBase = EglBase.create()

        remote_renderer.init(rootEglBase?.eglBaseContext, null)
        local_renderer.init(rootEglBase?.eglBaseContext, null)

        //悬浮顶端
        remote_renderer.setZOrderMediaOverlay(true)
        //硬件加速
        remote_renderer.setEnableHardwareScaler(true)
        local_renderer.setEnableHardwareScaler(true)
        remote_renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        remote_renderer.setMirror(true)

        //创建 factory， pc是从factory里获得的
        mPeerConnectionFactory = createPeerConnectionFactory(this)

        mVideoCapture = createVideoCapture()

        mSurfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
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
        mVideoTrack?.addSink(local_renderer)

        val audioSource =
            mPeerConnectionFactory!!.createAudioSource(MediaConstraints())
        mAudioTrack = mPeerConnectionFactory!!.createAudioTrack(
            AUDIO_TRACK_ID,
            audioSource
        )
        mAudioTrack?.setEnabled(true)
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

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     **/
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun connectSuccess(roomParam: RoomParam) {
        if (roomParam.result == ROOM_SUCCESS) {
            WebSocketClient.connectRegister(
                roomParam.params.wss_url,
                roomParam.params.room_id,
                roomParam.params.client_id
            )
            // 连接webSocket并注册房间id和clientid，等待有人上线时通知
            if (roomParam.params.is_initiator) {
                // 是否是主叫，是主叫则创建offer
                Logger.d("主叫")
            } else {
                // 被叫则创建answer
                Logger.d("被叫")
            }
        }
    }
}