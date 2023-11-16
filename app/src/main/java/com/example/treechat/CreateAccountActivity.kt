package com.example.treechat

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_create_account.*

class CreateAccountActivity : AppCompatActivity() {

    private val usersTableName = "users"
    private val emailChild = "email"
    private val humanChild = "human"
    private val nameChild = "name"
    private val usernameChild = "username"
    private val minimumPasswordLength = 6
    private var loggedOutCode = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
    }

    @SuppressLint("DefaultLocale")
    fun createAccountClicked(view: View) {
        val email = email_field.text.toString().toLowerCase()
        val password = password_field.text.toString()
        val confirm = confirm_field.text.toString()
        if(email == "" || password == "" || confirm == "") {
            Toast.makeText(this, "Text fields cannot be blank",
                Toast.LENGTH_LONG).show()
        } else if(password != confirm) {
            Toast.makeText(this, "Password fields do not match",
                Toast.LENGTH_LONG).show()
        } else if(password.length < minimumPasswordLength) {
            Toast.makeText(this, "Password must be at least " +
                    "$minimumPasswordLength characters",
                Toast.LENGTH_LONG).show()
        } else {
            checkIfEmailAlreadyExists(email, password)
        }
    }

    private fun checkIfEmailAlreadyExists(email: String, password: String) {
        val thisInstance = this
        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(usersTableName)
        val usersWithSubmittedEmail = usersTable.orderByChild(emailChild).equalTo(email)
        usersWithSubmittedEmail.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                var userExists = false
                for (user in data.children) {
                    userExists = true
                    break
                }

                if (userExists) {
                    Toast.makeText(
                        thisInstance, "A user with this email already exists",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    createAccount(email, password)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun createAccount(email: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val fb = FirebaseDatabase.getInstance().reference
                    val usersTable = fb.child(usersTableName)
                    val newUser = usersTable.push()
                    newUser.child(emailChild).setValue(email)
                    newUser.child(humanChild).setValue(false)
                    newUser.child(nameChild).setValue("")
                    newUser.child(usernameChild).setValue("")

                    val openPage = Intent(this, ChannelListActivity::class.java)
                    openPage.putExtra("email", email)
                    startActivityForResult(openPage, loggedOutCode)
                } else {
                    Toast.makeText(this, task.exception.toString(),
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == loggedOutCode)
            finish()
    }
}
