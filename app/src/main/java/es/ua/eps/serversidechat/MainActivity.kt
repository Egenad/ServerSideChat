package es.ua.eps.serversidechat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import es.ua.eps.serversidechat.databinding.ActivityMainBinding
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
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

        // Obtenemos la dirección IP para verificar que tenemos conexión
        Log.i(PACKAGE_NAME, getIpAddress())

        // Creamos el Server Socket y lo lanzamos (generamos otro hilo distinto del principal)
        val serverSocket = ServerSocketThread()
        serverSocket.start()
    }

    /**
     *
     * Función que hace uso de la interfaz NetworkInterface para obtener la dirección
     * IP actual, es decir, la IP del servidor.
     *
     * @return Cadena de texto que contiene la dirección IP del servidor
     */
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

    /**
     *
     */
    inner class ServerSocketThread : Thread(){

        private var connections = 0

        override fun run() {
            super.run()

            try{

                // Generamos el Server Socket utilizando el puerto definido por defecto.
                serverSocket = ServerSocket(resources.getInteger(R.integer.server_port))

                while(true){

                    // Aceptamos conexiones entrantes. Este hilo se quedará suspendido hasta
                    // que nos llegue una solicitud. En ese momento aumentaremos el contador de
                    // conexiones
                    val socket : Socket = serverSocket.accept()
                    connections++

                    Log.i(PACKAGE_NAME, "Connection number $connections from: ${socket.inetAddress}")

                    // Enviamos una respuesta utilizando el socket aceptado.
                    ServerSocketReplyThread(socket).sendReply()

                }
            }catch (error : IOException){
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }
        }
    }

    /**
     * @param Socket aceptado del cliente
     */
    inner class ServerSocketReplyThread(socket : Socket){

        private val hostThreadSocket = socket

        fun sendReply(){

            try {
                // Abrimos la salida de flujo de datos utilizando el socket del cliente.
                val outputStream: OutputStream = hostThreadSocket.getOutputStream()

                // Le devolvemos al cliente un mensaje simple y cerramos la conexión.
                val printStream = PrintStream(outputStream)
                printStream.print("Hello from the Server side")
                printStream.close()

                Log.i(PACKAGE_NAME, "Replayed: Hello from the Server side")

            }catch (error : IOException) {
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }
        }
    }
}