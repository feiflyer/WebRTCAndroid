package com.fly.webrtcandroid

import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.SessionDescription
import java.util.*
import kotlin.collections.ArrayList

// 加入房间成功
const val ROOM_SUCCESS = "SUCCESS"

// 加入的房间已满
const val ROOM_FULL = "FULL"

/**
 *
 *
 * 主叫
 *
 *
 * {
"result": "SUCCESS",
"params": {
"wss_post_url": "https://106.12.128.80:8089",
"media_constraints": "{\"audio\": true, \"video\": true}",
"is_loopback": "false",
"ice_server_transports": "",
"bypass_join_confirmation": "false",
"offer_options": "{}",
"is_initiator": "true",
"room_link": "http://106.12.128.80/r/120031",
"room_id": "120031",
"client_id": "50477544",
"header_message": "",
"warning_messages": [

],
"pc_config": "{\"rtcpMuxPolicy\": \"require\", \"bundlePolicy\": \"max-bundle\", \"iceServers\": [{\"credential\": \"123456\", \"username\": \"flyer\", \"urls\": [\"turn:106.12.128.80:3478?transport=udp\", \"turn:106.12.128.80:3478?transport=tcp\"]}, {\"urls\": [\"stun:106.12.128.80:3478\"]}]}",
"version_info": "{\"gitHash\": \"78600dbe205774c115cf481a091387d928c99d6a\", \"time\": \"Wed Sep 23 12:49:00 2020 +0200\", \"branch\": \"master\"}",
"ice_server_url": "https://106.12.128.80/v1alpha/iceconfig?key=",
"wss_url": "wss://106.12.128.80:8089/ws",
"messages": [

],
"error_messages": [

],
"include_loopback_js": "",
"pc_constraints": "{\"optional\": []}"
}
}

 */


/**
 *
 *被叫
 *{
"params":{
"bypass_join_confirmation":"false",
"client_id":"51379142",
"error_messages":[

],
"header_message":"",
"ice_server_transports":"",
"ice_server_url":"https://106.12.128.80/v1alpha/iceconfig?key\u003d",
"include_loopback_js":"",
"is_initiator":false,
"is_loopback":"false",
"media_constraints":"{\"audio\": true, \"video\": true}",
"messages":[

],
"offer_options":"{}",
"pc_config":"{\"rtcpMuxPolicy\": \"require\", \"bundlePolicy\": \"max-bundle\", \"iceServers\": [{\"credential\": \"123456\", \"username\": \"flyer\", \"urls\": [\"turn:106.12.128.80:3478?transport\u003dudp\", \"turn:106.12.128.80:3478?transport\u003dtcp\"]}, {\"urls\": [\"stun:106.12.128.80:3478\"]}]}",
"pc_constraints":"{\"optional\": []}",
"room_id":"120032",
"room_link":"http://106.12.128.80/r/120032",
"version_info":"{\"gitHash\": \"78600dbe205774c115cf481a091387d928c99d6a\", \"time\": \"Wed Sep 23 12:49:00 2020 +0200\", \"branch\": \"master\"}",
"warning_messages":[

],
"wss_post_url":"https://106.12.128.80:8089",
"wss_url":"wss://106.12.128.80:8089/ws"
},
"result":"SUCCESS"
}
 *
 */
@Keep
data class RoomParam(
        val params: Params,
        val result: String
)

@Keep
data class Params(
        val bypass_join_confirmation: String,
        val client_id: String,
        val error_messages: List<Any>,
        val header_message: String,
        val ice_server_transports: String,
        val ice_server_url: String,
        val include_loopback_js: String,
        val is_initiator: Boolean,
        val is_loopback: String,
        val media_constraints: String,
        val messages: List<Any>,
        val offer_options: String,
        val pc_config: String,
        val pc_constraints: String,
        val room_id: String,
        val room_link: String,
        val version_info: String,
        val warning_messages: List<Any>,
        val wss_post_url: String,
        val wss_url: String
) {

    fun parseIceServices(): List<PeerConnection.IceServer> {
        val list = LinkedList<IceServer>()
        val pc_configJson = JSONObject(pc_config)
        val iceServers = pc_configJson.getString("iceServers")
        val array = JSONArray(iceServers)
        for (i in 0 until array.length()) {
            val server: JSONObject = array.getJSONObject(i)
            val urlArray = JSONArray(server.getString("urls"))
            val urlList = ArrayList<String>()
            for (index in 0 until urlArray.length()) {
                urlList.add(urlArray.getString(index))
            }
            val credential = if (server.has("credential")) server.getString("credential") else "123456"
            val userName = if (server.has("username")) server.getString("username") else "flyer"
            val ice = IceServer.builder(urlList).setPassword(credential).setUsername(userName).createIceServer()
            list.add(ice)

            Logger.d("ice:${ice.toString()}")
        }
        return list
    }

    /**
     * 获取offer的sdp
     */
    fun parseSdp(): SessionDescription? {
        val sdpText = messages.getOrNull(0) as? String
        sdpText?.let {
            val sdpJson = JSONObject(sdpText)
            return SessionDescription(SessionDescription.Type.OFFER,sdpJson.getString("sdp"))
        }
        return null
    }
}