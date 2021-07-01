package com.fly.webrtcandroid

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.Executors

/**
 * 端对端通话
 *
 *
 * 测试方法：使用两台手机，一台先打开这个界面，另一台后打开该界面
 *
 */
class CallVideoActivity : AppCompatActivity(), ConnectRoomListener {


    val HOST_NAME = "https://106.12.128.80"
    val VIDEO_TRACK_ID = "1" //"ARDAMSv0";

    val AUDIO_TRACK_ID = "2" //"ARDAMSa0";

    private val VIDEO_RESOLUTION_WIDTH = 1280
    private val VIDEO_RESOLUTION_HEIGHT = 720
    private val VIDEO_FPS = 30

    private val local_renderer by lazy {
        findViewById<SurfaceViewRenderer>(R.id.local_renderer)
    }

    private val remote_renderer by lazy {
        findViewById<SurfaceViewRenderer>(R.id.video_remote_renderer)
    }

    private val executors by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val sdpMediaConstraints by lazy {
        MediaConstraints().apply {
//            mandatory.add(
//                    MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                    MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

//            optional.add(
//                    MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

    }

    //纹理渲染
    private var mSurfaceTextureHelper: SurfaceTextureHelper? = null

    //    绘制全局的上下文
    private var rootEglBase: EglBase? = null

    //用于数据传输
    private var mPeerConnection: PeerConnection? = null
    private var mPeerConnectionFactory: PeerConnectionFactory? = null

    private var mVideoTrack: VideoTrack? = null
    private var mAudioTrack: AudioTrack? = null

    private var mVideoCapture: VideoCapturer? = null


    val pcObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            // 将ice通过socket发送到远端
            Logger.d("-------onIceCandidate-----")
            mPeerConnection?.addIceCandidate(iceCandidate)
            val json = JSONObject()
            json.put("type", "candidate")
            json.put("label", iceCandidate?.sdpMLineIndex)
            json.put("id", iceCandidate?.sdpMid)
            json.put("candidate", iceCandidate?.sdp)
            WebSocketClient.sendMsg(json.toString())
        }

        override fun onDataChannel(p0: DataChannel?) {
            Logger.d("onDataChannel")
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Logger.d("onIceConnectionReceivingChange")
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Logger.d("onIceConnectionChange")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            Logger.d("onIceGatheringChange")
        }

        override fun onAddStream(p0: MediaStream?) {
            Logger.d("onAddStream")
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            Logger.d("onSignalingChange")
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
            Logger.d("onIceCandidatesRemoved")
        }

        override fun onRemoveStream(p0: MediaStream?) {
            Logger.d("onRemoveStream")
        }

        override fun onRenegotiationNeeded() {
            Logger.d("onRenegotiationNeeded")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver?, p1: Array<out MediaStream>?) {
            Logger.d("-------onAddTrack-----")
            val track = rtpReceiver?.track()
            if (track is VideoTrack) {
                val remoteVideoTrack = track
                remoteVideoTrack.setEnabled(true)
                remoteVideoTrack.addSink(remote_renderer)
                Logger.d("-------onAddTrack---远端渲染--")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_video)
        requestPermission()

        initRenderView()

        val url = HOST_NAME + "/join/120022"
        WebSocketClient.connectRoom(url, this)
//        WebSocketClient.connectRegister("wss://106.12.128.80:8089/ws","","120031","50477544")

//        connectSuccess(Gson().fromJson(" {\"result\": \"SUCCESS\", \"params\": {\"wss_post_url\": \"https://106.12.128.80:8089\", \"media_constraints\": \"{\\\"audio\\\": true, \\\"video\\\": {\\\"mandatory\\\": {}, \\\"optional\\\": [{\\\"minWidth\\\": \\\"1280\\\"}, {\\\"minHeight\\\": \\\"720\\\"}]}}\", \"is_loopback\": \"false\", \"ice_server_transports\": \"\", \"bypass_join_confirmation\": \"false\", \"offer_options\": \"{}\", \"is_initiator\": \"false\", \"room_link\": \"http://106.12.128.80/r/120036\", \"room_id\": \"120036\", \"client_id\": \"32328751\", \"header_message\": \"\", \"warning_messages\": [], \"pc_config\": \"{\\\"rtcpMuxPolicy\\\": \\\"require\\\", \\\"bundlePolicy\\\": \\\"max-bundle\\\", \\\"iceServers\\\": [{\\\"credential\\\": \\\"123456\\\", \\\"username\\\": \\\"flyer\\\", \\\"urls\\\": [\\\"turn:106.12.128.80:3478?transport=udp\\\", \\\"turn:106.12.128.80:3478?transport=tcp\\\"]}, {\\\"urls\\\": [\\\"stun:106.12.128.80:3478\\\"]}]}\", \"version_info\": \"{\\\"gitHash\\\": \\\"78600dbe205774c115cf481a091387d928c99d6a\\\", \\\"time\\\": \\\"Wed Sep 23 12:49:00 2020 +0200\\\", \\\"branch\\\": \\\"master\\\"}\", \"ice_server_url\": \"https://106.12.128.80/v1alpha/iceconfig?key=\", \"wss_url\": \"wss://106.12.128.80:8089/ws\", \"messages\": [\"{\\\"sdp\\\":\\\"v=0\\\\r\\\\no=- 6361709926694756832 2 IN IP4 127.0.0.1\\\\r\\\\ns=-\\\\r\\\\nt=0 0\\\\r\\\\na=group:BUNDLE audio video\\\\r\\\\na=msid-semantic: WMS ARDAMS\\\\r\\\\nm=audio 9 UDP\\\\/TLS\\\\/RTP\\\\/SAVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126\\\\r\\\\nc=IN IP4 0.0.0.0\\\\r\\\\na=rtcp:9 IN IP4 0.0.0.0\\\\r\\\\na=ice-ufrag:brYg\\\\r\\\\na=ice-pwd:P9x\\\\/VIehWOsJd3BYNqgGrvJ0\\\\r\\\\na=ice-options:trickle renomination\\\\r\\\\na=fingerprint:sha-256 C7:51:95:4C:13:2B:AA:23:07:27:F3:28:9D:F7:37:50:EA:24:A6:ED:14:2B:BB:5C:66:8C:9E:8B:5E:4C:FD:A9\\\\r\\\\na=setup:actpass\\\\r\\\\na=mid:audio\\\\r\\\\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\\\\r\\\\na=extmap:2 http:\\\\/\\\\/www.webrtc.org\\\\/experiments\\\\/rtp-hdrext\\\\/abs-send-time\\\\r\\\\na=extmap:3 http:\\\\/\\\\/www.ietf.org\\\\/id\\\\/draft-holmer-rmcat-transport-wide-cc-extensions-01\\\\r\\\\na=sendrecv\\\\r\\\\na=rtcp-mux\\\\r\\\\na=rtpmap:111 opus\\\\/48000\\\\/2\\\\r\\\\na=rtcp-fb:111 transport-cc\\\\r\\\\na=fmtp:111 minptime=10;useinbandfec=1\\\\r\\\\na=rtpmap:103 ISAC\\\\/16000\\\\r\\\\na=rtpmap:104 ISAC\\\\/32000\\\\r\\\\na=rtpmap:9 G722\\\\/8000\\\\r\\\\na=rtpmap:102 ILBC\\\\/8000\\\\r\\\\na=rtpmap:0 PCMU\\\\/8000\\\\r\\\\na=rtpmap:8 PCMA\\\\/8000\\\\r\\\\na=rtpmap:106 CN\\\\/32000\\\\r\\\\na=rtpmap:105 CN\\\\/16000\\\\r\\\\na=rtpmap:13 CN\\\\/8000\\\\r\\\\na=rtpmap:110 telephone-event\\\\/48000\\\\r\\\\na=rtpmap:112 telephone-event\\\\/32000\\\\r\\\\na=rtpmap:113 telephone-event\\\\/16000\\\\r\\\\na=rtpmap:126 telephone-event\\\\/8000\\\\r\\\\na=ssrc:1029140565 cname:qyy\\\\/o0JTeXBPLtEQ\\\\r\\\\na=ssrc:1029140565 msid:ARDAMS 2\\\\r\\\\na=ssrc:1029140565 mslabel:ARDAMS\\\\r\\\\na=ssrc:1029140565 label:2\\\\r\\\\nm=video 9 UDP\\\\/TLS\\\\/RTP\\\\/SAVPF 96 97 98 99 100 101 127 123 125\\\\r\\\\nc=IN IP4 0.0.0.0\\\\r\\\\na=rtcp:9 IN IP4 0.0.0.0\\\\r\\\\na=ice-ufrag:brYg\\\\r\\\\na=ice-pwd:P9x\\\\/VIehWOsJd3BYNqgGrvJ0\\\\r\\\\na=ice-options:trickle renomination\\\\r\\\\na=fingerprint:sha-256 C7:51:95:4C:13:2B:AA:23:07:27:F3:28:9D:F7:37:50:EA:24:A6:ED:14:2B:BB:5C:66:8C:9E:8B:5E:4C:FD:A9\\\\r\\\\na=setup:actpass\\\\r\\\\na=mid:video\\\\r\\\\na=extmap:14 urn:ietf:params:rtp-hdrext:toffset\\\\r\\\\na=extmap:2 http:\\\\/\\\\/www.webrtc.org\\\\/experiments\\\\/rtp-hdrext\\\\/abs-send-time\\\\r\\\\na=extmap:13 urn:3gpp:video-orientation\\\\r\\\\na=extmap:3 http:\\\\/\\\\/www.ietf.org\\\\/id\\\\/draft-holmer-rmcat-transport-wide-cc-extensions-01\\\\r\\\\na=extmap:5 http:\\\\/\\\\/www.webrtc.org\\\\/experiments\\\\/rtp-hdrext\\\\/playout-delay\\\\r\\\\na=extmap:6 http:\\\\/\\\\/www.webrtc.org\\\\/experiments\\\\/rtp-hdrext\\\\/video-content-type\\\\r\\\\na=extmap:7 http:\\\\/\\\\/www.webrtc.org\\\\/experiments\\\\/rtp-hdrext\\\\/video-timing\\\\r\\\\na=extmap:8 http:\\\\/\\\\/www.webrtc.org\\\\/experiments\\\\/rtp-hdrext\\\\/color-space\\\\r\\\\na=sendrecv\\\\r\\\\na=rtcp-mux\\\\r\\\\na=rtcp-rsize\\\\r\\\\na=rtpmap:96 VP8\\\\/90000\\\\r\\\\na=rtcp-fb:96 goog-remb\\\\r\\\\na=rtcp-fb:96 transport-cc\\\\r\\\\na=rtcp-fb:96 ccm fir\\\\r\\\\na=rtcp-fb:96 nack\\\\r\\\\na=rtcp-fb:96 nack pli\\\\r\\\\na=rtpmap:97 rtx\\\\/90000\\\\r\\\\na=fmtp:97 apt=96\\\\r\\\\na=rtpmap:98 VP9\\\\/90000\\\\r\\\\na=rtcp-fb:98 goog-remb\\\\r\\\\na=rtcp-fb:98 transport-cc\\\\r\\\\na=rtcp-fb:98 ccm fir\\\\r\\\\na=rtcp-fb:98 nack\\\\r\\\\na=rtcp-fb:98 nack pli\\\\r\\\\na=rtpmap:99 rtx\\\\/90000\\\\r\\\\na=fmtp:99 apt=98\\\\r\\\\na=rtpmap:100 H264\\\\/90000\\\\r\\\\na=rtcp-fb:100 goog-remb\\\\r\\\\na=rtcp-fb:100 transport-cc\\\\r\\\\na=rtcp-fb:100 ccm fir\\\\r\\\\na=rtcp-fb:100 nack\\\\r\\\\na=rtcp-fb:100 nack pli\\\\r\\\\na=fmtp:100 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\\\\r\\\\na=rtpmap:101 rtx\\\\/90000\\\\r\\\\na=fmtp:101 apt=100\\\\r\\\\na=rtpmap:127 red\\\\/90000\\\\r\\\\na=rtpmap:123 rtx\\\\/90000\\\\r\\\\na=fmtp:123 apt=127\\\\r\\\\na=rtpmap:125 ulpfec\\\\/90000\\\\r\\\\na=ssrc-group:FID 3014655706 1384463864\\\\r\\\\na=ssrc:3014655706 cname:qyy\\\\/o0JTeXBPLtEQ\\\\r\\\\na=ssrc:3014655706 msid:ARDAMS 1\\\\r\\\\na=ssrc:3014655706 mslabel:ARDAMS\\\\r\\\\na=ssrc:3014655706 label:1\\\\r\\\\na=ssrc:1384463864 cname:qyy\\\\/o0JTeXBPLtEQ\\\\r\\\\na=ssrc:1384463864 msid:ARDAMS 1\\\\r\\\\na=ssrc:1384463864 mslabel:ARDAMS\\\\r\\\\na=ssrc:1384463864 label:1\\\\r\\\\n\\\",\\\"type\\\":\\\"offer\\\"}\"], \"error_messages\": [], \"include_loopback_js\": \"\", \"pc_constraints\": \"{\\\"optional\\\": []}\"}}", RoomParam::class.java))
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

        local_renderer.init(rootEglBase?.eglBaseContext, null)
        remote_renderer.init(rootEglBase?.eglBaseContext, null)

        //悬浮顶端
        remote_renderer.setZOrderMediaOverlay(true)
        //硬件加速
        remote_renderer.setEnableHardwareScaler(true)
        local_renderer.setEnableHardwareScaler(true)
        remote_renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)

        //创建 factory， pc是从factory里获得的
        mPeerConnectionFactory = createPeerConnectionFactory(this)

        mVideoCapture = createVideoCapture()

        mSurfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
        val videoSource = mPeerConnectionFactory!!.createVideoSource(true)
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

        // 为了方便测试不需要声音
//        mAudioTrack?.setEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        // 开启摄像头预览
        mVideoCapture?.startCapture(
                VIDEO_RESOLUTION_WIDTH,
                VIDEO_RESOLUTION_HEIGHT,
                24
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


    private fun createPeerConnection(iceServers: List<IceServer>) {
        if (null == mPeerConnection) {
            val rtcConfig = RTCConfiguration(iceServers)
            // TCP candidates are only useful when connecting to a server that supports
            // ICE-TCP.
            // TCP candidates are only useful when connecting to a server that supports
            // ICE-TCP.
            rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            //rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            //rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            //rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            //rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            rtcConfig.continualGatheringPolicy =
                    PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            // Use ECDSA encryption.
            //rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
            // Enable DTLS for normal calls and disable for loopback calls.
            // Use ECDSA encryption.
            //rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
            // Enable DTLS for normal calls and disable for loopback calls.
            rtcConfig.enableDtlsSrtp = true
            //rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

            mPeerConnection = mPeerConnectionFactory?.createPeerConnection(rtcConfig, pcObserver)

            val mediaStreamLabels =
                    listOf("ARDAMS")

            mPeerConnection?.addTrack(mVideoTrack, mediaStreamLabels)
            mPeerConnection?.addTrack(mAudioTrack, mediaStreamLabels)
        }
    }


    /**
     * 创建offer
     */
    private fun createOffer(roomParam: RoomParam) {
        executors.execute {
            createPeerConnection(roomParam.params.parseIceServices())
            mPeerConnection?.createOffer(object : SdpObserver {
                override fun onSetFailure(p0: String?) {
                    Logger.d("createOffer----onSetFailure")
                }

                override fun onSetSuccess() {
                    Logger.d("createOffer----onSetSuccess")
                }

                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    Logger.d("createOffer----onCreateSuccess:${sessionDescription?.description}")

                    mPeerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetFailure(p0: String?) {
                            Logger.d("setLocalDescription----onSetFailure:${p0}")
                        }

                        override fun onSetSuccess() {
                            Logger.d("setLocalDescription----onSetSuccess")
                            // 设置成功，需要发送信令告诉服务器
                            WebSocketClient.sendOffer(HOST_NAME + "/message/" + roomParam.params.room_id + "/" + roomParam.params.client_id, sessionDescription!!)
                        }

                        override fun onCreateSuccess(p0: SessionDescription?) {

                        }

                        override fun onCreateFailure(p0: String?) {

                        }

                    }, sessionDescription)
                }

                override fun onCreateFailure(p0: String?) {
                    Logger.d("createOffer----onCreateFailure")
                }

            }, sdpMediaConstraints)
        }
    }

    /**
     * 创建answer
     */
    private fun createAnswer(roomParam: RoomParam) {

        executors.execute {
            createPeerConnection(roomParam.params.parseIceServices())


            val offerSdp = roomParam.params.parseSdp()
            Logger.d("createAnswer----remoteSdp:${offerSdp?.description}")

            mPeerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetFailure(p0: String?) {
                    Logger.d("createAnswer--setRemoteDescription--onSetFailure:${p0}")
                }

                override fun onSetSuccess() {
                    Logger.d("createAnswer---setRemoteDescription--onSetSuccess")

                    mPeerConnection?.createAnswer(object : SdpObserver {
                        override fun onSetFailure(p0: String?) {
                            Logger.d("createAnswer----onSetFailure")
                        }

                        override fun onSetSuccess() {
                            Logger.d("createAnswer----onSetSuccess")
                        }

                        override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                            Logger.d("createAnswer----onCreateSuccess")
                            mPeerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onSetFailure(p0: String?) {
                                    Logger.d("setLocalDescription----onSetFailure:${p0}")
                                }

                                override fun onSetSuccess() {
                                    Logger.d("setLocalDescription----onSetSuccess")
                                    // 设置成功，需要发送信令告诉服务器
                                    WebSocketClient.sendAnswer(sessionDescription!!)
                                }

                                override fun onCreateSuccess(p0: SessionDescription?) {

                                }

                                override fun onCreateFailure(p0: String?) {

                                }

                            }, sessionDescription)
                        }

                        override fun onCreateFailure(p0: String?) {
                            Logger.d("createAnswer----onCreateFailure:${p0}")
                        }

                    }, sdpMediaConstraints)


                }

                override fun onCreateSuccess(p0: SessionDescription?) {
                    Logger.d("createAnswer--setRemoteDescription--onCreateSuccess")
                    // 在这里创建answer

                }

                override fun onCreateFailure(p0: String?) {
                    Logger.d("createAnswer--setRemoteDescription--onCreateFailure")
                }

            }, offerSdp)
        }
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
                createOffer(roomParam)
            } else {
                // 被叫则创建answer
                Logger.d("被叫")
                createAnswer(roomParam)
            }
        }
    }

    override fun onReceiveRemoteIceCandidate(candidate: IceCandidate?) {
        Logger.d("onReceiveRemoteIceCandidate")
        mPeerConnection?.addIceCandidate(candidate)
    }

    override fun onReceiveRemoteDescription(sessionDescription: SessionDescription) {

        Logger.d("onReceiveRemoteDescription")

        mPeerConnection?.setRemoteDescription(object:SdpObserver{
            override fun onSetFailure(p0: String?) {
                Logger.d("onReceiveRemoteDescription------onSetFailure:${p0}")
            }

            override fun onSetSuccess() {
                Logger.d("onReceiveRemoteDescription------onSetSuccess")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                Logger.d("onReceiveRemoteDescription------onCreateSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Logger.d("onReceiveRemoteDescription------onCreateFailure:${p0}")
            }

        },sessionDescription)
    }
}