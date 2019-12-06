package com.twilio.video.quickstart.kotlin

import android.content.Context
import android.media.AudioManager
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import com.twilio.video.*
import com.twllio.video.quickstart.kotlin.utils.*
import kotlinx.android.synthetic.main.content_video.*
import java.lang.Exception

class TwilioController(val context: Context, val roomEventHandler: RoomEventHandler, val remoteParticipantEventHandler: RemoteParticipantEventHandler) {

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

    private val participantListener: RemoteParticipant.Listener by lazy {
        RemoteParticipantListenerResolver.getRemoteParticipantListener(remoteParticipantEventHandler)
    }

    private var localAudioTrack: LocalAudioTrack? = null
    private var localVideoTrack: LocalVideoTrack? = null
    private var alertDialog: android.support.v7.app.AlertDialog? = null
    private val cameraCapturerCompat: CameraCapturerCompat by lazy {
        CameraCapturerCompat(context, CameraUtils.getAvailableCameraSource())
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var participantIdentity: String? = null
    private var previousAudioMode = 0
    private var previousMicrophoneMute = false
    private lateinit var localVideoView: VideoRenderer
    private var disconnectedFromOnDestroy = false
    private var isSpeakerPhoneEnabled = true

    /**
     * public methods
     */

    fun createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(context, true)

        // Share your camera
        localVideoTrack = LocalVideoTrack.create(context,
                true,
                cameraCapturerCompat.videoCapturer)
    }

    fun recreateLocalVideoTrack() {
        localVideoTrack = if (localVideoTrack == null && CameraUtils.checkPermissionForCameraAndMicrophone(context)) {
            LocalVideoTrack.create(context,
                    true,
                    cameraCapturerCompat.videoCapturer)
        } else {
            localVideoTrack
        }
        localVideoTrack?.addRenderer(localVideoView)
    }


    /**
     * Listener implementation
     */

    /*
     * Room events listener
     * TODO: maybe would be a good idea to have the listener implememntation here in order to avoid complexity
     *  in the first iteration!
     */
    //private val roomListener: Room.Listener by lazy {
    //    TwilioRoomListenerResolver.getRoomListener(roomEventHandler)
    //}

    private val roomListener = object : Room.Listener {
        override fun onConnected(room: Room) {
            localParticipant = room.localParticipant
            roomEventHandler.onConnected(room)
        }

        override fun onReconnected(room: Room) {
            roomEventHandler.onReconnected(room)
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            roomEventHandler.onReconnecting(room, twilioException)
        }

        override fun onConnectFailure(room: Room, twilioException: TwilioException) {
            roomEventHandler.onConnectFailure(room, twilioException)
        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            localParticipant = null
            roomEventHandler.onDisconnected(room, twilioException)
        }

        override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
            //addRemoteParticipant(participant)
            roomEventHandler.onParticipantConnected(room, participant)
        }

        override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
            //removeRemoteParticipant(participant)
            roomEventHandler.onParticipantDisconnected(room, participant)
        }

        override fun onRecordingStarted(room: Room) {
            /*
             * Indicates when media shared to a Room is being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            Log.d(TAG, "onRecordingStarted")
        }

        override fun onRecordingStopped(room: Room) {
            /*
             * Indicates when media shared to a Room is no longer being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            Log.d(TAG, "onRecordingStopped")
        }
    }


}