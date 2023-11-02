package es.ua.eps.serversidechat.utils

import es.ua.eps.serversidechat.MainActivity
import java.net.Socket

class Client {
    var name : String? = null
    var socket : Socket? = null
    var chatThread : MainActivity.ConnectionThread? = null
}