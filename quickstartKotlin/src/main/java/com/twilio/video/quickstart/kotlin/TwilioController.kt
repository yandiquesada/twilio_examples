package com.twilio.video.quickstart.kotlin

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import com.twilio.video.*
import com.twllio.video.quickstart.kotlin.utils.RoomEventHandler
import com.twllio.video.quickstart.kotlin.utils.TwilioRoomListenerResolver
import kotlinx.android.synthetic.main.content_video.*

class TwilioController(val context: Context, val roomEventHandler: RoomEventHandler) {

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1
    private val TAG = "VideoActivity"

    /*
     * You must provide a Twilio Access Token to connect to the Video service
     */
    private val TWILIO_ACCESS_TOKEN = BuildConfig.TWILIO_ACCESS_TOKEN
    private val ACCESS_TOKEN_SERVER = BuildConfig.TWILIO_ACCESS_TOKEN_SERVER

    /*
     * Access token used to connect. This field will be set either from the console generated token
     * or the request to the token server.
     */
    private lateinit var accessToken: String

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private var room: Room? = null
    private var localParticipant: LocalParticipant? = null

    /*
     * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private val audioCodec: AudioCodec
        get() {
            val audioCodecName = sharedPreferences.getString(SettingsActivity.PREF_AUDIO_CODEC,
                    SettingsActivity.PREF_AUDIO_CODEC_DEFAULT)

            return when (audioCodecName) {
                IsacCodec.NAME -> IsacCodec()
                OpusCodec.NAME -> OpusCodec()
                PcmaCodec.NAME -> PcmaCodec()
                PcmuCodec.NAME -> PcmuCodec()
                G722Codec.NAME -> G722Codec()
                else -> OpusCodec()
            }
        }
    private val videoCodec: VideoCodec
        get() {
            val videoCodecName = sharedPreferences.getString(SettingsActivity.PREF_VIDEO_CODEC,
                    SettingsActivity.PREF_VIDEO_CODEC_DEFAULT)

            return when (videoCodecName) {
                Vp8Codec.NAME -> {
                    val simulcast = sharedPreferences.getBoolean(
                            SettingsActivity.PREF_VP8_SIMULCAST,
                            SettingsActivity.PREF_VP8_SIMULCAST_DEFAULT)
                    Vp8Codec(simulcast)
                }
                H264Codec.NAME -> H264Codec()
                Vp9Codec.NAME -> Vp9Codec()
                else -> Vp8Codec()
            }
        }

    private val enableAutomaticSubscription: Boolean
        get() {
            return sharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_AUTOMATIC_SUBSCRIPTION, SettingsActivity.PREF_ENABLE_AUTOMATIC_SUBCRIPTION_DEFAULT)
        }

    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private val encodingParameters: EncodingParameters
        get() {
            val defaultMaxAudioBitrate = SettingsActivity.PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT
            val defaultMaxVideoBitrate = SettingsActivity.PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT
            val maxAudioBitrate = Integer.parseInt(
                    sharedPreferences.getString(SettingsActivity.PREF_SENDER_MAX_AUDIO_BITRATE,
                            defaultMaxAudioBitrate) ?: defaultMaxAudioBitrate
            )
            val maxVideoBitrate = Integer.parseInt(
                    sharedPreferences.getString(SettingsActivity.PREF_SENDER_MAX_VIDEO_BITRATE,
                            defaultMaxVideoBitrate) ?: defaultMaxVideoBitrate
            )

            return EncodingParameters(maxAudioBitrate, maxVideoBitrate)
        }

    /*
     * Room events listener
     */
    private val roomListener: Room.Listener by lazy {
        TwilioRoomListenerResolver.getRoomListener(roomEventHandler)
    }
}