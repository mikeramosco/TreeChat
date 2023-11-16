package com.example.treechat

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_channel.*
import java.sql.Timestamp

class ChannelActivity : AppCompatActivity() {

    private val channelsTableName = "channels"
    private val messagesChild = "messages"
    private val nameChild = "name"
    private val fromChild = "from"
    private val textChild = "text"
    private val timestampChild = "timestamp"

    private lateinit var userLoggedIn: String
    private lateinit var channelName: String
    private lateinit var channelKey: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)
        userLoggedIn = intent.getStringExtra("userLoggedIn")!!
        channelName = intent.getStringExtra("channelName")!!
            .replace("#", "")
        channel_name_text_view.text = "#$channelName"
        updateOpenedChannelInPreferences(channelName)
        getChannelData()
    }

    private fun updateOpenedChannelInPreferences(openedChannel: String) {
        val sharedPreferences = getSharedPreferences(
            SHARED_PREF, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(OPENED_CHANNEL_REF, openedChannel)
        editor.apply()
    }

    private fun getChannelData() {
        val thisInstance = this
        val fb = FirebaseDatabase.getInstance().reference
        val channelsTable = fb.child(channelsTableName)
        val channelData = channelsTable.orderByChild(nameChild).equalTo(channelName)
        channelData.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                channelKey = data.children.iterator().next().key!!
                getChannelMessages()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getChannelMessages() {
        val thisInstance = this
        val fb = FirebaseDatabase.getInstance().reference
        val channelMessages =
            fb.child("$channelsTableName/$channelKey/$messagesChild")
        val allMessages = channelMessages.orderByChild(timestampChild)
        allMessages.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                val listOfMessages = ArrayList<String>()
                var readyToUpdateMessages = true
                for (message in data.children) {
                    val timestamp = message.child(timestampChild).value.toString()
                    if(timestamp == "null") readyToUpdateMessages = false
                    val from = message.child(fromChild).value.toString()
                    val text = message.child(textChild).value.toString()
                    if (from != "") listOfMessages.add("$from: $text")
                }
                if(readyToUpdateMessages)
                    updateMessagesView(listOfMessages)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateMessagesView(listOfMessages: ArrayList<String>) {
        var messagesText = ""
        for((count, message) in listOfMessages.withIndex()) {
            if(count > 0) messagesText += "\n"
            messagesText += message
        }
        messages_text_view.text = messagesText
        messages_scroll_view.post {
            messages_scroll_view.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun sendMessageClicked(view: View) {
        val message = message_field.text.toString()
        message_field.setText("")

        val fb = FirebaseDatabase.getInstance().reference
        val currentTimestamp = Timestamp(System.currentTimeMillis()).toString()
        val newMessage = fb.child(
            "$channelsTableName/$channelKey/$messagesChild").push()
        newMessage.child(fromChild).setValue(userLoggedIn)
        newMessage.child(textChild).setValue(message)
        newMessage.child(timestampChild).setValue(currentTimestamp)

    }

    override fun onStop() {
        super.onStop()
        updateOpenedChannelInPreferences("none")
    }
}
