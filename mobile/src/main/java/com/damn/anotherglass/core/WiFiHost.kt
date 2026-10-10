package com.damn.anotherglass.core

import android.content.Context
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.Constants
import com.damn.anotherglass.shared.rpc.IRPCHost
import com.damn.anotherglass.shared.rpc.PingAPI
import com.damn.anotherglass.shared.rpc.RPCHandler
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.anotherglass.shared.rpc.SerializerProvider
import com.damn.anotherglass.shared.utility.Closeables
import com.damn.anotherglass.shared.utility.Sleep
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.Volatile

class WiFiHost(listener: RPCMessageListener) : IRPCHost {

    private val logger = ALog(Logger.get(TAG))

    private val mHandler: RPCHandler = RPCHandler(listener)

    @Volatile
    private var mAcceptThread: AcceptThread? = null

    @Volatile
    private var mCurrentSession: ClientSession? = null

    override fun start(context: Context) {
        if (mAcceptThread != null) {
            logger.e(TAG, "Already started")
            return
        }
        mAcceptThread = AcceptThread().also { it.start() }
    }

    override fun send(message: RPCMessage) {
        val session = mCurrentSession
        if (session == null || !session.isConnected()) {
            logger.e(TAG, "Not connected")
        } else {
            session.send(message)
        }
    }

    override fun stop() {
        mAcceptThread?.let {
            mAcceptThread = null
            it.shutdown()
            it.join()
        }
        val session: ClientSession?
        synchronized(this) {
            session = mCurrentSession
            mCurrentSession = null
        }
        session?.close()
    }

    private fun handleNewConnection(newSocket: Socket) {
        val oldSession: ClientSession?
        val newSession = ClientSession(newSocket)
        synchronized(this) {
            oldSession = mCurrentSession
            mCurrentSession = newSession
        }
        if (oldSession != null && oldSession.isConnected()) {
            logger.i(TAG, "New connection from ${newSocket.inetAddress}, dropping old connection from ${oldSession.remoteAddress}")
            oldSession.close()
        }
        newSession.start()
    }

    inner class AcceptThread : Thread() {
        @Volatile
        private var mServerSocket: ServerSocket? = null
        @Volatile
        private var mActive = true

        override fun run() {
            try {
                mHandler.onWaiting()
                val serverSocket = ServerSocket(Constants.defaultPort)
                mServerSocket = serverSocket
                logger.i(TAG, "Listening for connections on port ${Constants.defaultPort}")

                while (mActive) {
                    try {
                        val clientSocket = serverSocket.accept()
                        if (!mActive) {
                            Closeables.close(clientSocket)
                            break
                        }
                        try {
                            clientSocket.tcpNoDelay = true
                            clientSocket.setSoLinger(true, 2)
                        } catch (_: Exception) {
                        }
                        handleNewConnection(clientSocket)
                    } catch (e: SocketException) {
                        if (!mActive) break
                        logger.w(TAG, "ServerSocket accept exception: ${e.message}")
                    } catch (e: Exception) {
                        if (!mActive) break
                        logger.e(TAG, "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Failed to start ServerSocket on port ${Constants.defaultPort}: ${e.message}")
            } finally {
                Closeables.close(mServerSocket)
                mServerSocket = null
                mHandler.onShutdown()
            }
        }

        fun shutdown() {
            mActive = false
            Closeables.close(mServerSocket)
        }
    }

    inner class ClientSession(private val socket: Socket) : Thread() {
        private val mQueue: BlockingQueue<RPCMessage> = LinkedBlockingDeque()
        @Volatile
        private var mActive = true

        val remoteAddress: String = socket.inetAddress?.toString() ?: "unknown"

        fun isConnected(): Boolean = mActive && socket.isConnected && !socket.isClosed

        fun send(message: RPCMessage) {
            if (!mActive) {
                logger.w(TAG, "Cannot send, session is closing/closed")
                return
            }
            mQueue.add(message)
        }

        fun close() {
            val wasActive = mActive
            mActive = false
            if (wasActive && socket.isConnected && !socket.isClosed) {
                mQueue.add(RPCMessage(null, null))
                if (isAlive && Thread.currentThread() !== this) {
                    try {
                        join(1000)
                    } catch (_: InterruptedException) {
                    }
                }
            }
            Closeables.close(socket)
            interrupt()
        }

        override fun run() {
            logger.i(TAG, "Client session started: $remoteAddress")
            mHandler.onConnectionStarted(remoteAddress)
            try {
                runLoop()
            } catch (e: Exception) {
                logger.w(TAG, "Session error ($remoteAddress): ${e.message}")
            } finally {
                Closeables.close(socket)
                logger.i(TAG, "Client session ended: $remoteAddress")
                synchronized(this@WiFiHost) {
                    if (mCurrentSession === this) {
                        mCurrentSession = null
                        mHandler.onConnectionLost(null)
                        if (mAcceptThread != null) {
                            mHandler.onWaiting()
                        }
                    }
                }
            }
        }

        private fun runLoop() {
            socket.getInputStream().use { inputStream ->
                socket.getOutputStream().use { outputStream ->
                    val serializer = SerializerProvider.getSerializer(inputStream, outputStream)
                    var lastSentTime = System.currentTimeMillis()
                    var lastReceivedTime = System.currentTimeMillis()

                    while (mActive || mQueue.peek() != null) {
                        val now = System.currentTimeMillis()

                        // Send queued messages
                        while (mQueue.peek() != null) {
                            val message = mQueue.take()
                            serializer.writeMessage(message)
                            lastSentTime = System.currentTimeMillis()
                            if (message.service == null) {
                                try {
                                    socket.shutdownOutput()
                                } catch (_: Exception) {
                                }
                                Sleep.sleep(200)
                                return // disconnect requested
                            }
                        }

                        // Send periodic ping if idle
                        if (mActive && now - lastSentTime >= PING_INTERVAL_MS) {
                            serializer.writeMessage(RPCMessage(PingAPI.ID, null))
                            lastSentTime = System.currentTimeMillis()
                        }

                        // Read incoming messages
                        while (mActive && (serializer.isReady || inputStream.available() > 0)) {
                            val message = serializer.readMessage()
                            lastReceivedTime = System.currentTimeMillis()
                            if (message?.service == null) {
                                return // client requested disconnect
                            }
                            when (message.service) {
                                PingAPI.ID -> {
                                    // Received Ping from client, reply with Pong
                                    serializer.writeMessage(RPCMessage(PingAPI.PONG_ID, null))
                                    lastSentTime = System.currentTimeMillis()
                                }
                                PingAPI.PONG_ID -> {
                                    // Received Pong reply from client
                                }
                                else -> {
                                    mHandler.onDataReceived(message)
                                }
                            }
                        }

                        // Check ping timeout
                        if (mActive && System.currentTimeMillis() - lastReceivedTime > PING_TIMEOUT_MS) {
                            throw SocketTimeoutException(
                                "Client ping timeout: no data from client for ${PING_TIMEOUT_MS}ms"
                            )
                        }

                        Sleep.sleep(100)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WiFiHost"
        private const val PING_INTERVAL_MS = 5000L
        private const val PING_TIMEOUT_MS = 20000L
    }
}