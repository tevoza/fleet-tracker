package com.example.trucklogger.other

object Constants {
    const val TRUCKLOGGING_DATABASE_NAME = "tblTruckLogs"

    const val REQUEST_CODE_LOCATION_PERMISSION = 0

    const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_SHOW_UI = "ACTION_SHOW_UI"

    const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME = "tracking"
    const val NOTIFICATION_ID = 1

    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_UPDATE_INTERVAL = 4000L
    const val MPS_TO_KMH = (60*60)/1000

    const val SERVER_IP = "192.168.8.100"
    const val SERVER_PORT = 1234
    const val SOCKET_TIMEOUT = 2 * 1000
    val KEYSTORE_PASS = "password".toCharArray()
}