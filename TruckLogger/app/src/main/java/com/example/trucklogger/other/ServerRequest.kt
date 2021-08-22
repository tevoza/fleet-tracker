package com.example.trucklogger.other

import com.example.trucklogger.db.TruckLog

data class ServerRequest (
    var id: Int,
    var request: String,
    var data: List<TruckLog>
        )