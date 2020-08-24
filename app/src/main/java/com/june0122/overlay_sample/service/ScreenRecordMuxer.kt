package com.june0122.overlay_sample.service

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.lang.Exception
import java.nio.ByteBuffer

class ScreenRecordMuxer {

    companion object {
        private val TAG = StreamRecordingMuxer::class.java.simpleName
        private const val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
        private const val CODEC_TIMEOUT = 5000
    }

    // Audio state
    private var mediaCodec: MediaCodec? = null
    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var audioFormat: MediaFormat
    private lateinit var audioExtractor: MediaExtractor
    private lateinit var codecInputBuffers: Array<ByteBuffer>
    private lateinit var codecOutputBuffers: Array<ByteBuffer>
    private var audioBufferInfo: MediaCodec.BufferInfo? = null
    private var audioPath: String = ""
    private var outputPath: String = ""
    private var audioTrackIndex = 0
    private var writeAudioTrackIndex = 0
    private var audioTrackId = 0
    private var totalBytesRead = 0
    private var presentationTimeUs = 0.0

    // Video state
    private var videoTrackId = 0
    private lateinit var videoExtractor: MediaExtractor
    private lateinit var videoFormat: MediaFormat
    private var videoPath: String = ""
    private var videoTrackIndex = 0
    private var writeVideoTrackIndex = 0
    private var maxFrameInputSize = 0
    private var rotationDegrees = 0
    private var frameRate = 0
    private var videoDuration = 0L

    fun setVideoPath(videoPath: String) {
        this.videoPath = videoPath
    }

    fun setAudioPath(audioPath: String) {
        this.audioPath = audioPath
    }

    fun setOutputPath(outputPath: String) {
        this.outputPath = outputPath
    }

    fun getVideoInfo() {
        Log.d(TAG, "Get Video Info")


        videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoPath)

        val videoTrackCount = videoExtractor.trackCount

        for (i in 0 until videoTrackCount) {
            videoFormat = videoExtractor.getTrackFormat(i)

            val mimeType = videoFormat.getString(MediaFormat.KEY_MIME) as String

            if (mimeType.startsWith("video/")) {
                videoTrackIndex = i
                maxFrameInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                frameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                videoDuration = videoFormat.getLong(MediaFormat.KEY_DURATION)
                break
            }
        }

        if (videoTrackIndex < 0) {
            Log.d(TAG, "Input File Error : No Video Track")
            return
        }
    }

    fun getAudioInfo() {
        Log.d(TAG, "Get Audio Info : $audioPath")

        audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(audioPath)

        val audioTrackCount = audioExtractor.trackCount

        for (i in 0 until audioTrackCount) {
            audioFormat = audioExtractor.getTrackFormat(i)

            val mimeType = audioFormat.getString(MediaFormat.KEY_MIME) as String

            if (mimeType.startsWith("audio/")) {
                audioTrackIndex = i
                break
            }

            if (audioTrackId < 0) {
                Log.d(TAG, "Input File Error : No Audio Track")
                return
            }
        }
    }

    fun writeVideoData() {
        val videoBufferInfo = MediaCodec.BufferInfo()
        mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat)
        writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat)

        mediaMuxer.start()

        val byteBuffer = ByteBuffer.allocate(maxFrameInputSize)
        videoExtractor.unselectTrack(videoTrackIndex)
        videoExtractor.selectTrack(videoTrackIndex)

        while (true) {
            val readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0)
            if (readVideoSampleSize < 0) {
                videoExtractor.unselectTrack(videoTrackIndex)
                break
            }

            val videoSampleTime = videoExtractor.sampleTime

            videoBufferInfo.apply {
                size = readVideoSampleSize
                presentationTimeUs = videoSampleTime
                offset = 0
                flags = videoExtractor.sampleFlags
            }

            mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBuffer, videoBufferInfo)
            videoExtractor.advance()
        }
    }

    fun writeAudioData() {
        var audioPresentationTimeUs = 0L
        var lastEndAudioTimeUs = 0L
        val audioBufferInfo = MediaCodec.BufferInfo()
        val byteBuffer = ByteBuffer.allocate(maxFrameInputSize)

        audioExtractor.selectTrack(audioTrackIndex)


        while (true) {
            val readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0)

            if (readAudioSampleSize < 0) {
                audioExtractor.unselectTrack(audioTrackIndex)

                if (audioPresentationTimeUs >= videoDuration) {
                    break
                } else {
                    lastEndAudioTimeUs += audioPresentationTimeUs
                    audioExtractor.selectTrack(audioTrackIndex)
                    continue
                }
            }

            val audioSampleTime = audioExtractor.sampleTime
            audioBufferInfo.size = readAudioSampleSize
            audioBufferInfo.presentationTimeUs = audioSampleTime + lastEndAudioTimeUs

            if (audioBufferInfo.presentationTimeUs > videoDuration) {
                audioExtractor.unselectTrack(audioTrackIndex)
                break
            }

            audioPresentationTimeUs = audioBufferInfo.presentationTimeUs
            audioBufferInfo.offset = 0
            audioBufferInfo.flags = audioExtractor.sampleFlags
            mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBuffer, audioBufferInfo)
            audioExtractor.advance()
        }
    }

    fun stop() {
        try {
            mediaMuxer.stop()
            mediaMuxer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            videoExtractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            audioExtractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}