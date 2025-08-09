package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.core.ConnectionUtils.getHostIPAddress
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
    private var mWorkerThread: WorkerThread? = null

    override fun start(context: Context, listener: RPCMessageListener) {
        // todo: pass startup errors up
        if (mWorkerThread != null) {
            Log.e(TAG, "Already started")
            return
        }
        val ip = hostIP ?: getHostIPAddress(context)
        if (null == ip) {
            Log.e(TAG, "No host IP address")
            Toast.makeText(context, R.string.msg_no_server_ip, Toast.LENGTH_SHORT).show()
            listener.onShutdown()
            return
        }
        Toast.makeText(context, context.getString(R.string.msg_connecting_to_ip_s, ip), Toast.LENGTH_SHORT).show()
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
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, Constants.defaultPort), 5000)
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
            socket.getInputStream().use { inputStream ->
                socket.getOutputStream().use { outputStream ->
                    val serializer = SerializerProvider.getSerializer(inputStream, outputStream)
                    while (true) {
                        while (null != mQueue.peek()) {
                            val message = mQueue.take()
                            serializer.writeMessage(message)
                            if (message.service == null) {
                                return // disconnect requested
                            }
                        }
                        while (inputStream.available() > 0) {
                            val message = serializer.readMessage()
                            if (null == message.service) {
                                return
                            }
                            mHandler.onDataReceived(message)
                        }
                        sleep(100)
                    }
                }
            }
        }

        fun send(message: RPCMessage) {
            mQueue.add(message)
        }

        fun shutdown() {
            // send empty message to notify host we are shutting down (we do not guarantee it will be sent though)
            // we do not keep the socket reference, so just wait till next iteration
            mQueue.add(RPCMessage(null, null))
        }

    }

    companion object {
        private const val TAG = "WiFiClient"
    }
}