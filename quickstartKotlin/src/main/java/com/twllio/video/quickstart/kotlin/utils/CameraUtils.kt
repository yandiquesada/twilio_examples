package com.twllio.video.quickstart.kotlin.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.twilio.video.CameraCapturer

class CameraUtils {
    companion object {
        fun getAvailableCameraSource(): CameraCapturer.CameraSource {
            return if (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA))
                CameraCapturer.CameraSource.FRONT_CAMERA
            else
                CameraCapturer.CameraSource.BACK_CAMERA
        }

        fun checkPermissionForCameraAndMicrophone(context: Context): Boolean {
            val resultCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            val resultMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)

            return resultCamera == PackageManager.PERMISSION_GRANTED &&
                    resultMic == PackageManager.PERMISSION_GRANTED
        }
    }
}