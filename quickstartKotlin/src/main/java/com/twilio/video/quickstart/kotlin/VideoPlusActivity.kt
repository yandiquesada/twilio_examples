package com.twilio.video.quickstart.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.media.AudioManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import com.twilio.video.*
import com.twllio.video.quickstart.kotlin.utils.CameraUtils
import com.twllio.video.quickstart.kotlin.utils.RemoteParticipantEventHandler
import com.twllio.video.quickstart.kotlin.utils.RoomEventHandler
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.content_video.*

class VideoPlusActivity : AppCompatActivity(), RoomEventHandler, RemoteParticipantEventHandler {

    private val TAG = "VideoPlusActivity"

    private val twilioController = TwilioController(this, this, this)

    private lateinit var localVideoView: VideoRenderer

    /**
     * Life cycle meyhods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        /*
         * Set local video view to primary view
         */
        localVideoView = primaryVideoView

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        /*
         * Needed for setting/abandoning audio focus during call
         */
        twilioController.audioManager.isSpeakerphoneOn = true

        /*
         * Set access token
         */
        twilioController.setAccessToken()

        /*
         * Request permissions.
         */
        CameraUtils.requestPermissionForCameraAndMicrophone(this, twilioController.CAMERA_MIC_PERMISSION_REQUEST_CODE)

        /*
         * Set the initial state of the UI
         */
        initializeUI()
    }

    override fun onResume() {
        super.onResume()

        twilioController.recreateLocalVideoTrack(localVideoView)

        /*
         * If connected to a Room then share the local video track.
         */
        twilioController.shareLocalVideoTrack()

        /*
         * Update encoding parameters if they have changed.
         */
        twilioController.updateEncodingParameters()

        /*
         * Route audio through cached value.
         */
        twilioController.restoreAudio()

        /*
         * Update reconnecting UI
         * TODO: move this to the Twilio abstraction layer
         */
        twilioController.room?.let {
            reconnectingProgressBar.visibility = if (it.state != Room.State.RECONNECTING)
                View.GONE else
                View.VISIBLE
            videoStatusTextView.text = "Connected to ${it.name}"
        }
    }

    /*
     * The initial state when there is no active room.
     */
    private fun initializeUI() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_video_call_white_24dp))
        connectActionFab.show()
        connectActionFab.setOnClickListener(connectActionClickListener())
        switchCameraActionFab.show()
        switchCameraActionFab.setOnClickListener(switchCameraClickListener())
        localVideoActionFab.show()
        localVideoActionFab.setOnClickListener(localVideoClickListener())

        //screenShare.show()
        //screenShare.setOnClickListener(screenShareListener())

        muteActionFab.show()
        muteActionFab.setOnClickListener(muteClickListener())
    }

    /**
     * UI Listener implementation section
     */

    private fun muteClickListener(): View.OnClickListener {
        return View.OnClickListener {
            /*
             * Enable/disable the local audio track. The results of this operation are
             * signaled to other Participants in the same Room. When an audio track is
             * disabled, the audio is muted.
             */
            twilioController.localAudioTrack?.let {
                val enable = !it.isEnabled
                it.enable(enable)
                val icon = if (enable)
                    R.drawable.ic_mic_white_24dp
                else
                    R.drawable.ic_mic_off_black_24dp
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                        this@VideoPlusActivity, icon))
            }
        }
    }

    private fun localVideoClickListener(): View.OnClickListener {
        return View.OnClickListener {
            /*
             * Enable/disable the local video track
             */
            twilioController.localVideoTrack?.let {
                val enable = !it.isEnabled
                it.enable(enable)
                val icon: Int
                if (enable) {
                    icon = R.drawable.ic_videocam_white_24dp
                    switchCameraActionFab.show()
                } else {
                    icon = R.drawable.ic_videocam_off_black_24dp
                    switchCameraActionFab.hide()
                }
                localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(this@VideoPlusActivity, icon))
            }
        }
    }

    private fun switchCameraClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val cameraSource = twilioController.cameraCapturerCompat.cameraSource
            twilioController.cameraCapturerCompat.switchCamera()
            if (thumbnailVideoView.visibility == View.VISIBLE) {
                thumbnailVideoView.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
            } else {
                primaryVideoView.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
            }
        }
    }

    private fun connectActionClickListener(): View.OnClickListener {
        return View.OnClickListener { showConnectDialog() }
    }

    private var alertDialog: android.support.v7.app.AlertDialog? = null

    /*
     * Creates an connect UI dialog
     */
    private fun showConnectDialog() {
        val roomEditText = EditText(this)
        alertDialog = createConnectDialog(roomEditText,
                connectClickListener(roomEditText), cancelConnectDialogClickListener(), this)
        alertDialog!!.show()
    }

    private fun cancelConnectDialogClickListener(): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            initializeUI()
            alertDialog!!.dismiss()
        }
    }

    private fun connectClickListener(roomEditText: EditText): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            /*
             * Connect to room
             */

            twilioController.connectToRoom(roomEditText.text.toString())
            setDisconnectAction()
        }
    }

    private fun createConnectDialog(participantEditText: EditText,
                                    callParticipantsClickListener: DialogInterface.OnClickListener,
                                    cancelClickListener: DialogInterface.OnClickListener,
                                    context: Context): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context).apply {
            setIcon(R.drawable.ic_video_call_white_24dp)
            setTitle("Connect to a room")
            setPositiveButton("Connect", callParticipantsClickListener)
            setNegativeButton("Cancel", cancelClickListener)
            setCancelable(false)
        }

        setRoomNameFieldInDialog(participantEditText, alertDialogBuilder, context)

        return alertDialogBuilder.create()
    }

    @SuppressLint("RestrictedApi")
    private fun setRoomNameFieldInDialog(roomNameEditText: EditText,
                                         alertDialogBuilder: AlertDialog.Builder,
                                         context: Context) {
        roomNameEditText.hint = "room name"
        val horizontalPadding = context.resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
        val verticalPadding = context.resources.getDimensionPixelOffset(R.dimen.activity_vertical_margin)
        alertDialogBuilder.setView(roomNameEditText,
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                0)
    }

    private fun setDisconnectAction() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_end_white_24px))
        connectActionFab.show()
        connectActionFab.setOnClickListener(disconnectClickListener())
    }

    private fun disconnectClickListener(): View.OnClickListener {
        return View.OnClickListener {
            /*
             * Disconnect from room
             */
            twilioController.room?.disconnect()
            initializeUI()
        }
    }

    /**
     *   add remote participant and addRemoteParticipantVideo implementations
     *   TODO: this section must be refactorized
     */

    /*
     * Called when participant joins the room
     */
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.visibility == View.VISIBLE) {
            Snackbar.make(connectActionFab,
                    "Multiple participants are not currently support in this UI",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            return
        }
        twilioController.participantIdentity = remoteParticipant.identity
        videoStatusTextView.text = "Participant ${twilioController.participantIdentity} joined"


        //Twilio logic must go inside twilio abstraction layer
        /*
         * Add participant renderer
         */
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let { addRemoteParticipantVideo(it) }
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(twilioController.participantListener)
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        moveLocalVideoToThumbnailView()
        primaryVideoView.mirror = false
        videoTrack.addRenderer(primaryVideoView)
    }

    private fun moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.visibility == View.GONE) {
            thumbnailVideoView.visibility = View.VISIBLE
            with(twilioController.localVideoTrack) {
                this?.removeRenderer(primaryVideoView)
                this?.addRenderer(thumbnailVideoView)
            }
            localVideoView = thumbnailVideoView
            //TODO: move this to the Twilio abstraction layer
            thumbnailVideoView.mirror = twilioController.cameraCapturerCompat.cameraSource ==
                    CameraCapturer.CameraSource.FRONT_CAMERA
        }
    }

    /**
     * Listener implementation section
     */

    override fun onConnected(room: Room) {
        videoStatusTextView.text = "Connected to ${room.name}"
        title = room.name

        // Only one participant is supported
        //TODO: move this from here
        //twilioController.room.remoteParticipants?.firstOrNull()?.let { addRemoteParticipant(it) }
    }

    override fun onReconnected(room: Room) {
        videoStatusTextView.text = "Connected to ${room.name}"
        reconnectingProgressBar.visibility = View.GONE;
    }

    override fun onReconnecting(room: Room, twilioException: TwilioException) {
        videoStatusTextView.text = "Reconnecting to ${room.name}"
        reconnectingProgressBar.visibility = View.VISIBLE;
    }

    override fun onConnectFailure(room: Room, e: TwilioException) {
        videoStatusTextView.text = "Failed to connect"
        twilioController.configureAudio(false)
        initializeUI()
    }

    override fun onDisconnected(room: Room, e: TwilioException?) {
        //localParticipant = null
        videoStatusTextView.text = "Disconnected from ${room.name}"
        reconnectingProgressBar.visibility = View.GONE;
        //this@VideoActivity.room = null
        // Only reinitialize the UI if disconnect was not called from onDestroy()
        //if (!disconnectedFromOnDestroy) {
        //    configureAudio(false)
        //    initializeUI()
        //    moveLocalVideoToPrimaryView()
        //}
    }

    override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
        //addRemoteParticipant(participant)
    }

    override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
        //removeRemoteParticipant(participant)
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

    /**
     *
     */

    override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                                       remoteAudioTrackPublication: RemoteAudioTrackPublication) {
        Log.i(TAG, "onAudioTrackPublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                "name=${remoteAudioTrackPublication.trackName}]")
        videoStatusTextView.text = "onAudioTrackAdded"
    }

    override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                         remoteAudioTrackPublication: RemoteAudioTrackPublication) {
        Log.i(TAG, "onAudioTrackUnpublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
                "name=${remoteAudioTrackPublication.trackName}]")
        videoStatusTextView.text = "onAudioTrackRemoved"
    }

    override fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                                      remoteDataTrackPublication: RemoteDataTrackPublication) {
        Log.i(TAG, "onDataTrackPublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                "name=${remoteDataTrackPublication.trackName}]")
        videoStatusTextView.text = "onDataTrackPublished"
    }

    override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                        remoteDataTrackPublication: RemoteDataTrackPublication) {
        Log.i(TAG, "onDataTrackUnpublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
                "name=${remoteDataTrackPublication.trackName}]")
        videoStatusTextView.text = "onDataTrackUnpublished"
    }

    override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                                       remoteVideoTrackPublication: RemoteVideoTrackPublication) {
        Log.i(TAG, "onVideoTrackPublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                "name=${remoteVideoTrackPublication.trackName}]")
        videoStatusTextView.text = "onVideoTrackPublished"
    }

    override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                         remoteVideoTrackPublication: RemoteVideoTrackPublication) {
        Log.i(TAG, "onVideoTrackUnpublished: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
                "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
                "name=${remoteVideoTrackPublication.trackName}]")
        videoStatusTextView.text = "onVideoTrackUnpublished"
    }

    override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                                        remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                        remoteAudioTrack: RemoteAudioTrack) {
        Log.i(TAG, "onAudioTrackSubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                "name=${remoteAudioTrack.name}]")
        videoStatusTextView.text = "onAudioTrackSubscribed"
    }

    override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                          remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                          remoteAudioTrack: RemoteAudioTrack) {
        Log.i(TAG, "onAudioTrackUnsubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
                "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
                "name=${remoteAudioTrack.name}]")
        videoStatusTextView.text = "onAudioTrackUnsubscribed"
    }

    override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                twilioException: TwilioException) {
        Log.i(TAG, "onAudioTrackSubscriptionFailed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
                "name=${remoteAudioTrackPublication.trackName}]" +
                "[TwilioException: code=${twilioException.code}, " +
                "message=${twilioException.message}]")
        videoStatusTextView.text = "onAudioTrackSubscriptionFailed"
    }

    override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                                       remoteDataTrackPublication: RemoteDataTrackPublication,
                                       remoteDataTrack: RemoteDataTrack) {
        Log.i(TAG, "onDataTrackSubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                "name=${remoteDataTrack.name}]")
        videoStatusTextView.text = "onDataTrackSubscribed"
    }

    override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                         remoteDataTrackPublication: RemoteDataTrackPublication,
                                         remoteDataTrack: RemoteDataTrack) {
        Log.i(TAG, "onDataTrackUnsubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
                "name=${remoteDataTrack.name}]")
        videoStatusTextView.text = "onDataTrackUnsubscribed"
    }

    override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                               remoteDataTrackPublication: RemoteDataTrackPublication,
                                               twilioException: TwilioException) {
        Log.i(TAG, "onDataTrackSubscriptionFailed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
                "name=${remoteDataTrackPublication.trackName}]" +
                "[TwilioException: code=${twilioException.code}, " +
                "message=${twilioException.message}]")
        videoStatusTextView.text = "onDataTrackSubscriptionFailed"
    }

    override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                                        remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                        remoteVideoTrack: RemoteVideoTrack) {
        Log.i(TAG, "onVideoTrackSubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                "name=${remoteVideoTrack.name}]")
        videoStatusTextView.text = "onVideoTrackSubscribed"
        //addRemoteParticipantVideo(remoteVideoTrack)
    }

    override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                          remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                          remoteVideoTrack: RemoteVideoTrack) {
        Log.i(TAG, "onVideoTrackUnsubscribed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
                "name=${remoteVideoTrack.name}]")
        videoStatusTextView.text = "onVideoTrackUnsubscribed"
        //removeParticipantVideo(remoteVideoTrack)
    }

    override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                                twilioException: TwilioException) {
        Log.i(TAG, "onVideoTrackSubscriptionFailed: " +
                "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
                "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
                "name=${remoteVideoTrackPublication.trackName}]" +
                "[TwilioException: code=${twilioException.code}, " +
                "message=${twilioException.message}]")
        videoStatusTextView.text = "onVideoTrackSubscriptionFailed"
        Snackbar.make(connectActionFab,
                "Failed to subscribe to ${remoteParticipant.identity}",
                Snackbar.LENGTH_LONG)
                .show()
    }


}