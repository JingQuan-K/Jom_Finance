package com.example.jom_finance

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ArrayAdapter
import com.example.jom_finance.intro.IntroActivity1
import kotlinx.android.synthetic.main.activity_add_new_income.*


class MainActivity :AppCompatActivity(){
    private var x1 = 0f
    private var x2 = 0f
    val MIN_DISTANCE = 150

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_income)
        ArrayAdapter.createFromResource(
            this,
            R.array.Category,
            R.layout.spinner_list
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(R.layout.spinner_list)
            // Apply the adapter to the spinner
            spinnerCategory.adapter = adapter
        }
        AddnewBtn.setOnClickListener{
            val resetView = LayoutInflater.from(this).inflate(R.layout.activity_popup, null)
            val resetViewBuilder = AlertDialog.Builder(this,R.style.CustomAlertDialog).setView(resetView)
            //show dialog
            val displayDialog = resetViewBuilder.show()
        }

    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> x1 = event.x
            MotionEvent.ACTION_UP -> {
                x2 = event.x
                val deltaX: Float = x2 - x1
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if(x2 > x1){ //left to right (going to left)
                        //..
                    }
                    else if(x1>x2){ //right to left (going to right)
                        val intent = Intent(this, IntroActivity1::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                } else {
                    // consider as something else - a screen tap for example
                }
            }
        }
        return super.onTouchEvent(event)
    }


}