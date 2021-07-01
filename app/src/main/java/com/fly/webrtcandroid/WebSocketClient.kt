package com.fly.webrtcandroid

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.io.IOException
import java.util.concurrent.TimeUnit


object WebSocketClient {

    private val okHttpClient by lazy {
        OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(
                        SSLSocketClient.getSSLSocketFactory(),
                        SSLSocketClient.x509TrustManager
                )
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                .build()
    }


    private var socket: WebSocket? = null


    var connectRoomListener: ConnectRoomListener? = null


    private var hasConnected = false
    private val msgQue by lazy {
        ArrayList<String>()
    }

    /**
     * http请求连接房间
     */
    fun connectRoom(url: String, listener: ConnectRoomListener) {

        connectRoomListener = listener

        val JSON = "application/json; charset=utf-8".toMediaType()
        val requestBody = "".toRequestBody(JSON)
        val request = Request.Builder().post(requestBody).url(url).build()
        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.d("onFailure")
            }

            override fun onResponse(call: Call, response: Response) {
                val roomParam = Gson().fromJson(response.body?.string(), RoomParam::class.java)
                Logger.d("onResponse:${Gson().toJson(roomParam)}")
                listener.connectSuccess(roomParam)
            }

        })

    }

    /**
     * 连接WebSocket
     */
    fun connectRegister(wssUrl: String, roomId: String, clientId: String) {

        // 增加Origin的请求头避免403
        val request = Request.Builder().get().url(wssUrl).header("Origin", "http://*******").build()
        socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Logger.d("onOpen")
                hasConnected = true
                // 注册
                registerRoom(roomId, clientId)

                // 发送消息队列
                msgQue.forEach { msg ->
                    webSocket.send(msg)
                }

                msgQue.clear()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Logger.d("onClosed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                Logger.d("onFailure")
            }

            override fun onMessage(webSocket: WebSocket, message: String) {
                super.onMessage(webSocket, message)
                Logger.d("onMessage:${message}")
                val json = JSONObject(message)
                val msgText: String = json.getString("msg")
                if (msgText.isNotEmpty()) {
                    val json1 = JSONObject(msgText)
                    val type = json1.getString("type")
                    if (type == "candidate") {
                        val iceCandidate = IceCandidate(
                                json1.getString("id"), json1.getInt("label"),
                                json1.getString("candidate")
                        )
                        connectRoomListener?.onReceiveRemoteIceCandidate(iceCandidate)

                    } else if (type == "remove-candidates") {
                        val candidateArray = json1.getJSONArray("candidates")
                        val candidates =
                                arrayOfNulls<IceCandidate>(candidateArray.length())
                        for (i in 0 until candidateArray.length()) {
                            val iceJson = candidateArray.getJSONObject(i)
                            val iceCandidate = IceCandidate(
                                    iceJson.getString("id"),
                                    iceJson.getInt("lable"),
                                    iceJson.getString("candidate")
                            )
                            candidates[i] = iceCandidate
                        }

                    } else if (type == "answer") {


                        // 被邀请
//                         if (initiator) {
////                        主动发送视频       并且对方同意了    sdp 对方
                        val sdp = SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type),
                                json1.getString("sdp")
                        )
                        connectRoomListener?.onReceiveRemoteDescription(sdp)
                    } else if (type == "offer") {
                        // 邀请
                    } else if (type == "bye") {
                        //
                    } else {

                    }
                } else {


                }
            }
        })

    }


    /**
     * 注册房间
     */
    private fun registerRoom(roomId: String, clientId: String) {
        val json = JSONObject()
        json.put("cmd", "register")
        json.put("roomid", roomId)
        json.put("clientid", clientId)
        if (true == socket?.send(json.toString())) {
            Logger.d("registerRoom-----true")
        } else {
            Logger.d("registerRoom-----false")
        }
    }

    /**
     * 使用WebSocket发送消息
     */
    fun sendMsg(msg: String) {
        val jsonObject = JSONObject()
        jsonObject.put("cmd", "send")
        jsonObject.put("msg", msg)

        if (hasConnected) {
            if (true == socket?.send(jsonObject.toString())) {
                Logger.d("webSocket----sendMsg-----true")
            } else {
                Logger.d("webSocket----sendMsg-----false")
            }
        } else {
            msgQue.add(jsonObject.toString())
            Logger.d("webSocket----sendMsg-----加入带发送队列")
        }
    }

    /**
     * offerUrl = host/message/roomId/clientId
     */
    fun sendOffer(offerUrl: String, offerSdp: SessionDescription) {

        Logger.d("sendOffer---offerUrl:${offerUrl}")
        val jsonObject = JSONObject()
        jsonObject.put("sdp", offerSdp.description)
        jsonObject.put("type", "offer")
        val JSON = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(JSON)
        val request = Request.Builder().post(requestBody).url(offerUrl).build()
        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.d("sendOffer---onFailure")
            }

            override fun onResponse(call: Call, response: Response) {
                Logger.d("sendOffer---onResponse:${response.body?.string()}")
            }
        })
    }

    fun sendAnswer(answerSdp: SessionDescription) {
        val jsonObject = JSONObject()
        jsonObject.put("sdp", answerSdp.description)
        jsonObject.put("type", "answer")
        sendMsg(jsonObject.toString())
    }

}

interface ConnectRoomListener {
    fun connectSuccess(param: RoomParam)

    //收到了对方的ICE   socket
    fun onReceiveRemoteIceCandidate(candidate: IceCandidate?)

    // 收到了对方的sdp
    fun onReceiveRemoteDescription(sessionDescription: SessionDescription)
}