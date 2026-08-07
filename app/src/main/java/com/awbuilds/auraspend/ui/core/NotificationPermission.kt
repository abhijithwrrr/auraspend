package com.awbuilds.auraspend.ui.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a launcher that requests [Manifest.permission.POST_NOTIFICATIONS] on Android 13+ (a
 * no-op below that, where notifications are granted at install time). All notification publishing
 * already guards on the permission, so a denial here is harmless.
 */
@Composable
fun rememberNotificationPermissionLauncher(): ActivityResultLauncher<String> =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

/** True when a runtime POST_NOTIFICATIONS request is needed on this device. */
fun isNotificationPermissionNeeded(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED