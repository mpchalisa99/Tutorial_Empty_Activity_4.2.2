package com.dm.tutorialemptyactivity422
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.*
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val sampleRate = 16000
    private val port = 50000
    private var isRecording = false
    private var socket: DatagramSocket? = null
    
    private lateinit var statusText: TextView
    private lateinit var logBox: TextView
    private lateinit var callsignInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- DESAIN UI TACTICAL HUD MILITER ---
        val bgColor = Color.parseColor("#05111A") // Biru Sangat Gelap
        val cyanNeon = Color.parseColor("#00E5FF")
        val cyanDark = Color.parseColor("#008B8B")
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(30, 60, 30, 30)
        }

        // Border Style untuk elemen
        val borderStyle = GradientDrawable().apply {
            setStroke(3, cyanDark)
            setColor(Color.parseColor("#0A1C29"))
            cornerRadius = 10f
        }

        // Judul Atas
        val titleText = TextView(this).apply {
            text = "TACTICAL WI-COMM SUBDISRUDAL"
            setTextColor(cyanNeon)
            textSize = 22f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // Input Callsign
        callsignInput = EditText(this).apply {
            hint = "CALLSIGN (MISAL: SERI-02)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setTypeface(Typeface.MONOSPACE)
            background = borderStyle
            setPadding(20, 20, 20, 20)
        }

        // Kotak Pesan / Log Transmisi
        logBox = TextView(this).apply {
            text = "[SISTEM] STANDBY - MENUNGGU TRANSMISI...\n"
            setTextColor(Color.WHITE)
            setTypeface(Typeface.MONOSPACE)
            background = borderStyle
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300
            ).apply { setMargins(0, 30, 0, 30) }
        }

        // Status Koneksi UDP
        val ipText = TextView(this).apply {
            text = "AUTO-DISCOVERY (PORT 50000) | P2P OFFLINE"
            setTextColor(cyanNeon)
            textSize = 12f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // Indikator Status Utama
        statusText = TextView(this).apply {
            text = "ONLINE"
            setTextColor(Color.GREEN)
            textSize = 18f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // Tombol PTT Bulat Tengah
        val pttButton = Button(this).apply {
            text = "\n\nTEKAN UNTUK\nBICARA\n\n"
            setTextColor(Color.BLACK)
            textSize = 18f
            setTypeface(Typeface.DEFAULT_BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(cyanNeon)
                setStroke(10, cyanDark)
            }
            layoutParams = LinearLayout.LayoutParams(400, 400).apply {
                gravity = Gravity.CENTER
                setMargins(0, 50, 0, 0)
            }
        }

        rootLayout.addView(titleText)
        rootLayout.addView(callsignInput)
        rootLayout.addView(logBox)
        rootLayout.addView(ipText)
        rootLayout.addView(statusText)
        rootLayout.addView(pttButton)
        setContentView(rootLayout)

        // --- LOGIKA JARINGAN UDP P2P ---
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        socket = DatagramSocket(port).apply { broadcast = true }
        startListening()

        pttButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isRecording = true
                    statusText.text = "TRANSMITTING..."
                    statusText.setTextColor(Color.RED)
                    pttButton.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.RED)
                    }
                    startTransmitting()
                }
                MotionEvent.ACTION_UP -> {
                    isRecording = false
                    statusText.text = "ONLINE"
                    statusText.setTextColor(Color.GREEN)
                    pttButton.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(cyanNeon)
                        setStroke(10, cyanDark)
                    }
                }
            }
            true
        }
    }

    private fun startListening() {
        thread {
            val minBufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSize, AudioTrack.MODE_STREAM)
            audioTrack.play()

            val buffer = ByteArray(minBufSize)
            while (true) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    if (!isRecording) {
                        audioTrack.write(packet.data, 0, packet.length)
                        runOnUiThread {
                            val senderIP = packet.address.hostAddress
                            statusText.text = "MENERIMA SUARA"
                            statusText.setTextColor(Color.YELLOW)
                            logBox.text = "[INCOMING] Menerima transmisi dari $senderIP\n"
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun startTransmitting() {
        thread {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return@thread
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSize)
            val buffer = ByteArray(minBufSize)
            val broadcastAddress = InetAddress.getByName("255.255.255.255")

            audioRecord.startRecording()
            runOnUiThread {
                val cs = callsignInput.text.toString()
                logBox.text = "[OUTGOING] $cs melakukan transmisi...\n"
            }
            
            while (isRecording) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val packet = DatagramPacket(buffer, read, broadcastAddress, port)
                    socket?.send(packet)
                }
            }
            audioRecord.stop()
            audioRecord.release()
            
            runOnUiThread {
                logBox.text = "[SISTEM] Transmisi selesai. Standby.\n"
            }
        }
    }
}
