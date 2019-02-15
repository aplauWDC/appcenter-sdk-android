package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Constants for Storage module.
 */
final class Constants {

    /**
     * Name of the service.
     */
    static final String SERVICE_NAME = "Storage";

    /**
     * TAG used in logging for Storage.
     */
    static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the storage group.
     */
    static final String STORAGE_GROUP = "group_storage";

    /**
     * User partition
     * An authenticated user can read/write documents in this partition
     */
    public static String USER = "user-{userid}";

    /**
     * Readonly partition
     * Everyone can read documents in this partition
     * Writes is not allowed via the SDK
     */
    public static String READONLY = "readonly";

    /**
     * Check latest public release API URL path. Contains the app secret variable to replace.
     */
    static final String GET_TOKEN_PATH_FORMAT = "/data/tokens";

    /**
     * App Secret Header
     */
    static final String APP_SECRET_HEADER = "App-Secret";

}
