package com.twllio.video.quickstart.kotlin.utils

import android.util.Log
import com.twilio.video.RemoteParticipant
import com.twilio.video.Room
import com.twilio.video.TwilioException
import kotlin.reflect.KProperty

class TwilioRoomListenerResolver {

    companion object {
        fun getRoomListener(roomEventHandler: RoomEventHandler): Room.Listener {
            return object : Room.Listener {
                override fun onConnected(room: Room) {
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
                    roomEventHandler.onDisconnected(room, twilioException)
                }

                override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
                    //todo
                    //addRemoteParticipant(participant)
                    roomEventHandler.onParticipantConnected(room, participant)
                }

                override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
                    //todo
                    //removeRemoteParticipant(participant)
                    roomEventHandler.onParticipantDisconnected(room, participant)
                }

                override fun onRecordingStarted(room: Room) {
                    /*
                     * Indicates when media shared to a Room is being recorded. Note that
                     * recording is only available in our Group Rooms developer preview.
                     */
                    Log.d("RoomListenerResolver", "onRecordingStarted")
                }

                override fun onRecordingStopped(room: Room) {
                    /*
                     * Indicates when media shared to a Room is no longer being recorded. Note that
                     * recording is only available in our Group Rooms developer preview.
                     */
                    Log.d("RoomListenerResolver", "onRecordingStopped")
                }
            }
        }
    }
}