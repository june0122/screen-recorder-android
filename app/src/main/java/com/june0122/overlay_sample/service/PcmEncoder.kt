package com.june0122.overlay_sample.service

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * PCMEncoder allows encoding multiple input streams of PCM data into one, compressed audio file.
 * Creates encoder with given params for output file
 *
 * @param bitrate
 * @param sampleRate
 * @param channelCount
 */

class PCMEncoder(private val bitrate: Int, private val sampleRate: Int, private val channelCount: Int) {
    companion object {
        private const val TAG = "PCMEncoder"
        private const val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
        private const val CODEC_TIMEOUT = 1000L
    }

    private lateinit var mediaFormat: MediaFormat
    private lateinit var mediaCodec: MediaCodec
    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var outputBuffer: ByteBuffer
    private lateinit var bufferInfo: MediaCodec.BufferInfo
    private var outputPath: String? = null
    private var audioTrackId = 0
    private var totalBytesRead = 0
    private var presentationTimeUs = 0L

    fun setOutputPath(outputPath: String?) {
        this.outputPath = outputPath
    }

    fun prepare() {
        checkNotNull(outputPath) { "The output path must be set first!" }
        try {
            mediaFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, sampleRate, channelCount)
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            mediaCodec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()
            bufferInfo = MediaCodec.BufferInfo()
            mediaMuxer = MediaMuxer(outputPath!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            totalBytesRead = 0
            presentationTimeUs = 0L
        } catch (e: IOException) {
            Log.e(TAG, "Exception while initializing PCMEncoder", e)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping PCMEncoder")
        handleEndOfStream()
        mediaCodec.stop()
        mediaCodec.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

    private fun handleEndOfStream() {
        val inputBufferIndex = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT)
        mediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                presentationTimeUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
        )
        writeOutputs()
    }

    /**
     * Encodes input stream
     *
     * @param inputStream
     * @param sampleRate sample rate of input stream
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encode(inputStream: InputStream, sampleRate: Int) {
        val tempBuffer = ByteArray(2 * sampleRate)
        var hasMoreData = true
        var stop = false

        while (!stop) {
            var inputBufferId = 0
            var currentBatchRead = 0

            while (inputBufferId != -1 && hasMoreData && currentBatchRead <= 50 * sampleRate) {
                inputBufferId = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT)

                if (inputBufferId >= 0) {
                    inputBuffer = mediaCodec.getInputBuffer(inputBufferId) as ByteBuffer
                    inputBuffer.clear()

                    val bytesRead: Int? = inputStream.read(tempBuffer, 0, inputBuffer.limit())

                    if (bytesRead == -1) {
                        mediaCodec.queueInputBuffer(inputBufferId, 0, 0, presentationTimeUs, 0)
                        hasMoreData = false
                        stop = true
                    } else {
                        if (bytesRead != null) {
                            totalBytesRead += bytesRead
                            currentBatchRead += bytesRead
                            inputBuffer.put(tempBuffer, 0, bytesRead)
                            mediaCodec.queueInputBuffer(inputBufferId, 0, bytesRead, presentationTimeUs, 0)
                            presentationTimeUs = 1000000L * (totalBytesRead / (2 * channelCount)) / sampleRate
                        }
                    }
                }
            }
            writeOutputs()
        }
        inputStream.close()
    }

    private fun writeOutputs() {
        var outputBufferId = 0
        while (outputBufferId != MediaCodec.INFO_TRY_AGAIN_LATER) {
            outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT)
            if (outputBufferId >= 0) {
                outputBuffer = mediaCodec.getOutputBuffer(outputBufferId) as ByteBuffer
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && bufferInfo.size != 0) {
                    mediaCodec.releaseOutputBuffer(outputBufferId, false)
                } else {
                    mediaMuxer.writeSampleData(audioTrackId, outputBuffer, bufferInfo)
                    mediaCodec.releaseOutputBuffer(outputBufferId, false)
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = mediaCodec.outputFormat
                audioTrackId = mediaMuxer.addTrack(mediaFormat)
                mediaMuxer.start()
            }
        }
    }
}