package com.example.syncplan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SmsService(private val context: Context) {

    companion object {
        private const val TAG = "SmsService"
        private const val APP_DOWNLOAD_URL = "https://github.com/likpik/SyncPlan"
        private const val MAX_SMS_LENGTH = 160
    }

    private val smsManager = SmsManager.getDefault()

    suspend fun sendGroupInvitation(
        phoneNumber: String,
        groupName: String,
        inviterName: String,
        invitationCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(
                    SecurityException("Brak uprawnień do wysyłania SMS")
                )
            }

            if (!isValidPhoneNumber(phoneNumber)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Nieprawidłowy numer telefonu")
                )
            }

            val message = createInvitationMessage(
                groupName = groupName,
                inviterName = inviterName,
                invitationCode = invitationCode
            )

            // Jeśli wiadomość jest za długa, podziel na części
            if (message.length > MAX_SMS_LENGTH) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }

            Log.d(TAG, "SMS invitation sent successfully to $phoneNumber")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS invitation", e)
            Result.failure(e)
        }
    }

    private fun createInvitationMessage(
        groupName: String,
        inviterName: String,
        invitationCode: String
    ): String {
        return """
            🎉 Zaproszenie do grupy!
            
            $inviterName zaprosił Cię do grupy "$groupName" w aplikacji SyncPlan.
            
            Kod zaproszenia: $invitationCode
            
            Pobierz aplikację: $APP_DOWNLOAD_URL
            
            Po instalacji wpisz kod zaproszenia aby dołączyć do grupy.
        """.trimIndent()
    }

    fun generateInvitationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val code = StringBuilder()

        repeat(6) {
            code.append(chars[random.nextInt(chars.length)])
        }

        return code.toString()
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace("\\s+".toRegex(), "")
        return cleanNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$"))
    }

    fun formatPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }

    fun canSendSms(): Boolean {
        return try {
            smsManager != null && hasPermission()
        } catch (e: Exception) {
            false
        }
    }
}