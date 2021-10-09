package com.example.trucklogger.repositories

data class Settings(
    var TRUCKER_ID: Int,
    var TRUCKER_UUID: String,
    var TRUCKER_NAME: String,
    var TRUCKER_VEHICLE_NUMBER: String,
    var TRUCKER_MANAGER: String,
    var TRUCKER_VERIFIED: Boolean,
    var UPLOAD_FREQUENCY: Int,
)
