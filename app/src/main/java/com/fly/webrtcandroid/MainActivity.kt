package com.fly.webrtcandroid

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
    }

    fun cameraPreview(view: View) {
        startActivity(Intent(this, CapturePreviewActivity::class.java))
    }

    fun videoCall(view: View) {
        startActivity(Intent(this, CallVideoActivity::class.java))
    }


    /**
     * 动态获取权限
     */
    private fun requestPermission() {
        val perms = arrayOf<String>(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
        } else {
            EasyPermissions.requestPermissions(
                this, "请求录像及麦克风权限",
                100, *perms
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}