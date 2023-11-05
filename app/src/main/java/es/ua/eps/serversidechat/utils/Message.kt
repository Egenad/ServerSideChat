package es.ua.eps.serversidechat.utils

import es.ua.eps.serversidechat.adapter.OUT_MESSAGE
import java.util.Date
import java.util.UUID

class Message {

    var authorName: String? = null
    var date: Date? = null
    var type: Int = OUT_MESSAGE
    var color: String = "#7F92FB"
    var content: String? = null
    var client : Client? = null

}