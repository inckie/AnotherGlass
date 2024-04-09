package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import android.util.Log
import com.damn.anotherglass.glass.ee.host.core.ConnectionUtils.getHostIPAddress
import com.damn.anotherglass.shared.Constants
import com.damn.anotherglass.shared.rpc.IRPCClient
import com.damn.anotherglass.shared.rpc.RPCHandler
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

class WiFiClient : IRPCClient {

    @Volatile
    private var mWorkerThread: WorkerThread? = null

    override fun start(context: Context, listener: RPCMessageListener) {
        // todo: pass startup errors up
        if (mWorkerThread != null) {
            Log.e(TAG, "Already started")
            return
        }
        // todo: this part should not be there
        val ip = getHostIPAddress(context)
        if (null == ip) {
            Log.e(TAG, "No host IP address")
            return
        }
        mWorkerThread = WorkerThread(listener, ip)
        mWorkerThread!!.start()
    }

    override fun send(message: RPCMessage) {
        mWorkerThread?.send(message)
    }

    override fun stop() {
        mWorkerThread?.let {
            it.shutdown()
            it.join()
        }
    }

    inner class WorkerThread(
        listener: RPCMessageListener,
        private val ip: String
    ) : Thread() {

        private val mHandler: RPCHandler = RPCHandler(listener)

        private val mQueue: BlockingQueue<RPCMessage> = LinkedBlockingDeque()

        override fun run() {
            mHandler.onWaiting()
            try {
                Socket(ip, Constants.defaultPort).use { socket ->
                    mHandler.onConnectionStarted(socket.inetAddress.toString())
                    runLoop(socket)
                }
            } catch (e: SocketException) {
                // since we do not close the socket on client, its actually some error
                mHandler.onConnectionLost(e.message)
            } catch (e: InterruptedException) {
                mHandler.onConnectionLost(null) // not an error, just a shutdown
            } catch (e: Exception) {
                mHandler.onConnectionLost(e.message)
                Log.e(TAG, "WorkerThread error", e)
            } finally {
                // note: there will be no onConnectionLost if we are shutting down
                mWorkerThread = null
                mHandler.onShutdown()
            }
        }

        private fun runLoop(socket: Socket) {
            val iss = socket.getInputStream()
            val oos = ObjectOutputStream(socket.getOutputStream())
            val ois = ObjectInputStream(iss)
            while (!isInterrupted) {
                while (null != mQueue.peek()) {
                    val message = mQueue.take()
                    oos.writeObject(message)
                    oos.flush()
                }
                while (iss.available() > 0) {
                    val message = ois.readObject() as RPCMessage
                    if (null == message.service) {
                        return
                    }
                    mHandler.onDataReceived(message);
                }
                sleep(100)
            }
        }

        fun send(message: RPCMessage) {
            mQueue.add(message)
        }

        fun shutdown() {
            // we do not keep the socket reference, so just wait till next iteration
            interrupt()
        }

    }

    companion object {
        private const val TAG = "WiFiClient"
    }
}