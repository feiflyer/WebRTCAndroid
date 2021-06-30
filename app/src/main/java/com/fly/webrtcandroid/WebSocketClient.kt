package com.fly.webrtcandroid

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.webrtc.IceCandidate
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

    /**
     * http请求连接房间
     */
    fun connectRoom(url: String, listener: ConnectRoomListener) {
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
    fun connectRegister(wssUrl: String,roomId:String,clientId:String) {

        // 增加Origin的请求头避免403
        val request = Request.Builder().get().url(wssUrl).header("Origin", "http://*******").build()
        socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                // 注册
                registerRoom(roomId,clientId)
                Logger.d("onOpen")
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
//                             val sdp = SessionDescription(
//                                 SessionDescription.Type.fromCanonicalForm(type),
//                                 json1.getString("sdp")
//                             )
//                             mainActivityInterface.onRemoteDescription(sdp)
//                         } else {
//                         }
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
    private fun registerRoom(roomId:String,clientId:String){
        val json = JSONObject()
        json.put("cmd", "register")
        json.put("roomid", roomId)
        json.put("clientid", clientId)
        sendMsg(json.toString())
    }

    /**
     * 使用WebSocket发送消息
     */
    fun sendMsg(msg: String) {
        socket?.send(msg)
    }

}

interface ConnectRoomListener {
    fun connectSuccess(param: RoomParam)
}