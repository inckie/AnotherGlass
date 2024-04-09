package com.damn.anotherglass.core

import android.content.Context
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.Constants
import com.damn.anotherglass.shared.rpc.IRPCHost
import com.damn.anotherglass.shared.rpc.RPCHandler
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.anotherglass.shared.utility.Closeables
import com.damn.anotherglass.shared.utility.Sleep
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.Volatile

class WiFiHost(listener: RPCMessageListener) : IRPCHost {

    private val logger = ALog(Logger.get(TAG))

    private val mHandler: RPCHandler = RPCHandler(listener)

    private val mQueue: BlockingQueue<RPCMessage> = LinkedBlockingDeque()

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
        when(val workerThread = mWorkerThread) {
            null -> logger.e(TAG, "Not started")
            else -> if (!workerThread.isConnected())
                logger.e(TAG, "Not connected")
            else
                mQueue.add(message)
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

        // has to expose these for cleaner shutdown
        @Volatile
        private var mServerSocket: ServerSocket? = null
        @Volatile
        private var mSocket: Socket? = null

        override fun run() {
            while (!isInterrupted) {
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
            val iss = socket.getInputStream()
            val oos = ObjectOutputStream(socket.getOutputStream())
            val ois = ObjectInputStream(iss)
            while (!isInterrupted) {
                while (mQueue.peek() != null) {
                    val message = mQueue.take()
                    oos.writeObject(message)
                    oos.flush()
                }
                while (iss.available() > 0) {
                    val message = ois.readObject() as RPCMessage
                    if (message.service == null) {
                        return // client disconnected
                    }
                    mHandler.onDataReceived(message)
                }
                Sleep.sleep(100)
            }
        }

        fun shutdown() {
            interrupt()
            mSocket?.close()
            mServerSocket?.close()
        }

        fun isConnected(): Boolean = mSocket != null
    }

    companion object {
        private const val TAG = "WiFiHost"
    }
}