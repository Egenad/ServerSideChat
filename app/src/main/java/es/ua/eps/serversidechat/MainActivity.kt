package es.ua.eps.serversidechat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import es.ua.eps.serversidechat.databinding.ActivityMainBinding
import java.io.IOException
import java.io.OutputStream
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

const val PACKAGE_NAME = "es.ua.eps"

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private lateinit var serverSocket : ServerSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i(PACKAGE_NAME, getIpAddress())
    }

    private fun getIpAddress() : String{

        var ipAddress = ""

        try{
            val netInterfaces = NetworkInterface.getNetworkInterfaces()

            var found = false

            while (netInterfaces.hasMoreElements() && !found){
                val inetAddresses = netInterfaces.nextElement().inetAddresses
                while(inetAddresses.hasMoreElements() && !found){

                    val actualAddress = inetAddresses.nextElement()

                    if(actualAddress.isSiteLocalAddress){
                        found = true
                        ipAddress = actualAddress.hostAddress ?: "Error"
                    }
                }
            }
        }catch(error : SocketException){
            Log.e(PACKAGE_NAME, error.stackTraceToString())
        }

        return ipAddress
    }

    inner class ServerSocketThread : Thread(){

        private var connections = 0

        override fun run() {
            super.run()

            try{
                serverSocket = ServerSocket(resources.getInteger(R.integer.server_port))

                while(true){
                    val socket : Socket = serverSocket.accept()
                    connections++

                    Log.i(PACKAGE_NAME, "Connection from: ${socket.inetAddress}")

                    ServerSocketReplyThread(socket, connections).run()

                }
            }catch (error : IOException){
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }
        }
    }

    inner class ServerSocketReplyThread(socket : Socket, count : Int){

        val hostThreadSocket = socket
        val connectionNmbr = count

        fun run(){
            val outputStream : OutputStream = hostThreadSocket.getOutputStream()

            
        }
    }
}