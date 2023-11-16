package com.example.treechat

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_channel_list.*
import java.sql.Timestamp

class ChannelListActivity : AppCompatActivity() {

    private val channelsTableName = "channels"
    private val nameChild = "name"
    private val descriptionChild = "description"
    private val membersChild = "members"
    private val messagesChild = "messages"
    private val messagesCountChild = "messagescount"
    private val membersEmailChild = "email"
    private val messagesFromChild = "from"
    private val messagesTextChild = "text"
    private val messagesTimeChild = "timestamp"

    private lateinit var userLoggedIn: String
    private lateinit var listOfChannels: ArrayList<String>

    private val notifChannel = "channel1"
    private lateinit var notifManager : NotificationManagerCompat

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_list)
        userLoggedIn = intent.getStringExtra("email")!!
        description.text = "Hello, $userLoggedIn"
        createNotificationChannels()
        setListenerForNewMessages()
        getChannelsFromFirebaseToDisplay()
        list_of_channels_view.setOnItemClickListener { _, _, index, _ ->
            openChannel(listOfChannels[index])
        }
    }

    private fun createNotificationChannels() {
        // notification channels not needed for old Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notifChannel, "Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
        notifManager = NotificationManagerCompat.from(this)
    }

    private fun setListenerForNewMessages() {
        val thisInstance = this
        val fb = FirebaseDatabase.getInstance().reference
        val channelsTable = fb.child(channelsTableName)
        channelsTable.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                handleChannelsDataChanges(data)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleChannelsDataChanges(data: DataSnapshot) {
        val sharedPreferences = getSharedPreferences(
            SHARED_PREF, MODE_PRIVATE)
        val openedChannel = sharedPreferences
            .getString(OPENED_CHANNEL_REF, "none")!!
        for(channel in data.children) {
            val channelName = channel.child(nameChild).value.toString()
            if(listOfChannels.contains("#$channelName")) {
                val messages = channel.child(messagesChild)
                val newMessagesCount = messages.childrenCount.toInt()
                val savedMessagesCount =
                    channel.child(messagesCountChild).value.toString().toInt()
                if (newMessagesCount != savedMessagesCount) {
                    handleNewMessage(channel, newMessagesCount, messages,
                        channelName, openedChannel)
                }
            }
        }
    }

    private fun handleNewMessage(channel: DataSnapshot, newMessagesCount: Int,
                                 messages: DataSnapshot, channelName: String,
                                 openedChannel: String) {
        val thread = Thread {
            Thread.sleep(1000)
            channel.ref.child(messagesCountChild).setValue(newMessagesCount)
        }
        thread.start()

        val lastMessage = messages.children.last()
        val from = lastMessage.child(messagesFromChild).value.toString()
        val text = lastMessage.child(messagesTextChild).value.toString()
        val timestamp = lastMessage.child(messagesTimeChild).value.toString()
        if (channelName != openedChannel && timestamp != "null") {
            createNotification(channelName, from, text)
        }
    }

    private fun createNotification(channel: String, from: String, text: String) {
        val notification = NotificationCompat.Builder(this, notifChannel)
            .setSmallIcon(R.drawable.icon_chat)
            .setContentTitle("New Message in #$channel")
            .setContentText("$from: $text")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        notifManager.notify(1, notification)
    }

    private fun getChannelsFromFirebaseToDisplay() {
        val thisInstance = this
        val fb = FirebaseDatabase.getInstance().reference
        val channelsTable = fb.child(channelsTableName)
        val allChannels = channelsTable.orderByChild(nameChild)
        allChannels.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                listOfChannels = ArrayList()
                for(channelData in data.children) {
                    val name = channelData.child(nameChild).value.toString()
                    val members = channelData.child(membersChild).value.toString()
                    if(members.contains(userLoggedIn))
                        listOfChannels.add("#$name")
                }
                val adapter = ArrayAdapter(thisInstance,
                    android.R.layout.simple_list_item_1, listOfChannels)
                list_of_channels_view.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun openChannel(channelName: String) {
        val openPage = Intent(this, ChannelActivity::class.java)
        openPage.putExtra("channelName", channelName)
        openPage.putExtra("userLoggedIn", userLoggedIn)
        startActivity(openPage)
    }

    fun createChannelClicked(view: View) {
        val thisInstance = this
        val channelName = channel_field.text.toString()
        channel_field.setText("")
        val fb = FirebaseDatabase.getInstance().reference
        val channelsTable = fb.child(channelsTableName)
        val channelOfSubmittedName = channelsTable.orderByChild(nameChild).equalTo(channelName)
        channelOfSubmittedName.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                var channelExists = false
                var userIsNotAMember = false
                var channelKey = ""
                for(channelData in data.children) {
                    channelExists = true
                    channelKey = channelData.key!!
                    val members = channelData.child(membersChild).value.toString()
                    if(!members.contains(userLoggedIn)) userIsNotAMember = true
                    break
                }

                addUserToChannelOrCreateChannel(channelExists, userIsNotAMember,
                                                channelsTable, channelKey,
                                                thisInstance, channelName)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun addUserToChannelOrCreateChannel(channelExists: Boolean, userIsNotAMember: Boolean,
                                                channelsTable: DatabaseReference, channelKey: String,
                                                thisInstance: ChannelListActivity, channelName: String) {
        if(channelExists) {
            if(userIsNotAMember) {
                // Add user to list of channel's members
                val newMember = channelsTable
                    .child("$channelKey/$membersChild").push()
                newMember.child(membersEmailChild).setValue(userLoggedIn)
                Toast.makeText(
                    thisInstance, "You are now a member of \'$channelName\'",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // User is already a member of this channel
                Toast.makeText(
                    thisInstance, "You are already a member of \'$channelName\'",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // channel does not exist
            createChannel(channelName)
            Toast.makeText(
                thisInstance, "You have created the new channel \'$channelName\'",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createChannel(channelName: String) {

        // adds a new channel to firebase channelsTable
        val fb = FirebaseDatabase.getInstance().reference
        val channelsTable = fb.child(channelsTableName)
        val newChannel = channelsTable.push()
        newChannel.child(nameChild).setValue(channelName)
        newChannel.child(descriptionChild).setValue("")
        newChannel.child(messagesCountChild).setValue(1)

        // adds current user to array of members
        val newMember = newChannel.child(membersChild).push()
        newMember.child(membersEmailChild).setValue(userLoggedIn)

        // adds blank message to array of messages
        val currentTimestamp = Timestamp(System.currentTimeMillis()).toString()
        val newMessage = newChannel.child(messagesChild).push()
        newMessage.child(messagesFromChild).setValue("")
        newMessage.child(messagesTextChild).setValue("")
        newMessage.child(messagesTimeChild).setValue(currentTimestamp)
    }

    fun logOutClicked(view: View) {
        val sharedPreferences = getSharedPreferences(
            SHARED_PREF, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(LOGIN_REF, false)
        editor.apply()
        finish()
    }
}
