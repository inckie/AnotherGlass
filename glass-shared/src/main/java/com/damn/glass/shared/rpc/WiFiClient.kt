package com.damn.glass.shared.rpc

import android.content.Context
import android.util.Log
import com.damn.anotherglass.shared.Constants
import com.damn.anotherglass.shared.rpc.IRPCClient
import com.damn.anotherglass.shared.rpc.PingAPI
import com.damn.anotherglass.shared.rpc.RPCHandler
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.anotherglass.shared.rpc.SerializerProvider
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
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
        workerThread = WorkerThread(listener, ip).also { it.start() }
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

        @Volatile
        private var active = true

        @Volatile
        private var currentSocket: Socket? = null

        override fun run() {
            var attempt = 0
            while (active) {
                handler.onWaiting()
                val socket = Socket()
                currentSocket = socket
                try {
                    try {
                        socket.tcpNoDelay = true
                        socket.setSoLinger(true, 2)
                    } catch (_: Exception) {
                    }
                    attempt++
                    Log.i(TAG, "Connecting to $ip (attempt $attempt/$MAX_RECONNECT_ATTEMPTS)...")
                    socket.connect(InetSocketAddress(ip, Constants.defaultPort), CONNECT_TIMEOUT_MS)
                    if (!active) {
                        break
                    }
                    try {
                        socket.tcpNoDelay = true
                    } catch (_: Exception) {
                    }
                    Log.i(TAG, "Connected to $ip on attempt $attempt")
                    attempt = 0 // Reset attempt counter on success
                    handler.onConnectionStarted(socket.inetAddress.toString())

                    runLoop(socket)

                    if (!active) {
                        break
                    }
                    Log.w(TAG, "Connection lost to $ip")
                    handler.onConnectionLost(null)
                } catch (e: Exception) {
                    if (!active) {
                        break
                    }
                    Log.w(TAG, "Connection failed (attempt $attempt/$MAX_RECONNECT_ATTEMPTS): ${e.message}")
                    handler.onConnectionLost(e.message)
                } finally {
                    currentSocket = null
                    try {
                        socket.close()
                    } catch (_: Exception) {
                    }
                }

                if (!active) {
                    break
                }

                if (attempt >= MAX_RECONNECT_ATTEMPTS) {
                    Log.e(TAG, "Exhausted all $MAX_RECONNECT_ATTEMPTS reconnect attempts to $ip")
                    break
                }

                try {
                    sleep(RECONNECT_DELAY_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
            workerThread = null
            handler.onShutdown()
        }

        private fun runLoop(socket: Socket) {
            socket.getInputStream().use { inputStream ->
                socket.getOutputStream().use { outputStream ->
                    val serializer = SerializerProvider.getSerializer(inputStream, outputStream)
                    var lastSentTime = System.currentTimeMillis()
                    var lastReceivedTime = System.currentTimeMillis()

                    while (active || queue.peek() != null) {
                        val now = System.currentTimeMillis()

                        // Send queued messages
                        while (queue.peek() != null) {
                            val message = queue.take()
                            serializer.writeMessage(message)
                            lastSentTime = System.currentTimeMillis()
                            if (message.service == null) {
                                try {
                                    socket.shutdownOutput()
                                } catch (_: Exception) {
                                }
                                try {
                                    sleep(200)
                                } catch (_: InterruptedException) {
                                }
                                return // disconnect requested
                            }
                        }

                        // Send periodic ping if idle
                        if (active && now - lastSentTime >= PING_INTERVAL_MS) {
                            serializer.writeMessage(RPCMessage(PingAPI.ID, null))
                            lastSentTime = System.currentTimeMillis()
                        }

                        // Read incoming messages
                        while (active && (serializer.isReady || inputStream.available() > 0)) {
                            val message = serializer.readMessage()
                            lastReceivedTime = System.currentTimeMillis()
                            if (message?.service == null) {
                                return // peer disconnected
                            }
                            when (message.service) {
                                PingAPI.ID -> {
                                    // Peer sent ping, reply with pong immediately
                                    serializer.writeMessage(RPCMessage(PingAPI.PONG_ID, null))
                                    lastSentTime = System.currentTimeMillis()
                                }
                                PingAPI.PONG_ID -> {
                                    // Received pong reply
                                }
                                else -> {
                                    handler.onDataReceived(message)
                                }
                            }
                        }

                        // Check ping timeout
                        if (active && System.currentTimeMillis() - lastReceivedTime > PING_TIMEOUT_MS) {
                            throw SocketTimeoutException(
                                "Ping timeout: no response from server for ${PING_TIMEOUT_MS}ms"
                            )
                        }

                        try {
                            sleep(100)
                        } catch (_: InterruptedException) {
                            if (!active && queue.peek() == null) {
                                return
                            }
                        }
                    }
                }
            }
        }

        fun send(message: RPCMessage) {
            if (!active) {
                Log.w(TAG, "Cannot send, client is shutting down")
                return
            }
            queue.add(message)
        }

        fun shutdown() {
            val wasActive = active
            active = false
            val socket = currentSocket
            if (wasActive && socket != null && socket.isConnected && !socket.isClosed) {
                queue.add(RPCMessage(null, null))
                if (isAlive && Thread.currentThread() !== this) {
                    try {
                        join(1000)
                    } catch (_: InterruptedException) {
                    }
                }
            }
            try {
                currentSocket?.close()
            } catch (_: Exception) {
            }
            interrupt()
        }
    }

    companion object {
        private const val TAG = "WiFiClient"
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 2000L
        private const val PING_INTERVAL_MS = 5000L
        private const val PING_TIMEOUT_MS = 15000L
    }
}
