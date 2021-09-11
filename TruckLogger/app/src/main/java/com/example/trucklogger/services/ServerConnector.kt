package com.example.trucklogger.services

import android.app.Service
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import com.example.trucklogger.other.Constants.SERVER_IP
import com.example.trucklogger.other.Constants.SERVER_PORT
import com.example.trucklogger.other.Constants.SOCKET_TIMEOUT
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

    public fun sendMessage(msg : String) : String {
        Timber.d("sending....")
        try {
            socket = socketFactory.createSocket(SERVER_IP, SERVER_PORT, ) as SSLSocket
            socket.soTimeout = SOCKET_TIMEOUT
            outputBuffer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.outputStream)))
            inputBuffer = BufferedReader(InputStreamReader(socket.inputStream))

            outputBuffer.print(msg + "\u0000")
            outputBuffer.flush()

            reply = inputBuffer.readLine()

            inputBuffer.close()
            outputBuffer.close()
            socket.close()
        } catch (e: ConnectException) {
            reply = "TIMEOUT"
        }
        return reply
    }
}