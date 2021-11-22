package com.example.jom_finance.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.jom_finance.R
import com.example.jom_finance.models.Category
import com.example.jom_finance.models.Transaction
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_voice.*
import kotlinx.android.synthetic.main.fragment_pie_fragment.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Pie_fragment : Fragment() {

    private lateinit var fAuth : FirebaseAuth
    private lateinit var fStore : FirebaseFirestore
    private lateinit var userID : String
    private lateinit var currentUser : FirebaseUser
    private lateinit var transactionArrayList : ArrayList<Transaction>
    private lateinit var categoryArrayList : ArrayList<Category>
    private lateinit var categoryHash : HashMap<String,Int>
    private var yValues = ArrayList<PieEntry>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_pie_fragment, container, false)
        val p = view.pieChart
        p.setUsePercentValues(true)
        p.description.isEnabled = false
        p.setExtraOffsets(5F,10f,5f,5f)
        p.dragDecelerationFrictionCoef = 0.95f
        p.isDrawHoleEnabled = true
        p.setHoleColor(R.color.white)
        p.transparentCircleRadius = 61f

        transactionArrayList = arrayListOf()
        categoryArrayList = arrayListOf()
        categoryHash = hashMapOf()
        setupDB()
        //Use Category to cal transaction number
        //If Category[i] == transaction[i].category, add counter

        fStore = FirebaseFirestore.getInstance()

        fStore.collection("transaction/$userID/Transaction_detail")
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
                    fStore.collection("category/$userID/Category_detail")
                        .addSnapshotListener(object : EventListener<QuerySnapshot> {
                            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                                if(error!=null){
                                    Log.e("FireStore Error",error.message.toString())
                                    return
                                }
                                for(dc : DocumentChange in value?.documentChanges!!){
                                    if(dc.type == DocumentChange.Type.ADDED){
                                        categoryArrayList.add(dc.document.toObject(Category::class.java))
                                        categoryHash[categoryArrayList.last().categoryName.toString()] = 0
                                    }
                                }
                                for(item in transactionArrayList){
                                    Log.d("Pie transaction 2",categoryHash.containsKey(item.transactionCategory).toString())
                                    if(categoryHash.containsKey(item.transactionCategory)){
                                        Log.d("Pie FOdd",categoryHash["Food"].toString())
                                        categoryHash[item.transactionCategory.toString()] = categoryHash[item.transactionCategory.toString()]!!.plus(1)
                                    }
                                }

                                for (item in categoryHash){
                                    Log.d("Pie float",item.value.toFloat().toString())
                                    Log.d("Pie key",item.key)
                                    yValues.add(PieEntry(1f,item.key))
                                }
                                createPie(yValues,p)
                            }
                        })
                }
            })

        // Inflate the layout for this fragment
        return view
    }

    private fun createPie(yValues : ArrayList<PieEntry>,p:PieChart) {
        var set1 = PieDataSet(yValues,"Category")
        set1.sliceSpace = 3f
        set1.selectionShift = 5f

        val arrayList = ColorTemplate.JOYFUL_COLORS.toCollection(ArrayList())

        set1.setColors(arrayList)

        var dataSets = PieData(set1)
        dataSets.setValueTextSize(10f)
        dataSets.setValueTextColor(Color.YELLOW)
        p.data = dataSets
        p.notifyDataSetChanged()
        p.invalidate()
    }


    private fun setupDB(){
        fAuth = FirebaseAuth.getInstance()
        val currentUser = fAuth.currentUser
        if (currentUser != null) {
            userID = currentUser.uid
        }

    }

}