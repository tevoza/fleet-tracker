package com.example.trucklogger.services

import android.app.Service
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import com.example.trucklogger.other.Constants.SERVER_IP
import com.example.trucklogger.other.Constants.SERVER_PORT
import timber.log.Timber
import java.io.*
import javax.net.ssl.SSLServerSocketFactory

class ServerConnector (sslSocketFactory: SSLSocketFactory){
    //var socket: SSLSocket = socketFactory.createSocket(SERVER_IP , SERVER_PORT) as SSLSocket
    private var socketFactory = sslSocketFactory
    private lateinit var socket: SSLSocket
    private lateinit var outputBuffer: PrintWriter
    private lateinit var inputBuffer: BufferedReader

    public fun sendMessage(msg : String) : String {
        socket = socketFactory.createSocket(SERVER_IP, SERVER_PORT) as SSLSocket
        outputBuffer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.outputStream)))
        inputBuffer = BufferedReader(InputStreamReader(socket.inputStream))

        Timber.d("sending....")
        outputBuffer.println(msg)
        outputBuffer.flush()

        val reply = inputBuffer.readLine()
        Timber.d("reply: $reply")

        inputBuffer.close()
        outputBuffer.close()
        socket.close()

        return reply
    }
}