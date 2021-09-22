package com.example.trucklogger.other

import com.example.trucklogger.db.TruckLog
import com.google.gson.annotations.SerializedName

enum class ServerRequestCode (val value: Int) {
    REQUEST_UPDATE_ID(1),
    REQUEST_VERIFY_ID(2),
    REQUEST_UPDATE_LOGS(3),
}

enum class ServerResponseCode(val value: Int) {
    RESPONSE_TIMEOUT(-1),
    RESPONSE_FAIL(0),
    RESPONSE_OK(1),
    RESPONSE_INVALID_CREDENTIALS(2),
    RESPONSE_DB_CONN_FAIL(3),
    RESPONSE_PARSE_FAIL(4);
    companion object {
        fun fromInt(value: Int) = ServerResponseCode.values().first { it.value == value }
    }
}

data class ServerRequest(
    var id: Int,
    var uuid: String,
    var req: Int,
    var data: List<TruckLog>?
    )

data class ServerResponse(
    @SerializedName("res") var res: Int,
    @SerializedName("trucker") var trucker: String?
)
