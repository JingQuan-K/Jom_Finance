package com.example.jom_finance

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.jom_finance.models.Category
import com.example.jom_finance.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SignUpActivity : AppCompatActivity(){

    private lateinit var fAuth : FirebaseAuth
    private lateinit var fStore : FirebaseFirestore
    private lateinit var userID : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        setupDataBase()
        btn_Sign_Google.setOnClickListener{
            googleSetup()
            googleSignIn()
        }

        /*https://android--code.blogspot.com/2020/02/android-kotlin-ktx-clickablespan-example.html for the T&C spanable*/
        btn_sign_up.setOnClickListener {
            if(validate()){
                val email = SignUpEmailField.text.toString().trim()
                val password = SignUpPasswordField.text.toString().trim()
                val name = SignUpNameField.text.toString()
                fAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        try {
                            val currentUser = fAuth.currentUser
                            if (currentUser != null) {
                                userID = currentUser.uid
                            }
                            var documentReference = fStore.collection("users").document(userID)
                            val user = User(email,name)
                            documentReference.set(user).addOnSuccessListener {
                                //Name,icon,Color
                                //food,transport,shopping

                                documentReference = fStore.collection("transaction").document(userID)
                                var initialData = HashMap<String,Int>()
                                initialData.put("Income",0)
                                initialData.put("Expense",0)
                                initialData.put("Transaction_counter",0)
                                documentReference.set(initialData).addOnFailureListener{
                                    Toast.makeText(this,it.message.toString(), Toast.LENGTH_SHORT).show()
                                }.addOnSuccessListener {
                                    documentReference = fStore.collection("accounts").document(userID)
                                    var initialAccount = HashMap<String,Int>()
                                    initialAccount.put("Total",0)
                                    documentReference.set(initialAccount).addOnFailureListener{
                                        Toast.makeText(this,it.message.toString(), Toast.LENGTH_SHORT).show()
                                    }
                                    documentReference = fStore.collection("budget").document(userID)
                                    var initialbudget = HashMap<String,Int>()
                                    initialbudget.put("Budget_counter",0)
                                    documentReference.set(initialbudget).addOnFailureListener{
                                        Toast.makeText(this,it.message.toString(), Toast.LENGTH_SHORT).show()
                                    }

                                    var categoryNameArray = arrayOf("Food", "Transport", "Shopping")
                                    var categoryIconArray = arrayOf(452,384,258)
                                    var categoryColorArray = arrayOf(-278483,-10044566,-12627531)
                                    var defaultCategory : Category
                                    for(i in 0..2){
                                        documentReference = fStore.collection("category/$userID/category_detail").document(categoryNameArray[i])
                                        defaultCategory = Category(categoryNameArray[i],categoryIconArray[i],categoryColorArray[i])
                                        documentReference.set(defaultCategory)
                                    }
                                    // TODO : Direct Login
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                    Toast.makeText(this,"User Account Created", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }catch (e : Exception){
                            Toast.makeText(this," "+e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener{
                        Toast.makeText(this," "+it.message, Toast.LENGTH_SHORT).show()
                    }
            }
        }

    }

    private fun setupDataBase(){
        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()
    }

    fun openLogin(view: View?) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    fun backFun(view: View?) {
        this.finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun validate() : Boolean{
        var result : Boolean = false
        var snack  : Snackbar
        if(SignUpNameField.text.isNullOrEmpty()){
            SignUpNameField.requestFocus()
            snack = Snackbar.make(SignUpLayout,"Please Enter Username",Snackbar.LENGTH_LONG)
            snack.show()
            return false
        }
        if(SignUpEmailField.text.isNullOrEmpty()){
            SignUpEmailField.requestFocus()
            snack = Snackbar.make(SignUpLayout,"Please Enter Email",Snackbar.LENGTH_LONG)
            snack.show()
            return false
        }
        if(SignUpPasswordField.text.isNullOrEmpty()){
            SignUpPasswordField.requestFocus()
            snack = Snackbar.make(SignUpLayout,"Please Enter Password",Snackbar.LENGTH_LONG)
            snack.show()
            return false
        }
        result = true

        return result
    }

    /*Below are the Google sign in Method*/

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
    // [START declare_auth]
    private lateinit var auth: FirebaseAuth
    // [END declare_auth]
    private lateinit var googleSignInClient: GoogleSignInClient

    private fun googleSetup() {
        // Configure Google Sign In

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SignUpActivity.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d("Account", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(SignUpActivity.TAG, "Google sign in failed", e)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(SignUpActivity.TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        updateUI(user)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(SignUpActivity.TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: Any?) {
        if(user!=null){
            //Toast.makeText(this,"Login Successful", Toast.LENGTH_SHORT).show()
            val currentUser = fAuth.currentUser
            if (currentUser != null) {
                userID = currentUser.uid
            }
            // If the user is found the Database
            var documentReference = fStore.collection("users").document(userID)
            documentReference.get().addOnCompleteListener {
                if (it.result.exists()) {
                    val sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    // Initialize sharedPreferences to edit
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.putBoolean("isLogin", true).apply()
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finishAffinity()
                } else {
                    // If the user not found the Database
                    val userInfo = User(currentUser?.email.toString(), currentUser?.displayName)
                    documentReference.set(userInfo).addOnSuccessListener {
                        //Name,icon,Color
                        //food,transport,shopping
                        documentReference = fStore.collection("transaction").document(userID)
                        var initialData = HashMap<String, Int>()
                        initialData.put("Income", 0)
                        initialData.put("Expense", 0)
                        initialData.put("Transaction_counter", 0)
                        Log.d("initialData", initialData.toString())
                        documentReference.set(initialData).addOnFailureListener {
                            Log.d("Signup", it.message.toString())
                            //Toast.makeText(this,it.message.toString(), Toast.LENGTH_SHORT).show()
                        }.addOnSuccessListener {
                            Log.d("Signup", "Signup Complete")
                            documentReference = fStore.collection("accounts").document(userID)
                            var initialAccount = HashMap<String, Int>()
                            initialAccount.put("Total", 0)
                            documentReference.set(initialAccount).addOnFailureListener {
                                Log.d("Signup2", it.message.toString())
                                //Toast.makeText(this,it.message.toString(), Toast.LENGTH_SHORT).show()
                            }
                            documentReference = fStore.collection("budget").document(userID)
                            var initialbudget = HashMap<String, Int>()
                            initialbudget.put("Budget_counter", 0)
                            documentReference.set(initialbudget).addOnFailureListener {
                                Log.d("Signup3", it.message.toString())
                                //Toast.makeText(this,it.message.toString(), Toast.LENGTH_SHORT).show()
                            }

                            var categoryNameArray = arrayOf("Food", "Transport", "Shopping")
                            var categoryIconArray = arrayOf(452, 384, 258)
                            var categoryColorArray = arrayOf(-278483, -10044566, -12627531)
                            var defaultCategory: Category
                            for (i in 0..2) {
                                documentReference =
                                    fStore.collection("category/$userID/category_detail")
                                        .document(categoryNameArray[i])
                                defaultCategory = Category(categoryNameArray[i],
                                    categoryIconArray[i],
                                    categoryColorArray[i])
                                documentReference.set(defaultCategory)
                            }
                            val sharedPreferences =
                                PreferenceManager.getDefaultSharedPreferences(applicationContext)
                            // Initialize sharedPreferences to edit
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putBoolean("isLogin", true).apply()
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finishAffinity()
                        }
                    }
                }
            }

      /*    val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()*/

        }else{
            Toast.makeText(this,"Something Wrong", Toast.LENGTH_SHORT).show()
        }
    }
}

