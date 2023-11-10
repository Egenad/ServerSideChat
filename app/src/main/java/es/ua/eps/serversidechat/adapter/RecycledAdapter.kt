package es.ua.eps.serversidechat.adapter

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import es.ua.eps.serversidechat.PACKAGE_NAME
import es.ua.eps.serversidechat.R
import es.ua.eps.serversidechat.java.ContextBuilder
import es.ua.eps.serversidechat.utils.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val OUT_MESSAGE = 0
const val IN_MESSAGE = 1

class RecycledAdapter(val messageList: List<Message>) :
    RecyclerView.Adapter<RecycledAdapter.ViewHolder?>() {

    private var listener: (msgPosition: Int) -> Unit = {}
    private var listenerLong: (msgPosition: Int) -> Boolean = {false}

    inner class outViewHolder(v: View) : ViewHolder(v) {
        init {
            v.setOnClickListener {
                listener(adapterPosition)
            }

            v.setOnLongClickListener {
                listenerLong(adapterPosition)
            }
        }
    }

    inner class inViewHolder(v: View) : ViewHolder(v){
        init {
            v.setOnClickListener {
                listener(adapterPosition)
            }

            v.setOnLongClickListener {
                listenerLong(adapterPosition)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return messageList[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        var viewHolder : ViewHolder? = null

        when(viewType){
            OUT_MESSAGE -> {
                viewHolder = outViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.msg_item_out, parent, false)
                )
            }
            IN_MESSAGE -> {
                viewHolder = inViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.msg_item_in, parent, false)
                )
            }
        }

        return viewHolder!!
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    open class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var author: TextView?
        private var content: TextView
        private var time: TextView
        private var image: ImageView?
        private var constraint: ConstraintLayout?
        private val view = v

        fun bind(it: Message) {
            author?.text = it.authorName
            content.text = it.content
            if(content.text.isBlank()) content.visibility = View.GONE

            var dateString = ""

            try{
                val format = SimpleDateFormat("HH:mm", Locale.US)
                dateString = format.format(it.date ?: Date())
            }catch (error : Exception){
                Log.e(PACKAGE_NAME, error.stackTraceToString())
            }

            if(it.image != null) {
                image?.setImageDrawable(it.image)
                constraint?.visibility = View.VISIBLE
            }else{
                constraint?.visibility = View.GONE
            }

            time.text = dateString

            if(it.type == IN_MESSAGE)
                author?.setTextColor(Color.parseColor(it.color))
        }

        init {
            author = view.findViewById(R.id.author)
            content = view.findViewById(R.id.content)
            time = view.findViewById(R.id.time)
            image = view.findViewById(R.id.imageView)
            constraint = view.findViewById(R.id.constraint)
        }
    }

    fun setOnItemClickListener(listener: (filmPosition: Int) -> Unit) {
        this.listener = listener
    }

    fun setOnLongItemClickListener(listener: (filmPosition: Int) -> Boolean) {
        this.listenerLong = listener
    }

}