package es.ua.eps.serversidechat

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import es.ua.eps.serversidechat.databinding.ActivityMainBinding
import es.ua.eps.serversidechat.utils.Client
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

const val PACKAGE_NAME = "es.ua.eps.server"
const val PERMISSION_CODE = 101

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private var serverSocket : ServerSocket? = null

    private var usersList : MutableList<Client> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (serverSocket != null) {
            try {
                serverSocket!!.close()
            } catch (error : IOException) {
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }
        }
    }

    private fun checkPermissions(){

        val permsArray = arrayOf(
            android.Manifest.permission.INTERNET
        )

        if(hasPermissions(permsArray))
            startServer()
        else
            askPermissions(permsArray)
    }

    private fun hasPermissions(perms : Array<String>) : Boolean{
        return perms.all {
            return ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun askPermissions(perms : Array<String>){
        ActivityCompat.requestPermissions(
            this,
            perms,
            PERMISSION_CODE)
    }

    private fun startServer(){

        createRecyclerList()

        // Obtenemos la dirección IP para verificar que tenemos conexión
        val ipAddress = getIpAddress()
        Log.i(PACKAGE_NAME, ipAddress)
        binding.ipAddress.text = "${binding.ipAddress.text} $ipAddress"
        binding.portAddress.text = "${binding.portAddress.text} ${resources.getInteger(R.integer.server_port)}"

        // Creamos el Server Socket y lo lanzamos (generamos otro hilo distinto del principal)
        val serverSocket = ServerSocketThread()
        serverSocket.start()
    }

    private fun createRecyclerList(){
        binding.btnSend.setOnClickListener {
            if(binding.btnSend.text.isNotBlank()){
                broadcastMsg(binding.btnSend.text.toString())
                binding.btnSend.text = ""
            }
        }
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

        override fun run() {
            super.run()

            try{

                // Generamos el Server Socket utilizando el puerto definido por defecto.
                serverSocket = ServerSocket(resources.getInteger(R.integer.server_port))

                while(true){

                    // Aceptamos conexiones entrantes. Este hilo se quedará suspendido hasta
                    // que nos llegue una solicitud. En ese momento aumentaremos el contador de
                    // conexiones

                    Log.i(PACKAGE_NAME, "Listening for connections")

                    val socket : Socket = serverSocket!!.accept()

                    // Creamos un cliente nuevo y enviamos una respuesta utilizando el socket aceptado.
                    val newClient = Client()
                    usersList.add(newClient)

                    updateHUD()

                    ConnectionThread(newClient, socket).start()

                }
            }catch (error : IOException){
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }
        }
    }

    /**
     * @param Socket aceptado del cliente
     */
    inner class ConnectionThread(client : Client, socket : Socket) : Thread(){

        private val hostThreadSocket = socket
        private val connectedClient = client

        private var msgToSend = ""

        init {
            connectedClient.socket = hostThreadSocket
            connectedClient.chatThread = this
        }

        override fun run(){

            var inputStream : DataInputStream? = null
            var outputStream : DataOutputStream? = null

            try {

                inputStream = DataInputStream(hostThreadSocket.getInputStream())
                outputStream = DataOutputStream(hostThreadSocket.getOutputStream())

                connectedClient.name = inputStream.readUTF()

                Log.i(PACKAGE_NAME, "Connected client with name: ${connectedClient.name}")

                outputStream.writeUTF("Server: Welcome ${connectedClient.name}")
                outputStream.flush()

                while(true){
                    if(inputStream.available() > 0){
                        val receivedMsg = inputStream.readUTF()

                        Log.i(PACKAGE_NAME, "Message received: $receivedMsg")

                        /*binding.listFragment.post{
                            //bindingRecycled.list.adapter?.notifyItemInserted(FilmDataSource.films.size - 1)
                            //binding.listFragment

                            var msg = Message()
                            msg.content =

                            MessageChatData.messages.add()

                            val listFragmentStatic = supportFragmentManager.findFragmentById(R.id.list_fragment) as? FilmListFragment
                            val listFragmentDynamic = supportFragmentManager.findFragmentById(R.id.fragment_container) as? FilmListFragment

                            listFragmentStatic?.notifyItemInserted()
                        }*/
                    }

                    if(msgToSend.isNotBlank()){
                        outputStream.writeUTF(msgToSend)
                        outputStream.flush()
                        msgToSend = ""
                    }
                }

            }catch (error : Exception) {
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }finally {
                if(inputStream != null){
                    try{
                        inputStream.close()
                    }catch (error : Exception){
                        Log.e(PACKAGE_NAME, error.stackTraceToString())
                    }
                }
            }

            if(inputStream != null){
                try{
                    inputStream.close()
                }catch (error : Exception){
                    Log.e(PACKAGE_NAME, error.stackTraceToString())
                }
            }

            usersList.remove(connectedClient)
            updateHUD()
            Log.i(PACKAGE_NAME, "Desconnected user: ${connectedClient.name}")
        }

        fun sendMsg(msg: String) {
            msgToSend = msg
        }
    }

    private fun broadcastMsg(msg: String) {
        for (i in 0 until usersList.size) {
            usersList[i].chatThread?.sendMsg(msg)
        }
    }

    private fun updateHUD(){

        Log.i(PACKAGE_NAME, "Connection number ${usersList.size}")

        if(usersList.isNotEmpty()){
            binding.linearLayout1.post {
                binding.linearLayout1.visibility = View.VISIBLE
                binding.linearLayout1.isEnabled = true
            }
            binding.listFragment.post {
                binding.listFragment.visibility = View.VISIBLE
                binding.listFragment.isEnabled = true
            }
        }else{
            binding.linearLayout1.post {
                binding.linearLayout1.visibility = View.INVISIBLE
                binding.linearLayout1.isEnabled = false
            }
            binding.listFragment.post {
                binding.listFragment.visibility = View.INVISIBLE
                binding.listFragment.isEnabled = false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_CODE){
            val allPerms = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if(grantResults.isNotEmpty() && allPerms)
                startServer()
        }
    }
}