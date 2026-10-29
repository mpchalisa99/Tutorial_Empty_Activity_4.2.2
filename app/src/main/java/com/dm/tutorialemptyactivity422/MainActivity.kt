package com.dm.tutorialemptyactivity422

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class MainActivity : AppCompatActivity() {

    private val sampleRate = 16000
    private var isRecording = false
    private var udpSocket: DatagramSocket? = null
    private val broadcastIp = "192.168.43.255" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE), 1)
        }

        startUdpReceiver()

        // Menjadikan seluruh area layar sebagai tombol PTT
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startStreaming()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopStreaming()
                    true
                }
                else -> false
            }
        }
    }

    private fun startStreaming() {
        isRecording = true
        Thread {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                
                val socket = DatagramSocket()
                socket.broadcast = true
                val address = InetSocketAddress(broadcastIp, 8888)
                val buffer = ByteArray(bufferSize)

                audioRecord.startRecording()
                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val packet = DatagramPacket(buffer, read, address)
                        socket.send(packet)
                    }
                }
                audioRecord.stop()
                audioRecord.release()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun stopStreaming() {
        isRecording = false
    }

    private fun startUdpReceiver() {
        Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                udpSocket = DatagramSocket(null)
                udpSocket?.reuseAddress = true
                udpSocket?.bind(InetSocketAddress("0.0.0.0", 8888))
                
                val buffer = ByteArray(bufferSize)
                audioTrack.play()

                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    if (!isRecording) { 
                        audioTrack.write(packet.data, 0, packet.length)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
