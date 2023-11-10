package es.ua.eps.serversidechat.utils

import android.graphics.drawable.Drawable
import es.ua.eps.serversidechat.adapter.OUT_MESSAGE
import java.util.Date

class Message {

    var authorName: String? = null
    var date: Date? = null
    var type: Int = OUT_MESSAGE
    var color: String = "#7F92FB"
    var content: String? = null
    var client : Client? = null
    var image : Drawable? = null
    var hasImage : Boolean = false

    constructor()

    constructor(message : Message){
        authorName = message.authorName
        date = message.date
        type = message.type
        color = message.color
        content = message.content
        hasImage = message.hasImage
        image = message.image
    }
}