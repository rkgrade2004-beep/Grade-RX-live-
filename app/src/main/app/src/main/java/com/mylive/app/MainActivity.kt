package com.mylive.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView

class MainActivity : Activity(), ConnectChecker, SurfaceHolder.Callback {

    private lateinit var glView: OpenGlView
    private lateinit var camera: RtmpCamera2
    private lateinit var urlInput: EditText
    private lateinit var btn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1
        )

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        glView = OpenGlView(this)
        root.addView(glView, LinearLayout.LayoutParams(-1, 0, 1f))
        urlInput = EditText(this).apply {
            hint = "rtmp://a.rtmp.youtube.com/live2/STREAM-KEY"
            setSingleLine()
        }
        root.addView(urlInput, LinearLayout.LayoutParams(-1, -2))
        btn = Button(this).apply { text = "Go Live" }
        root.addView(btn, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)

        camera = RtmpCamera2(glView, this)
        glView.holder.addCallback(this)

        btn.setOnClickListener {
            if (!camera.isStreaming) {
                val ok = camera.prepareAudio() &&
                    camera.prepareVideo(1280, 720, 30, 2500 * 1000, 0)
                if (ok) {
                    camera.startStream(urlInput.text.toString().trim())
                    btn.text = "Stop"
                }
            } else {
                camera.stopStream()
                btn.text = "Go Live"
            }
        }
    }

    private fun hasPerms() =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        if (hasPerms() && !camera.isOnPreview) camera.startPreview()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {
        if (hasPerms() && !camera.isOnPreview) camera.startPreview()
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (camera.isStreaming) camera.stopStream()
        if (camera.isOnPreview) camera.stopPreview()
    }

    private fun toast(msg: String) = runOnUiThread {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionStarted(url: String) {}
    override fun onConnectionSuccess() = toast("Live ho gaye!")
    override fun onConnectionFailed(reason: String) {
        toast("Fail: $reason")
        runOnUiThread { camera.stopStream(); btn.text = "Go Live" }
    }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() = toast("Disconnected")
    override fun onAuthError() = toast("Auth error")
    override fun onAuthSuccess() {}
}
