package com.twllio.video.quickstart.kotlin.utils

import com.twilio.video.RemoteParticipant
import com.twilio.video.Room
import com.twilio.video.TwilioException

interface RoomEventHandler {
    fun onConnected(room: Room)
    fun onReconnected(room: Room)
    fun onReconnecting(room: Room, twilioException: TwilioException)
    fun onConnectFailure(room: Room, e: TwilioException)
    fun onDisconnected(room: Room, e: TwilioException?)
    fun onParticipantConnected(room: Room, participant: RemoteParticipant)
    fun onParticipantDisconnected(room: Room, participant: RemoteParticipant)
    fun onRecordingStarted(room: Room)
    fun onRecordingStopped(room: Room)
}