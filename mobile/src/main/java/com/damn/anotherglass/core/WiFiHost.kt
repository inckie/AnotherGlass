package com.damn.anotherglass.core

import android.content.Context
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.Constants
import com.damn.anotherglass.shared.rpc.IRPCHost
import com.damn.anotherglass.shared.rpc.RPCHandler
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.anotherglass.shared.rpc.SerializerProvider
import com.damn.anotherglass.shared.utility.Closeables
import com.damn.anotherglass.shared.utility.Sleep
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.Volatile

class WiFiHost(listener: RPCMessageListener) : IRPCHost {

    private val logger = ALog(Logger.get(TAG))

    private val mHandler: RPCHandler = RPCHandler(listener)

    @Volatile
    private var mWorkerThread: WorkerThread? = null

    override fun start(context: Context) {
        if (mWorkerThread != null) {
            logger.e(TAG, "Already started")
            return
        }
        mWorkerThread = WorkerThread()
        mWorkerThread?.start()
    }

    override fun send(message: RPCMessage) {
        when (val workerThread = mWorkerThread) {
            null -> logger.e(TAG, "Not started")
            else -> if (!workerThread.isConnected())
                logger.e(TAG, "Not connected")
            else
                workerThread.send(message)
        }
    }

    override fun stop() {
        mWorkerThread?.let {
            mWorkerThread = null
            it.shutdown()
            it.join()
        }
    }

    inner class WorkerThread : Thread() {
        // not amazing threading code, but will do for now

        private val mQueue: BlockingQueue<RPCMessage> = LinkedBlockingDeque()

        // has to expose these for cleaner shutdown
        @Volatile
        private var mServerSocket: ServerSocket? = null
        @Volatile
        private var mSocket: Socket? = null
        @Volatile
        private var mActive = true

        override fun run() {
            while (mActive) {
                try {
                    mHandler.onWaiting()
                    ServerSocket(Constants.defaultPort).use { serverSocket ->
                        mServerSocket = serverSocket
                        // do not accept more than one connection
                        serverSocket.accept().use { socket ->
                            mServerSocket = null
                            Closeables.close(serverSocket)
                            mSocket = socket
                            mHandler.onConnectionStarted(socket.inetAddress.toString())
                            runLoop(socket)
                        }
                        mHandler.onConnectionLost(null) // client disconnected
                    }
                } catch (e: SocketException) {
                    mHandler.onConnectionLost(null) // not always actual error, can be shutdown
                } catch (e: InterruptedException) {
                    mHandler.onConnectionLost(null) // not an error, just a shutdown
                } catch (e: Exception) {
                    mHandler.onConnectionLost(e.message)
                } finally {
                    mSocket = null
                    mServerSocket = null
                }
            }
            mWorkerThread = null
            mHandler.onShutdown()
        }

        private fun runLoop(socket: Socket) {
            socket.getInputStream().use { inputStream ->
                socket.getOutputStream().use { outputStream ->
                    val serializer = SerializerProvider.getSerializer(inputStream, outputStream)
                    while (mActive) {
                        while (mQueue.peek() != null) {
                            val message = mQueue.take()
                            serializer.writeMessage(message)
                            if (message.service == null) {
                                return // disconnect requested
                            }
                        }
                        while (mActive && inputStream.available() > 0) {
                            val message = serializer.readMessage()
                            if (message.service == null) {
                                return // client disconnected
                            }
                            mHandler.onDataReceived(message)
                        }
                        Sleep.sleep(100)
                    }
                }
            }
        }

        fun shutdown() {
            // send empty message to notify host we are shutting down (we do not guarantee it will be sent though)
            mQueue.add(RPCMessage(null, null))
            mActive = false
            Closeables.close(mSocket)
            Closeables.close(mServerSocket)
        }

        fun isConnected(): Boolean = mSocket != null
        fun send(message: RPCMessage) {
            mQueue.add(message)
        }
    }

    companion object {
        private const val TAG = "WiFiHost"
    }
}