package com.example.trucklogger.services

import android.app.Service
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import com.example.trucklogger.other.Constants.SERVER_IP
import com.example.trucklogger.other.Constants.SERVER_PORT
import com.example.trucklogger.other.Constants.SOCKET_TIMEOUT
import com.example.trucklogger.other.ServerRequest
import com.example.trucklogger.other.ServerResponse
import com.example.trucklogger.other.ServerResponseCode
import com.google.gson.Gson
import timber.log.Timber
import java.io.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLServerSocketFactory

class ServerConnector (sslSocketFactory: SSLSocketFactory){
    private var socketFactory = sslSocketFactory
    private lateinit var socket: SSLSocket
    private lateinit var outputBuffer: PrintWriter
    private lateinit var inputBuffer: BufferedReader
    private lateinit var reply: String
    private lateinit var response: ServerResponse

    fun sendMessage(request : ServerRequest) : ServerResponse {
        val msg = Gson().toJson(request)
        try {
            socket = socketFactory.createSocket(SERVER_IP, SERVER_PORT) as SSLSocket
            socket.soTimeout = SOCKET_TIMEOUT
            outputBuffer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.outputStream)))
            inputBuffer = BufferedReader(InputStreamReader(socket.inputStream))

            outputBuffer.print(msg + "\u0000")
            outputBuffer.flush()

            reply = inputBuffer.readLine()

            inputBuffer.close()
            outputBuffer.close()
            socket.close()
            response = Gson().fromJson(reply, ServerResponse::class.java)
        } catch (e: Exception) {
            response = ServerResponse(ServerResponseCode.RESPONSE_TIMEOUT.value, null)
        }
        return response
    }
}