package com.twllio.video.quickstart.kotlin.utils

import com.twilio.video.*

interface RemoteParticipantEventHandler {
    fun onAudioTrackPublished(remoteParticipant: RemoteParticipant,
                              remoteAudioTrackPublication: RemoteAudioTrackPublication)
    fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant,
                                remoteAudioTrackPublication: RemoteAudioTrackPublication)
    fun onDataTrackPublished(remoteParticipant: RemoteParticipant,
                             remoteDataTrackPublication: RemoteDataTrackPublication)
    fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant,
                                remoteDataTrackPublication: RemoteDataTrackPublication)
    fun onVideoTrackPublished(remoteParticipant: RemoteParticipant,
                              remoteVideoTrackPublication: RemoteVideoTrackPublication)
    fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant,
                                remoteVideoTrackPublication: RemoteVideoTrackPublication)
    fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant,
                               remoteAudioTrackPublication: RemoteAudioTrackPublication,
                               remoteAudioTrack: RemoteAudioTrack)
    fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                 remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                 remoteAudioTrack: RemoteAudioTrack)
    fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                       remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                       twilioException: TwilioException)
    fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant,
                              remoteDataTrackPublication: RemoteDataTrackPublication,
                              remoteDataTrack: RemoteDataTrack)
    fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                remoteDataTrackPublication: RemoteDataTrackPublication,
                                remoteDataTrack: RemoteDataTrack)
    fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                      remoteDataTrackPublication: RemoteDataTrackPublication,
                                      twilioException: TwilioException)
    fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant,
                               remoteVideoTrackPublication: RemoteVideoTrackPublication,
                               remoteVideoTrack: RemoteVideoTrack)
    fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant,
                                 remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                 remoteVideoTrack: RemoteVideoTrack)
    fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant,
                                       remoteVideoTrackPublication: RemoteVideoTrackPublication,
                                       twilioException: TwilioException)
}