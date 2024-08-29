package com.example.camera

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CallActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CALL_PHONE_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the contact information from the Intent
        val contactName = intent.getStringExtra("CONTACT_NAME")
        val contactPhoneNumber = intent.getStringExtra("CONTACT_PHONE_NUMBER")

        // Make the call
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            makeCall(contactPhoneNumber)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION
            )
        }
    }

    private fun makeCall(phoneNumber: String?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            phoneNumber?.let {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$it"))
                Log.d("CallActivity", "Initiating call to $it")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e("CallActivity", "Error initiating call: ${e.message}")
                    Toast.makeText(this, "Error initiating call", Toast.LENGTH_SHORT).show()
                }
                finish() // Finish the CallActivity immediately after initiating the call
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION
            )
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, attempt to make the call again
                val contactPhoneNumber = intent.getStringExtra("CONTACT_PHONE_NUMBER")
                makeCall(contactPhoneNumber)
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied to make calls", Toast.LENGTH_SHORT).show()
                finish() // Finish the CallActivity if permission is denied
            }
        }
    }
}
