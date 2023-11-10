package es.ua.eps.serversidechat

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.doOnTextChanged
import com.google.gson.Gson
import es.ua.eps.serversidechat.adapter.OUT_MESSAGE
import es.ua.eps.serversidechat.databinding.ActivityMainBinding
import es.ua.eps.serversidechat.fragment.MessageListFragment
import es.ua.eps.serversidechat.utils.AESHelper
import es.ua.eps.serversidechat.utils.Client
import es.ua.eps.serversidechat.utils.ClientKey
import es.ua.eps.serversidechat.utils.Message
import es.ua.eps.serversidechat.utils.MessageChatData
import es.ua.eps.serversidechat.utils.RSAHelper
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Date
import javax.crypto.SecretKey


const val PACKAGE_NAME = "es.ua.eps.server"
const val PERMISSION_CODE = 101
const val REQUEST_MEDIA_FILE = 1

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private lateinit var secretKey: SecretKey                       // clave simétrica AES

    private var serverSocket : ServerSocket? = null                 // Socket del servidor

    private var usersList : MutableList<Client> = mutableListOf()   // Lista de clientes conectados

    private var mediaPlayer : MediaPlayer? = null                   // Clase que nos permite lanzar eventos de sonido

    private val serverClient : Client = Client()                    // Objeto de tipo Cliente que tiene dentro el nombre del servidor

    private val colorArray = arrayOf("#FF5733", "#FFCE33", "#33FF8D", "#FF33CA", "#FFAF33")

    private val startForResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            onActivityResult(REQUEST_MEDIA_FILE, result.resultCode, result.data)
        }

    init {
        serverClient.name = "Server"
    }

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

        // Lista de permisos que deben estar garantizados
        val permsArray = arrayOf(
            android.Manifest.permission.INTERNET
        )

        // Comprobamos si tenemos los permisos necesarios
        if(hasPermissions(permsArray))
            startServer()               // Iniciamos el servidor
        else
            askPermissions(permsArray)  // Solicitamos los permisos al usuario
    }

    /**
     *  Función que nos devuelve 'true' en caso de que la aplicación
     *  ya tenga los permisos requeridos, indicados en el Array pasado
     *  por parámetro.
     */
    private fun hasPermissions(perms : Array<String>) : Boolean{
        return perms.all {
            return ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Función que solicita al usuario los permisos requeridos,
     * indicados en el Array pasado por parámetro.
     */
    private fun askPermissions(perms : Array<String>){
        ActivityCompat.requestPermissions(
            this,
            perms,
            PERMISSION_CODE)
    }

    /**
     * Función donde se encuentra la inicialización de la actividad,
     * tanto de la pantalla/HUD, como del hilo principal del servidor.
     */
    private fun startServer(){

        bindingInit()   // Generamos el binding del botón de enviar mensajes.
        updateHUD()     // Actualizamos el HUD para deshabilitar las funciones de envío de mensajes hasta que haya al menos 1 usuario conectado.

        // Generamos la clave simétrica AES
        secretKey = AESHelper.generateSecretKey()

        // Obtenemos la dirección IP para verificar que tenemos conexión y mostrarla por pantalla
        val ipAddress = getIpAddress()
        Log.i(PACKAGE_NAME, ipAddress)
        binding.ipAddress.text = "${binding.ipAddress.text} $ipAddress"
        binding.portAddress.text = "${binding.portAddress.text} ${resources.getInteger(R.integer.server_port)}"

        // Creamos el Server Socket y lo lanzamos (generamos otro hilo distinto del principal)
        val serverSocket = ServerSocketThread()
        serverSocket.start()
    }

    /**
     * Función que nos genera el binding del botón de enviar mensaje y la lógica
     * que debe seguir.
     */
    private fun bindingInit(){
        binding.btnSend.setOnClickListener {
            if(binding.textInput.text.isNotBlank()){
                // Enviamos el mensaje a todos los usuarios activos.
                val formatText = binding.textInput.text.toString().replace(Regex(" {2,}"), " ").trim()
                broadcastMsg(createMessage(formatText, OUT_MESSAGE, serverClient), null)
                // Lanzamos evento de audio.
                startAudio(R.raw.send_message)
                // Reseteamos el input de texto.
                binding.textInput.setText("")
            }
        }

        binding.textInput.doOnTextChanged { text, start, before, count ->
            run {
                if (text!!.isNotBlank()) {
                    binding.btnSend.visibility = View.VISIBLE
                    binding.btnSend.isEnabled = true
                    binding.btnMedia.visibility = View.GONE
                    binding.btnMedia.isEnabled = false
                }else{
                    binding.btnSend.visibility = View.GONE
                    binding.btnSend.isEnabled = false
                    binding.btnMedia.visibility = View.VISIBLE
                    binding.btnMedia.isEnabled = true
                }
            }
        }

        // Generamos el binding del botón de enviar imágenes.
        binding.btnMedia.setOnClickListener {
            val mediaFileIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            if (intent.resolveActivity(packageManager) != null) {
                if(Build.VERSION.SDK_INT >= 30) {
                    startForResult.launch(mediaFileIntent)
                }
                else
                    @Suppress("DEPRECATION")
                    startActivityForResult(mediaFileIntent, REQUEST_MEDIA_FILE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MEDIA_FILE && resultCode == RESULT_OK){
            val filePath: Uri? = data?.data

            if(filePath != null) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(filePath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    Log.i(PACKAGE_NAME, bitmap.toString())
                    if(bitmap != null) {
                        broadcastMsg(
                            createMessage(
                                binding.textInput.text.toString(),
                                OUT_MESSAGE,
                                serverClient
                            ),
                            bitmap
                        )
                        startAudio(R.raw.send_message)
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
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
                    usersList.add(newClient)                                                        // Añadimos el nuevo cliente a la lista de usuarios conectados.

                    var index = usersList.size - 1                                                  // Asignamos el color del cliente.
                    while(index > colorArray.size){
                        index /= colorArray.size
                    }
                    newClient.color = colorArray[index]

                    updateHUD()                                                                     // Actualizamos el HUD para desplegar el chat
                    ConnectionThread(newClient, socket).start()                                     // Lanzamos el hilo del cliente.

                }
            }catch (error : IOException){
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }
        }
    }

    private fun sendUserListMessage(){

        var completeList = "UserList: "

        for (i in 0 until usersList.size) {
            completeList += usersList[i].name + ", "
        }

        broadcastStringMsg(completeList.replace(Regex(", \\z"), ""))
    }

    /**
     * @param Socket aceptado del cliente
     */
    inner class ConnectionThread(client : Client, socket : Socket) : Thread(){

        private val hostThreadSocket = socket
        private val connectedClient = client

        private var msgToSend : Message? = null
        private var msgStringToSend : String? = null
        private var bitmapToSend : Bitmap? = null

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

                val input = inputStream.readUTF()
                val clientMsg = Gson().fromJson(input, ClientKey::class.java)
                connectedClient.name = clientMsg.clientName

                Log.i(PACKAGE_NAME, "Connected client with name: ${connectedClient.name}")

                val publicBytes: ByteArray = Base64.decode(clientMsg.publicKey!!, Base64.DEFAULT)
                val keySpec = X509EncodedKeySpec(publicBytes)
                val keyFactory = KeyFactory.getInstance("RSA")
                val pubKey = keyFactory.generatePublic(keySpec)

                val encodedKey: String = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)

                // Ciframos la clave AES utilizando la clave pública RSA del cliente y la devolvemos
                val encryptedAESKey = RSAHelper.encrypt(encodedKey, pubKey)
                outputStream.writeUTF(encryptedAESKey)
                outputStream.flush()

                Log.i(PACKAGE_NAME, "AES Key: $secretKey")

                // Enviamos mensaje a todos los clientes con la lista de usuarios actualizada.
                sendUserListMessage()

                while(true){
                    if(inputStream.available() > 0){
                        val receivedMsg = inputStream.readUTF()
                        val decryptedMsg = AESHelper.decrypt(receivedMsg, secretKey)
                        val parsedMsg = Gson().fromJson(decryptedMsg, Message::class.java)
                        parsedMsg.client = connectedClient
                        parsedMsg.color = connectedClient.color ?: "#7F92FB"

                        if(parsedMsg.hasImage){
                            val len = inputStream.readInt()
                            val data = ByteArray(len)
                            inputStream.readFully(data, 0, data.size)
                            parsedMsg.hasImage = true
                            parsedMsg.image = parseImageByteArray(AESHelper.decrypt(data, secretKey)) // Obtenemos el Drawable a mostrar a partir del bitmap
                        }

                        Log.i(PACKAGE_NAME, "Message received: $receivedMsg")
                        startAudio(R.raw.pop_up)
                        broadcastMsg(parsedMsg, parsedMsg.image?.toBitmap())
                    }

                    if(msgStringToSend != null){                                                    // Comprobamos si hay mensajes simples informativos para enviar
                        val encryptedMsg = AESHelper.encrypt(msgStringToSend!!, secretKey)
                        outputStream.writeUTF(encryptedMsg)
                        outputStream.flush()
                        msgStringToSend = null
                    }

                    if(msgToSend != null){                                                          // Comprobamos si hay mensajes por parte del propio server u otros clientes por enviar
                        if(bitmapToSend != null) msgToSend!!.hasImage = true
                        val newMsg = Message(msgToSend!!)
                        val rawMsg = Gson().toJson(newMsg)
                        val encryptedMsg = AESHelper.encrypt(rawMsg, secretKey)
                        outputStream.writeUTF(encryptedMsg)
                        outputStream.flush()
                        msgToSend = null

                        if(bitmapToSend != null) {
                            val stream = ByteArrayOutputStream()
                            bitmapToSend!!.compress(Bitmap.CompressFormat.PNG, 0, stream)    // Comprimimos la imagen para enviar sus bytes por socket.
                            val array = AESHelper.encrypt(stream.toByteArray(), secretKey)
                            outputStream.writeInt(array.size)
                            outputStream.write(array,0,array.size)                              // Lo escribimos por el flujo de salida de datos.
                            outputStream.flush()
                            bitmapToSend = null                                                     // Reiniciamos el valor del bitmap
                        }

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
                if(outputStream != null){
                    try{
                        outputStream.close()
                    }catch (error : Exception){
                        Log.e(PACKAGE_NAME, error.stackTraceToString())
                    }
                }
            }

            usersList.remove(connectedClient)
            updateHUD()
            Log.i(PACKAGE_NAME, "Disconnected user: ${connectedClient.name}")
        }

        fun sendMsg(msg: Message) {
            msg.image = null
            msgToSend = msg
        }

        fun sendStringMsg(msg: String) {
            msgStringToSend = msg
        }

        fun sendImage(image: Bitmap?) {
            bitmapToSend = image
        }
    }

    private fun broadcastMsg(msg: Message, image: Bitmap?) {

        val newMsg = Message(msg)

        for (i in 0 until usersList.size) {
            if(usersList[i] != msg.client) {
                usersList[i].chatThread?.sendMsg(msg)
                usersList[i].chatThread?.sendImage(image)
            }
        }

        addMessageMainThread(newMsg)
    }

    private fun broadcastStringMsg(msg : String) {
        for (i in 0 until usersList.size) {
            usersList[i].chatThread?.sendStringMsg(msg)
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

    private fun createMessage(content : String, type : Int, user : Client?) : Message{
        val msg = Message()
        msg.content = content
        msg.type = type
        msg.date = Date()
        msg.authorName = user?.name
        return msg
    }

    private fun addMessageMainThread(message : Message){
        binding.listFragment.post{
            MessageChatData.messages.add(message)
            val listFragmentStatic = supportFragmentManager.findFragmentById(R.id.list_fragment) as? MessageListFragment
            listFragmentStatic?.notifyItemInserted()
        }
    }

    /**
     * Función que lanza un evento de audio según el identificador recibido
     * por parámetro.
     */
    private fun startAudio(audio : Int){
        try {
            mediaPlayer = MediaPlayer.create(this, audio)
            mediaPlayer?.start()
        }catch (error : Exception){ // El sonido pasado por parámetro no existe.
            Log.e(PACKAGE_NAME, error.stackTraceToString())
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

    private fun parseImageByteArray(byteArray : ByteArray?) : Drawable?{
        if(byteArray != null) {
            val bitmapReceived = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            return BitmapDrawable(resources, bitmapReceived)
        }
        return null
    }
}