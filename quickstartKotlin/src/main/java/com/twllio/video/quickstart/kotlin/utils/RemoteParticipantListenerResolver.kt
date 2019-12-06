@file:Suppress("PackageDirectoryMismatch")

package com.twllio.video.quickstart.kotlin.utils

import android.support.design.widget.Snackbar
import android.util.Log
import com.twilio.video.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.content_video.*

class RemoteParticipantListenerResolver {
    companion object {
        fun getRemoteParticipantListener(remoteParticipantEventHandler: RemoteParticipantEventHandler): RemoteParticipant.Listener {
            return object : RemoteParticipant.Listener {
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
    }
}