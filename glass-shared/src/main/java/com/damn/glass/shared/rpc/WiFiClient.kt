package com.damn.glass.shared.rpc

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.damn.anotherglass.shared.Constants
import com.damn.anotherglass.shared.rpc.IRPCClient
import com.damn.anotherglass.shared.rpc.RPCHandler
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.anotherglass.shared.rpc.SerializerProvider
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

class WiFiClient(private val hostIP: String? = null) : IRPCClient {

    @Volatile
    private var workerThread: WorkerThread? = null

    override fun start(context: Context, listener: RPCMessageListener) {
        if (workerThread != null) {
            Log.e(TAG, "Already started")
            return
        }

        val ip = hostIP ?: ConnectionUtils.getHostIPAddress(context)
        if (ip == null) {
            Log.e(TAG, "No host IP address")
            listener.onShutdown()
            return
        }

        Log.i(TAG, "Connecting to $ip")
        workerThread = WorkerThread(listener, ip)
        workerThread!!.start()
    }

    override fun send(message: RPCMessage) {
        workerThread?.send(message)
    }

    override fun stop() {
        workerThread?.let {
            it.shutdown()
            it.join()
        }
    }

    private inner class WorkerThread(
        listener: RPCMessageListener,
        private val ip: String
    ) : Thread() {

        private val handler: RPCHandler = RPCHandler(listener)

        private val queue: BlockingQueue<RPCMessage> = LinkedBlockingDeque()

        override fun run() {
            handler.onWaiting()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, Constants.defaultPort), 5000)
                    handler.onConnectionStarted(socket.inetAddress.toString())
                    runLoop(socket)
                }
            } catch (e: SocketException) {
                handler.onConnectionLost(e.message)
            } catch (e: InterruptedException) {
                handler.onConnectionLost(null)
            } catch (e: Exception) {
                handler.onConnectionLost(e.message)
                Log.e(TAG, "WorkerThread error", e)
            } finally {
                workerThread = null
                handler.onShutdown()
            }
        }

        private fun runLoop(socket: Socket) {
            socket.getInputStream().use { inputStream ->
                socket.getOutputStream().use { outputStream ->
                    val serializer = SerializerProvider.getSerializer(inputStream, outputStream)
                    while (true) {
                        while (queue.peek() != null) {
                            val message = queue.take()
                            serializer.writeMessage(message)
                            if (message.service == null) {
                                return
                            }
                        }
                        while (inputStream.available() > 0) {
                            val message = serializer.readMessage()
                            if (message.service == null) {
                                return
                            }
                            handler.onDataReceived(message)
                        }
                        sleep(100)
                    }
                }
            }
        }

        fun send(message: RPCMessage) {
            queue.add(message)
        }

        fun shutdown() {
            // Send an empty message as a best-effort disconnect request.
            queue.add(RPCMessage(null, null))
        }
    }

    companion object {
        private const val TAG = "WiFiClient"
    }
}

