package com.example.askgemini

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatList = mutableListOf<ChatItem>()
    private val chats: MutableList<ChatItem> = mutableListOf()

    fun addChat(chatItem: ChatItem) {
        chatList.add(chatItem)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = chatList[position]
        if (holder is ChatViewHolder) {
            holder.bind(chatItem)
        }
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        private val textAnswer: TextView = itemView.findViewById(R.id.textAnswer)

        fun bind(chatItem: ChatItem) {
            if (chatItem.isUser) {
                textMessage.visibility = View.VISIBLE
                textAnswer.visibility = View.GONE
                textMessage.text = chatItem.message
            } else {
                textMessage.visibility = View.GONE
                textAnswer.visibility = View.VISIBLE
                textAnswer.text = chatItem.message
            }
        }
    }

    fun clear() {
        chatList.clear()
        notifyDataSetChanged()
    }
}