package com.example.jom_finance.fragment

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jom_finance.ExpenseDetailActivity
import com.example.jom_finance.HomeActivity

import com.example.jom_finance.R
import com.example.jom_finance.databinding.TransactionListAdapter
import com.example.jom_finance.income.DetailIncome
import com.example.jom_finance.models.Category
import com.example.jom_finance.models.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.mlsdev.animatedrv.AnimatedRecyclerView
import kotlinx.android.synthetic.main.fragment_home_fragment.*
import kotlinx.android.synthetic.main.fragment_home_fragment.view.*
import java.io.File
import java.text.SimpleDateFormat


class Home_fragment : Fragment(),TransactionListAdapter.OnItemClickListener{

    private lateinit var fAuth : FirebaseAuth
    private lateinit var userID : String
    private lateinit var recyclerView: AnimatedRecyclerView
    private lateinit var transactionArrayList : ArrayList<Transaction>
    private lateinit var categoryArrayList : ArrayList<Category>
    private lateinit var categoryHash : HashMap<String,Category>
    private lateinit var transactionListAdapter: TransactionListAdapter
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setUpdb()
        readDB()
        val view : View = inflater.inflate(R.layout.fragment_home_fragment, container, false)
        db.collection("transaction").document(userID).get().addOnCompleteListener{
            val income_amount : Double = it.result["Income"].toString().toDouble()
            val expense_amount : Double = it.result["Expense"].toString().toDouble()
            view.home_income_amount.text = String.format("RM %.2f",income_amount)
            view.home_expenses_amount.text =String.format("RM %.2f",expense_amount)
        }
        db.collection("accounts").document(userID)
            .get()
            .addOnCompleteListener{ value ->
                val accountTotal : Double = value.result["Total"].toString().toDouble()
                val balance_amount : Double = accountTotal
                if(balance_amount<0.0){
                    view.home_balance.setTextColor(Color.RED)
                }
                view.home_balance.text = String.format("RM %.2f",balance_amount)
            }
        val storageReference = FirebaseStorage.getInstance()
            .getReference("user/$userID")

        val localFile = File.createTempFile("tempImage", "jpg")
        storageReference.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            view.profile_Icon.setImageBitmap(bitmap)
        }.addOnFailureListener{
            val storageReference = FirebaseStorage.getInstance()
                .getReference("user/sample-user.png")
            storageReference.getFile(localFile).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                view.profile_Icon.setImageBitmap(bitmap)
            }
        }

        view.seeAllBtn.setOnClickListener{
            requireActivity().run {
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("fragment_to_load","viewMore")
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        recyclerView = view.home_recyclerView
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.setHasFixedSize(true)
        transactionArrayList = arrayListOf()
        categoryArrayList = arrayListOf()
        categoryHash = hashMapOf()
        transactionListAdapter = TransactionListAdapter(transactionArrayList,categoryHash,this)
        recyclerView.adapter = transactionListAdapter
        EventChangeListener()
        // Inflate the layout for this fragment
        return view
    }

    @SuppressLint("SetTextI18n")
    private fun readDB() {
        db = FirebaseFirestore.getInstance()
    }

    private fun EventChangeListener() {
        db = FirebaseFirestore.getInstance()
        db.collection("transaction/$userID/Transaction_detail")
            .orderBy("Transaction_timestamp", Query.Direction.DESCENDING)
            .limit(8)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if(error!=null){
                        Log.e("FireStore Error",error.message.toString())
                        return
                    }
                    for(dc : DocumentChange in value?.documentChanges!!){
                        if(dc.type == DocumentChange.Type.ADDED){
                                transactionArrayList.add(dc.document.toObject(Transaction::class.java))
                        }
                    }
                }
            })

        db.collection("category/$userID/category_detail")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if(error!=null){
                        Log.e("FireStore Error",error.message.toString())
                        return
                    }
                    for(dc : DocumentChange in value?.documentChanges!!){
                        if(dc.type == DocumentChange.Type.ADDED){
                            categoryArrayList.add(dc.document.toObject(Category::class.java))
                            categoryHash[categoryArrayList.last().categoryName.toString()] = categoryArrayList.last()
                        }
                    }
                    transactionListAdapter.notifyDataSetChanged()
                    recyclerView.scheduleLayoutAnimation()
                }
            })

    }
    private fun setUpdb(){
        fAuth = FirebaseAuth.getInstance()
        val currentUser = fAuth.currentUser
        if (currentUser != null) {
            userID = currentUser.uid
        }
    }

    override fun onItemClick(position: Int) {
        val item = transactionArrayList[position]
        requireActivity().run {
            val type = item.transactionType
            val dateFormatName = SimpleDateFormat("dd MMMM yyyy")
            val dateFormatNum = SimpleDateFormat("dd-MM-yyyy")
            val timeFormat = SimpleDateFormat("hh:mm")
            val date = item.transactionTime?.toDate()
            if(type == "income"){
                val intent = Intent(this, DetailIncome::class.java)
                intent.putExtra("transactionName",item.transactionName)
                intent.putExtra("transactionName",item.transactionName)
                intent.putExtra("transactionAmount",item.transactionAmount)
                intent.putExtra("transactionCategory",item.transactionCategory)
                intent.putExtra("transactionAccount",item.transactionAccount)
                intent.putExtra("transactionDescription",item.transactionDescription)
                intent.putExtra("transactionAttachment",item.transactionAttachment)
                intent.putExtra("transactionDateName", dateFormatName.format(date))
                intent.putExtra("transactionDateNum", dateFormatNum.format(date))
                intent.putExtra("transactionTime", timeFormat.format(date))
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }else{
                val intent = Intent(this, ExpenseDetailActivity::class.java)
                intent.putExtra("transactionName",item.transactionName)
                intent.putExtra("transactionAmount",item.transactionAmount)
                intent.putExtra("transactionCategory",item.transactionCategory)
                intent.putExtra("transactionAccount",item.transactionAccount)
                intent.putExtra("transactionDescription",item.transactionDescription)
                intent.putExtra("transactionAttachment",item.transactionAttachment)
                intent.putExtra("transactionDateName", dateFormatName.format(date))
                intent.putExtra("transactionDateNum", dateFormatNum.format(date))
                intent.putExtra("transactionTime", timeFormat.format(date))
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }
}