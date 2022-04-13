/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static android.app.PendingIntent.FLAG_MUTABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Installer utils.
 */
class InstallerUtils {

    /**
     * Value when {@link Settings.Secure#INSTALL_NON_MARKET_APPS} setting is enabled.
     */
    @VisibleForTesting
    static final String INSTALL_NON_MARKET_APPS_ENABLED = "1";

    /**
     * Name of package installer stream.
     */
    private static final String sOutputStreamName = "AppCenterPackageInstallerStream";

    /**
     * Buffer capacity of package installer.
     */
    private static final int BUFFER_CAPACITY = 64 * 1024;

    /**
     * Installer package names that are not app stores.
     */
    private static final Set<String> LOCAL_STORES = new HashSet<>();

    /**
     * Used to cache the result of {@link #isInstalledFromAppStore(String, Context)}, null until first call.
     */
    private static Boolean sInstalledFromAppStore;

    /* Populate local stores. */
    static {
        LOCAL_STORES.add("adb");
        LOCAL_STORES.add("com.android.packageinstaller");
        LOCAL_STORES.add("com.google.android.packageinstaller");
        LOCAL_STORES.add("com.android.managedprovisioning");
        LOCAL_STORES.add("com.miui.packageinstaller");
        LOCAL_STORES.add("com.samsung.android.packageinstaller");
        LOCAL_STORES.add("pc");
        LOCAL_STORES.add("com.google.android.apps.nbu.files");
        LOCAL_STORES.add("org.mozilla.firefox");
        LOCAL_STORES.add("com.android.chrome");
    }

    @VisibleForTesting
    InstallerUtils() {

        /* Hide constructor in utils pattern. */
    }

    /**
     * Check if this installation was made via an application store.
     *
     * @param logTag  log tag for debug.
     * @param context any context.
     * @return true if the application was installed from an app store, false if it was installed via adb or via unknown source.
     */
    static boolean isInstalledFromAppStore(@NonNull String logTag, @NonNull Context context) {
        if (sInstalledFromAppStore == null) {
            String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            AppCenterLog.debug(logTag, "InstallerPackageName=" + installer);
            sInstalledFromAppStore = installer != null && !LOCAL_STORES.contains(installer) && !installer.equals(context.getPackageName());
        }
        return sInstalledFromAppStore;
    }

    /**
     * Add new stores to local stores list.
     *
     * @param stores list of stores.
     */
    public static void addLocalStores(Set<String> stores) {
        LOCAL_STORES.addAll(stores);
    }

    /**
     * Check whether user enabled installation via unknown sources.
     *
     * @param context any context.
     * @return true if installation via unknown sources is enabled, false otherwise.
     */
    @SuppressWarnings("deprecation")
    public static boolean isUnknownSourcesEnabled(@NonNull Context context) {

        /*
         * On Android 8 with applications targeting lower versions,
         * it's impossible to check unknown sources enabled: using old APIs will always return true
         * and using the new one will always return false,
         * so in order to avoid a stuck dialog that can't be bypassed we will assume true.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.O || context.getPackageManager().canRequestPackageInstalls();
        } else {
            return INSTALL_NON_MARKET_APPS_ENABLED.equals(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS));
        }
    }

    /**
     * Check whether user enabled start activity from the background.
     *
     * @param context any context.
     * @return true if start activity from the background is enabled, false otherwise.
     */
    public static boolean isSystemAlertWindowsEnabled(@NonNull Context context) {

        /*
        * From Android 10 (29 API level) or higher we have to
        * use this permission for restarting activity after update.
        * See more about restrictions on starting activities from the background:
        * - https://developer.android.com/guide/components/activities/background-starts
        * - https://developer.android.com/about/versions/10/behavior-changes-all#sysalert-go
        */
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                Settings.canDrawOverlays(context);
    }

    /**
     * Install a new release.
     */
    @WorkerThread
    public static void installPackage(@NonNull Uri localUri, Context context, PackageInstaller.SessionCallback sessionCallback) {
        PackageInstaller.Session session = null;
        try {

            /* Prepare package installer. */
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            if (sessionCallback != null) {
                packageInstaller.registerSessionCallback(sessionCallback);
            }
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);

            /* Prepare session. */
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            // FIXME: android.os.strictmode.LeakedClosableViolation: A resource was acquired at attached stack trace but never released.
            ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(localUri, "r");
            addFileToInstallSession(fileDescriptor, session);
            fileDescriptor.close(); // TODO: finally

            /* Start to install a new release. */
            session.commit(createIntentSender(context, sessionId));
            session.close();
        } catch (IOException e) {
            if (session != null) {
                session.abandon();
            }
            AppCenterLog.error(LOG_TAG, "Couldn't install a new release.", e);
        }
    }

    /**
     * Return IntentSender with the receiver that listens to the package installer session status.
     *
     * @param context any context.
     * @param sessionId install sessionId.
     * @return IntentSender with receiver.
     */
    public static IntentSender createIntentSender(Context context, int sessionId) {
        int broadcastFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            broadcastFlags |= FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(AppCenterPackageInstallerReceiver.START_ACTION),
                broadcastFlags);
        return pendingIntent.getIntentSender();
    }

    @WorkerThread
    private static void addFileToInstallSession(ParcelFileDescriptor fileDescriptor, PackageInstaller.Session session)
            throws IOException {
        try (OutputStream out = session.openWrite(sOutputStreamName, 0, fileDescriptor.getStatSize());
             InputStream in = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            byte[] buffer = new byte[BUFFER_CAPACITY];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            session.fsync(out);
        }
    }
}
