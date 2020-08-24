package com.june0122.overlay_sample.service

import android.app.Activity.RESULT_OK
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.june0122.overlay_sample.utils.*
import java.io.File
import java.io.FileOutputStream
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.experimental.and

class AudioCaptureService : Service() {
    companion object {
        private const val LOG_TAG = "AudioCaptureService"

        private const val NUM_SAMPLES_PER_READ = 1024
        private const val BYTES_PER_SAMPLE = 2 // 2 bytes since we hardcoded the PCM 16-bit format
        private const val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"

        var AUDIO_PATH: File? = null
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private lateinit var audioCaptureThread: Thread
    private var audioRecord: AudioRecord? = null

    private val context: Context
        get() = this

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager =
                applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    NotificationCreator.getNotification(this, context, intent)

                    mediaProjection = mediaProjectionManager
                            .getMediaProjection(
                                    RESULT_OK,
                                    intent.getParcelableExtra(EXTRA_RESULT_DATA) as Intent
                            ) as MediaProjection

                    // 명시적 NullPointerException 사용 여부 고려 필요
                    startAudioCapture(mediaProjection ?: throw NullPointerException("mediaProjection is null"))
                    START_STICKY
                }

                ACTION_STOP -> {
                    stopAudioCapture()
                    context.stopService(intent)

                    START_NOT_STICKY
                }

                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            START_NOT_STICKY
        }
    }

    private fun startAudioCapture(mp: MediaProjection) {
        val config =
                AudioPlaybackCaptureConfiguration.Builder(mp)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .build()

        // PCM signed 16 bit, little endian, stereo
        val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLING_RATE_44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

        audioRecord = config.let {
            AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
                    .setAudioPlaybackCaptureConfig(it)
                    .build()
        }

        audioRecord?.startRecording()
        audioCaptureThread = thread(start = true) {
            val outputFile = createAudioFile()
//            AUDIO_PATH = outputFile
            Log.d(LOG_TAG, "Created file for capture target: ${outputFile.absolutePath}")
            writeAudioToFile(outputFile)
        }
    }

    private fun createAudioFile(): File {
        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) audioCapturesDirectory.mkdirs()
        val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.KOREA).format(Date())
        val fileName = "AudioCapture-$timestamp.pcm"
        return File(audioCapturesDirectory.absolutePath + "/" + fileName)
    }

    private fun writeAudioToFile(outputFile: File) {
        val fileOutputStream = FileOutputStream(outputFile)
        val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)

        while (!audioCaptureThread.isInterrupted) {
            audioRecord?.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)

            /**
             * This loop should be as fast as possible to avoid artifacts in the captured audio
             * You can uncomment the following line to see the capture samples but
             * that will incur a performance hit due to logging I/O.
             */
//            Log.v(LOG_TAG, "Audio samples captured: ${capturedAudioSamples.toList()}")

            fileOutputStream.write(
                    capturedAudioSamples.toByteArray(),
                    0,
                    BUFFER_SIZE_IN_BYTES
            )
        }

        fileOutputStream.close()

        val outputFileSize = outputFile.length().toFloat() / (1024.0 * 1024.0)
        Log.d(LOG_TAG, "Audio capture finished for ${outputFile.absolutePath}.")
        Log.d(LOG_TAG, "File size is ${outputFileSize.roundOffDecimal()} MB.")
    }

    private fun stopAudioCapture() {
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread.interrupt()
        audioCaptureThread.join()

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        stopSelf()


        /** SetOverlayFragment() 클래스 내부의 mediaProjection 에 영향을 줌 */
//        mediaProjection?.stop()
//        stopSelf()

        val bufferSize = AudioTrack.getMinBufferSize(
                AUDIO_SAMPLING_RATE_44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        )

        val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLING_RATE_44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

        val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        val audio = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

        audio.play()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ShortArray.toByteArray(): ByteArray {
        // Samples get translated into bytes following "little-endianness":
        // least significant byte first and the most significant byte last
        val bytes = ByteArray(size * 2)
        for (i in 0 until size) {
            bytes[i * 2] = (this[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
            this[i] = 0
        }
        return bytes
    }

    override fun onDestroy() {
        Toast.makeText(this, "Audio Capture Service Done", Toast.LENGTH_SHORT).show()
    }
}


