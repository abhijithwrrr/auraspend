package com.awbuilds.auraspend.data.remote

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class DriveSyncManager(private val context: Context) {

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestEmail()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    companion object {
        private const val BACKUP_FILE_NAME = "auraspend_backup.json"
        private const val TAG = "DriveSyncManager"
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun isSignedIn(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    suspend fun handleSignInResult(data: Intent?): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            account != null
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in result handling failed", e)
            false
        }
    }

    suspend fun backupLocalData(dataJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService() ?: return@withContext false

            val existingFileId = findBackupFile(drive)
            val content = com.google.api.client.http.ByteArrayContent(
                "application/json", dataJson.toByteArray(Charsets.UTF_8)
            )

            if (existingFileId != null) {
                drive.files().update(existingFileId, File(), content).execute()
                Log.d(TAG, "Backup updated for file: $existingFileId")
            } else {
                val file = File().apply {
                    name = BACKUP_FILE_NAME
                    mimeType = "application/json"
                }
                drive.files().create(file, content).execute()
                Log.d(TAG, "Backup created")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            false
        }
    }

    suspend fun restoreLocalData(): String? = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService() ?: return@withContext null
            val fileId = findBackupFile(drive) ?: return@withContext null

            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)

            val json = outputStream.toString(Charsets.UTF_8.name())
            Log.d(TAG, "Restore successful, ${json.length} chars")
            json
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            null
        }
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = Account(account.email ?: return null, "com.google")
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("AuraSpend").build()
    }

    private fun findBackupFile(drive: Drive): String? {
        val result = drive.files().list()
            .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
        return result.files.firstOrNull()?.id
    }
}
