package es.ua.eps.serversidechat.utils

import es.ua.eps.serversidechat.adapter.IN_MESSAGE

object MessageChatData {

    var messages : MutableList<Message> = mutableListOf()


    init {

        var f = Message()
        f.content = "AAAAA"
        f.authorName = "EEEEEE"
        f.color = "#D8A300"
        f.type = IN_MESSAGE
        messages.add(f)

        f = Message()
        f.content = "QUE PASA COLEGA"
        f.authorName = "YO"
        messages.add(f)

        f = Message()
        f.content = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        f.authorName = "EEEEEEE"
        f.color = "#D8A300"
        f.type = IN_MESSAGE
        messages.add(f)
    }
}