package com.example.askgemini

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val API_KEY = "AIzaSyCDvOqxT3TMnVslS38ToxTFFiS3JTad6Cc"
    private lateinit var chat: Chat
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextInput: EditText
    private val SEARCH_HISTORY_PREF_KEY = "search_history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter()
        recyclerView.adapter = chatAdapter

        editTextInput = findViewById(R.id.editTextInput)

        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = API_KEY
        )

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text("Hello") },
                content(role = "model") { text("Hello, how can I help you?") }
            )
        )
        addChatToAdapter("Hello", true)
        addChatToAdapter("Hello, how can I help you?", false)
    }

    fun buttonSendChat(view: View) {
        val userMessage = editTextInput.text.toString()
        addChatToAdapter(userMessage, true)

        MainScope().launch {
            val result = chat.sendMessage(userMessage)
            result.text?.let { addChatToAdapter(it, false) }
        }
        editTextInput.setText("")
    }

    fun buttonMoreOptions(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.new_chat -> {
                    chatAdapter.clear()
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-pro",
                        apiKey = API_KEY
                    )
                    chat = generativeModel.startChat()
                    true
                }
                R.id.help -> {
                    true
                }
                R.id.history -> {
                    showSearchHistory(view)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showSearchHistory(anchorView: View) {
        val searchHistoryList = retrieveSearchHistory(this)
        val popupMenu = PopupMenu(this, anchorView)
        if (searchHistoryList.isEmpty()) {
            popupMenu.menu.add("No search history")
        } else {
            for (search in searchHistoryList) {
                popupMenu.menu.add(search)
            }
            val clearHistoryItem = popupMenu.menu.add("Clear History")
            clearHistoryItem.setIcon(android.R.drawable.ic_menu_delete)
        }
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.title.toString() == "Clear History") {
                clearSearchHistory()
            } else {
                val searchQuery = item.title.toString()
                addChatToAdapter(searchQuery, true)
                MainScope().launch {
                    val result = chat.sendMessage(searchQuery)
                    result.text?.let { addChatToAdapter(it, false) }
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun clearSearchHistory() {
        val sharedPreferences = getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(SEARCH_HISTORY_PREF_KEY, "")
        editor.apply()
    }
    private fun retrieveSearchHistory(context: Context): List<String> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            "MySharedPreferences",
            Context.MODE_PRIVATE
        )

        val searchHistoryString: String? = sharedPreferences.getString(SEARCH_HISTORY_PREF_KEY, "")
        return searchHistoryString?.split(",")?.filterNotNull()?.filter { it.trim().isNotEmpty() }?.toList() ?: emptyList()
    }

    private fun addChatToAdapter(message: String, isUser: Boolean) {
        val chatItem = ChatItem(message, isUser)
        chatAdapter.addChat(chatItem)
        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)

        if (isUser) {
            saveSearchToHistory(message)
        }
    }

    private fun saveSearchToHistory(search: String) {
        if (search.trim().isNotEmpty()) {
            val sharedPreferences: SharedPreferences = getSharedPreferences(
                "MySharedPreferences",
                Context.MODE_PRIVATE
            )

            val currentHistory = retrieveSearchHistory(this).toMutableList()
            currentHistory.add(search)

            val historyString = currentHistory.joinToString(",")
            val editor = sharedPreferences.edit()
            editor.putString(SEARCH_HISTORY_PREF_KEY, historyString)
            editor.apply()
        }
    }
}