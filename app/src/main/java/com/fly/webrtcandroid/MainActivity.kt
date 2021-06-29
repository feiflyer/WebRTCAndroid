package com.fly.webrtcandroid

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
    }


    private fun requestPermission(){
        val perms = arrayOf<String>(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
        } else {
            EasyPermissions.requestPermissions(this, "请求录像及麦克风权限",
                    100, *perms)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}