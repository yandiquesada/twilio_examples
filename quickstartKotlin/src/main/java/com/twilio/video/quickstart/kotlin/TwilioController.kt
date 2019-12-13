package com.twilio.video.quickstart.kotlin

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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

    val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1
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
    var room: Room? = null
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

    var localAudioTrack: LocalAudioTrack? = null
    var localVideoTrack: LocalVideoTrack? = null
    private var alertDialog: android.support.v7.app.AlertDialog? = null
    val cameraCapturerCompat: CameraCapturerCompat by lazy {
        CameraCapturerCompat(context, CameraUtils.getAvailableCameraSource())
    }

    //TODO: could be out of this class, since is not from twilio
    val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var participantIdentity: String? = null
    private var previousAudioMode = 0
    private var previousMicrophoneMute = false
    //private lateinit var localVideoView: VideoRenderer

    private var isSpeakerPhoneEnabled = true

    /**
     * private methods
     */

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

    fun recreateLocalVideoTrack(localVideoView: VideoRenderer) {
        localVideoTrack = if (localVideoTrack == null && CameraUtils.checkPermissionForCameraAndMicrophone(context)) {
            LocalVideoTrack.create(context,
                    true,
                    cameraCapturerCompat.videoCapturer)
        } else {
            localVideoTrack
        }
        localVideoTrack?.addRenderer(localVideoView)
    }

    fun configureAudio(enable: Boolean) {
        with(audioManager) {
            if (enable) {
                previousAudioMode = audioManager.mode
                // Request audio focus before making any device switch
                requestAudioFocus()
                /*
                 * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
                 * to be in this mode when playout and/or recording starts for the best
                 * possible VoIP performance. Some devices have difficulties with
                 * speaker mode if this is not set.
                 */
                mode = AudioManager.MODE_IN_COMMUNICATION
                /*
                 * Always disable microphone mute during a WebRTC call.
                 */
                previousMicrophoneMute = isMicrophoneMute
                isMicrophoneMute = false
            } else {
                mode = previousAudioMode
                abandonAudioFocus(null)
                isMicrophoneMute = previousMicrophoneMute
            }
        }
    }

    fun updateEncodingParameters() {
        localParticipant?.setEncodingParameters(encodingParameters)
    }

    fun shareLocalVideoTrack() {
        localVideoTrack?.let { localParticipant?.publishTrack(it) }
    }

    fun restoreAudio() {
        audioManager.isSpeakerphoneOn = isSpeakerPhoneEnabled
    }

    fun disconnectRoom() {
        room?.disconnect()
    }

    fun releaseAudioAndVideoTracks() {
        localAudioTrack?.release()
        localVideoTrack?.release()
    }

    fun unpublishTrack() {
        localVideoTrack?.let {  localParticipant?.unpublishTrack(it) }
    }

    fun releaseVideoTrack() {
        localVideoTrack?.release()
        localVideoTrack = null
    }

    //TODO: Move to Audio utils
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { }
                    .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    fun connectToRoom(roomName: String) {
        configureAudio(true)
        val connectOptionsBuilder = ConnectOptions.Builder(accessToken)
                .roomName(roomName)

        /*
         * Add local audio track to connect options to share with participants.
         */
        localAudioTrack?.let { connectOptionsBuilder.audioTracks(listOf(it)) }

        /*
         * Add local video track to connect options to share with participants.
         */
        localVideoTrack?.let { connectOptionsBuilder.videoTracks(listOf(it)) }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(listOf(audioCodec))
        connectOptionsBuilder.preferVideoCodecs(listOf(videoCodec))

        /*
         * Set the sender side encoding parameters.
         */
        connectOptionsBuilder.encodingParameters(encodingParameters)

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription)

        room = Video.connect(context, connectOptionsBuilder.build(), roomListener)
    }

    fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {

    }

    /**
     * Access Token Implementaion
     */

    fun setAccessToken() {
        if (!BuildConfig.USE_TOKEN_SERVER) {
            /*
             * OPTION 1 - Generate an access token from the getting started portal
             * https://www.twilio.com/console/video/dev-tools/testing-tools and add
             * the variable TWILIO_ACCESS_TOKEN setting it equal to the access token
             * string in your local.properties file.
             */
            this.accessToken = TWILIO_ACCESS_TOKEN
        } else {
            /*
             * OPTION 2 - Retrieve an access token from your own web app.
             * Add the variable ACCESS_TOKEN_SERVER assigning it to the url of your
             * token server and the variable USE_TOKEN_SERVER=true to your
             * local.properties file.
             */
            //retrieveAccessTokenfromServer()
        }
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
            roomEventHandler.onParticipantConnected(room, participant)
        }

        override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
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

    /*
     * RemoteParticipant events listener
     */
    //private val participantListener: RemoteParticipant.Listener by lazy {
    //    RemoteParticipantListenerResolver.getRemoteParticipantListener(remoteParticipantEventHandler)
    //}

    val participantListener = object : RemoteParticipant.Listener {
        override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            remoteParticipantEventHandler.onAudioTrackPublished(remoteParticipant, remoteAudioTrackPublication)
        }

        override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            remoteParticipantEventHandler.onAudioTrackUnpublished(remoteParticipant, remoteAudioTrackPublication)
        }

        override fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                                          remoteDataTrackPublication: RemoteDataTrackPublication) {
            remoteParticipantEventHandler.onDataTrackPublished(remoteParticipant, remoteDataTrackPublication)
        }

        override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                            remoteDataTrackPublication: RemoteDataTrackPublication) {
            remoteParticipantEventHandler.onDataTrackUnpublished(remoteParticipant, remoteDataTrackPublication)
        }

        override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                                           remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            remoteParticipantEventHandler.onVideoTrackPublished(remoteParticipant, remoteVideoTrackPublication)
        }

        override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                             remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            remoteParticipantEventHandler.onVideoTrackUnpublished(remoteParticipant, remoteVideoTrackPublication)
        }

        override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                            remoteAudioTrack: RemoteAudioTrack) {
            remoteParticipantEventHandler.onAudioTrackSubscribed(remoteParticipant, remoteAudioTrackPublication, remoteAudioTrack)
        }

        override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                              remoteAudioTrack: RemoteAudioTrack) {
            remoteParticipantEventHandler.onAudioTrackUnsubscribed(remoteParticipant, remoteAudioTrackPublication, remoteAudioTrack)
        }

        override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                    twilioException: TwilioException) {
            remoteParticipantEventHandler.onAudioTrackSubscriptionFailed(remoteParticipant, remoteAudioTrackPublication, twilioException)
        }

        override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                                           remoteDataTrackPublication: RemoteDataTrackPublication,
                                           remoteDataTrack: RemoteDataTrack) {
            remoteParticipantEventHandler.onDataTrackSubscribed(remoteParticipant, remoteDataTrackPublication, remoteDataTrack)
        }

        override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                             remoteDataTrackPublication: RemoteDataTrackPublication,
                                             remoteDataTrack: RemoteDataTrack) {
            remoteParticipantEventHandler.onDataTrackUnsubscribed(remoteParticipant, remoteDataTrackPublication, remoteDataTrack)
        }

        override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                   remoteDataTrackPublication: RemoteDataTrackPublication,
                                                   twilioException: TwilioException) {
            remoteParticipantEventHandler.onDataTrackSubscriptionFailed(remoteParticipant, remoteDataTrackPublication, twilioException)
        }

        override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                                            remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                            remoteVideoTrack: RemoteVideoTrack) {
            remoteParticipantEventHandler.onVideoTrackSubscribed(remoteParticipant, remoteVideoTrackPublication, remoteVideoTrack)
        }

        override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                              remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                              remoteVideoTrack: RemoteVideoTrack) {
            remoteParticipantEventHandler.onVideoTrackUnsubscribed(remoteParticipant, remoteVideoTrackPublication, remoteVideoTrack)
        }

        override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                    remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                                    twilioException: TwilioException) {
            remoteParticipantEventHandler.onVideoTrackSubscriptionFailed(remoteParticipant, remoteVideoTrackPublication, twilioException)
        }

        override fun onAudioTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteAudioTrackPublication: RemoteAudioTrackPublication) {
        }

        override fun onVideoTrackEnabled(remoteParticipant: RemoteParticipant,
                                         remoteVideoTrackPublication: RemoteVideoTrackPublication) {
        }

        override fun onVideoTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteVideoTrackPublication: RemoteVideoTrackPublication) {
        }

        override fun onAudioTrackDisabled(remoteParticipant: RemoteParticipant,
                                          remoteAudioTrackPublication: RemoteAudioTrackPublication) {
        }
    }

}