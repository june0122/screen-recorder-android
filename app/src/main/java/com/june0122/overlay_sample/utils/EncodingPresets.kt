@file:Suppress("unused")

package com.june0122.overlay_sample.utils

const val Kbps = 1000
const val Mbps = 1000000

const val VIDEO_TARGET_BITRATE_720P_60 = 20 * Mbps
const val VIDEO_MAX_BITRATE_720P_60 = 40 * Mbps

const val VIDEO_TARGET_BITRATE_1080P_30 = 28 * Mbps
const val VIDEO_MAX_BITRATE_1080P_30 = 56 * Mbps

const val VIDEO_TARGET_BITRATE_1080P_60 = 45 * Mbps
const val VIDEO_MAX_BITRATE_1080P_60 = 90 * Mbps

const val VIDEO_TARGET_BITRATE_4K_24 = 95 * Mbps
const val VIDEO_MAX_BITRATE_4K_24 = 190 * Mbps

const val VIDEO_TARGET_BITRATE_4K_60 = 110 * Mbps
const val VIDEO_MAX_BITRATE_4K_60 = 220 * Mbps

const val AUDIO_BITRATE_128K = 128 * Kbps
const val AUDIO_BITRATE_256K = 256 * Kbps
const val AUDIO_BITRATE_320K = 320 * Kbps

/**
 * 44100 Hz is the Audacity default setting.
 * It is highly recommended that you use this setting
 * unless you have good reasons to deviate from it.
 */
const val AUDIO_SAMPLING_RATE_44100 = 44100

/**
 * 48000 Hz (48 kHz) is the sample rate used for DVDs
 * so if you are creating DVD audio discs from your Audacity projects
 * you may prefer to work with this setting.
 */
const val AUDIO_SAMPLING_RATE_48000 = 48000