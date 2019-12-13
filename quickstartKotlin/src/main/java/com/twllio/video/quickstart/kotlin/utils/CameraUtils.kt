package com.twllio.video.quickstart.kotlin.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.twilio.video.CameraCapturer
import com.twilio.video.quickstart.kotlin.R

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

        fun requestPermissionForCameraAndMicrophone(activity: Activity, permissionRequestCode: Int) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(activity,
                        R.string.permissions_needed,
                        Toast.LENGTH_LONG).show()
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                        permissionRequestCode)
            }
        }
    }
}