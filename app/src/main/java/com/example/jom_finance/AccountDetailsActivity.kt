package com.example.jom_finance

import android.content.Intent
import android.graphics.Color
import android.graphics.Color.WHITE
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maltaisn.icondialog.pack.IconDrawableLoader
import com.maltaisn.icondialog.pack.IconPackLoader
import com.maltaisn.iconpack.defaultpack.createDefaultIconPack
import kotlinx.android.synthetic.main.activity_account_details.*

class AccountDetailsActivity : AppCompatActivity() {

    private lateinit var fAuth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore
    private lateinit var userID: String

    private lateinit var accountName: String
    private var accountAmount: Double = 0.0
    private var accountIcon: Int = 278
    private var accountColor: Int = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_details)
        setupDatabase()

        setFavourite_btn.visibility = View.GONE

        //Icon loader
        val loader = IconPackLoader(this)
        val iconPack = createDefaultIconPack(loader)

        accountName = intent?.extras?.getString("account_name").toString()
        if(accountName != "null") {
            accountAmount = intent?.extras?.getDouble("account_amount")!!
            accountIcon = intent?.extras?.getInt("account_icon")!!
            accountColor = intent?.extras?.getInt("account_color")!!

            accountDetails_bg.setBackgroundColor(accountColor)

            val drawable = iconPack.getIconDrawable(accountIcon, IconDrawableLoader(this))
            accountDetailIcon_img.setImageDrawable(drawable)
            accountDetailIcon_img.setColorFilter(WHITE)

            accountDetailName_text.text = accountName
            accountDetailBalance_text.text = "RM " + String.format("%.2f", accountAmount)


        }

        editAccount_btn.setOnClickListener {
            val intent = Intent(this, AddNewAccountActivity::class.java)
            intent.putExtra("account_name", accountName)
            intent.putExtra("account_amount", accountAmount)
            intent.putExtra("account_icon", accountIcon)
            intent.putExtra("account_color", accountColor)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        deleteAccount_btn.setOnClickListener {
            openDeleteBottomSheetDialog()
        }

        setFavourite_btn.setOnClickListener{
            setFavourite()
        }

    }

    private fun openDeleteBottomSheetDialog(){
        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(R.layout.bottomsheet_delete)
        val yesBtn = bottomSheet.findViewById<Button>(R.id.removeYesbtn) as Button
        val noBtn = bottomSheet.findViewById<Button>(R.id.removeNobtn) as Button
        val title = bottomSheet.findViewById<TextView>(R.id.bottomsheetDeleteTitle_text) as TextView
        val description = bottomSheet.findViewById<TextView>(R.id.bottomsheetDeleteDesc_text) as TextView

        title.text = "Remove this account?"
        description.text = "Are you sure you want to remove this account?"

        yesBtn.setOnClickListener {
            fStore.collection("accounts/$userID/account_detail").document(accountName)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AccountsListActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Could not delete account : $it", Toast.LENGTH_SHORT).show()
                }
        }

        noBtn.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun setFavourite(){
        setFavourite_btn.setImageResource(R.drawable.ic_baseline_star_24)
    }

    private fun setupDatabase() {
        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()
        val currentUser = fAuth.currentUser
        if (currentUser != null)
            userID = currentUser.uid

    }
}