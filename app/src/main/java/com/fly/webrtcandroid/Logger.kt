package com.fly.webrtcandroid

import android.util.Log

object Logger{

    const val LOG_TAG = "fly_web_rtc"
    fun d(msg:String){
        Log.v(LOG_TAG,msg)
    }

}