package com.example.jom_finance

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import com.example.jom_finance.models.Transaction
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.android.synthetic.main.activity_add_new_expense.*
import kotlinx.android.synthetic.main.bottomsheet_attachment.*
import kotlinx.android.synthetic.main.bottomsheet_repeat.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

private lateinit var fAuth: FirebaseAuth
private lateinit var fStore: FirebaseFirestore
private lateinit var userID: String

private lateinit var transactionID : String
private var transactionNum by Delegates.notNull<Int>()

private const val TRANSACTION_TYPE = "expense"
private var transactionAmount: Double = 0.0
private lateinit var transactionCategory: String
private lateinit var transactionDescription: String
private lateinit var transactionAccount: String
private lateinit var transactionTimestamp : Timestamp
private  var transactionAttachment: Boolean = false

private const val IMAGE_REQUEST_CODE = 100
private const val CAMERA_REQUEST_CODE = 42
private const val DOCUMENT_REQUEST_CODE = 111

private lateinit var attachmentType: String
private const val FILE_NAME = "photo.jpg" //temporary file name
private lateinit var photoFile: File
private lateinit var imageUri : Uri
private lateinit var imageBitmap: Bitmap
private lateinit var documentUri : Uri

private var day = 0
private var month = 0
private var year = 0
private var hour = 0
private var minute = 0

private lateinit var savedDay : String
private lateinit var savedMonth : String
private lateinit var savedYear : String
private lateinit var savedHour : String
private lateinit var savedMinute : String
private lateinit var timestampString : String


class AddNewExpenseActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_expense)
        setupDataBase()

        //hide repeat section
        repeat_constraintLayout.visibility = View.GONE

        //hide attachment image and doc
        attachment_img.visibility = View.GONE
        attachmentDocument_txt.visibility = View.GONE

        transactionID = intent?.extras?.getString("transactionName").toString()
        if(transactionID != "null"){
            //UPDATE TRANSACTION
            transactionAmount = intent?.extras?.getDouble("transactionAmount")!!
            transactionCategory = intent?.extras?.getString("transactionCategory").toString()
            transactionAccount = intent?.extras?.getString("transactionAccount").toString()
            transactionDescription = intent?.extras?.getString("transactionDescription").toString()
            transactionAttachment = intent?.extras?.getBoolean("transactionAttachment")!!

            expenseAmount_edit.text = Editable.Factory.getInstance().newEditable(transactionAmount.toString())
            expenseCategory_ddl.editText?.text = Editable.Factory.getInstance().newEditable(transactionCategory)
            expenseAccount_ddl.editText?.text = Editable.Factory.getInstance().newEditable(transactionAccount)
            expenseDescription_outlinedTextField.editText?.text = Editable.Factory.getInstance().newEditable(transactionDescription)

            if(transactionAttachment){
                expenseAddAttachment_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_attachment_red_24, 0, 0, 0)
                expenseAddAttachment_btn.setTextColor(Color.parseColor("#FD3C4A"))
                expenseAddAttachment_btn.text = "Remove Attachment"
                attachment_img.visibility = View.VISIBLE
            }



        }else{
            //ADD TRANSACTION
            //current date & time
            val cal = Calendar.getInstance()
            day = cal.get(Calendar.DAY_OF_MONTH)
            month = cal.get(Calendar.MONTH)
            year = cal.get(Calendar.YEAR)
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)

            savedDay = String.format("%02d", day)
            savedMonth = String.format("%02d", month + 1)
            savedYear = year.toString()
            savedHour = String.format("%02d", hour)
            savedMinute = String.format("%02d", minute)
            expenseDate_edit.text = Editable.Factory.getInstance().newEditable( "$savedDay-$savedMonth-$savedYear" )
            expenseTime_edit.text = Editable.Factory.getInstance().newEditable("$savedHour:$savedMinute")
            timestampString= "$savedDay-$savedMonth-$savedYear $savedHour:$savedMinute"

            expenseConfirm_btn.setOnClickListener {
                // TODO: 5/11/2021 make sure all inputs are not NULL
                addExpenseToDatabase()
                updateBudget()
                updateAccount()
            }

        }

        //category drop down list
        val cat : MutableList<String> = mutableListOf()
        fStore.collection("category/$userID/category_detail")
            .get()
            .addOnSuccessListener {
                for (document in it.documents){
                    cat.add(document.getString("category_name")!!)
                }
            }
        val catAdapter = ArrayAdapter(this, R.layout.item_dropdown, cat)
        expenseCategory_autoCompleteTextView.setAdapter(catAdapter)

        //accounts drop down list
        val acc : MutableList<String> = mutableListOf()
        fStore.collection("accounts/$userID/account_detail")
            .get()
            .addOnSuccessListener {
                for (document in it.documents){
                    acc.add(document.getString("account_name")!!)
                }
            }
        val accAdapter = ArrayAdapter(this, R.layout.item_dropdown, acc)
        expenseAccount_autoCompleteTextView.setAdapter(accAdapter)

        //set attachment if come from receipt scanner
        val imagePath = intent?.extras?.getString("image_path").toString()
        if (imagePath != "null"){
            expenseAddAttachment_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_attachment_red_24, 0, 0, 0)
            expenseAddAttachment_btn.setTextColor(Color.parseColor("#FD3C4A"))
            expenseAddAttachment_btn.text = "Remove Attachment"
            attachment_img.visibility = View.VISIBLE

            Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show()

            val image = BitmapFactory.decodeFile(imagePath)
            imageBitmap = image
            attachment_img.setImageBitmap(image)
            transactionAttachment = true
            attachmentType = "camera"

            //EXTRACT IMPORTANT TEXT
            //create instance of text recognizer
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            //create InputImage object from Bitmap
            val inputImage = InputImage.fromBitmap(image, 90)

            var elementArr :ArrayList<String> = arrayListOf()
            //process the image
            //store all elements in an array
            val result = recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->

                    //loop through all elements and find "total"
                    var foundTotal = false
                    var temp : String = ""
                    for (block in visionText.textBlocks){
                        //val blockText = block.text
                        for (line in block.lines){
                            //val lineText = line.text
                            for (element in line.elements){
                                val elementText = element.text
                                //elementArr.add(element.text)
                                if (elementText.equals("total", true))
                                    foundTotal = true

                                if(foundTotal){
                                    try {
                                        if (elementText.contains(".")){
                                            val total = elementText.toDouble()
                                            if (total > 1.0)
                                                expenseAmount_edit.text = Editable.Factory.getInstance().newEditable(total.toString())
                                        }
                                    }catch (e : Exception){

                                    }
                                }
                                temp += "$elementText | "
                            }
                        }
                    }

                    val b =visionText.textBlocks
                    val l = b[0].lines
                    expenseDescription_outlinedTextField.editText?.text = Editable.Factory.getInstance().newEditable(l[0].text)

                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                }
        }

        expenseDate_edit.setOnClickListener{
            DatePickerDialog(this,  this, year, month, day).show()
        }

        expenseTime_edit.setOnClickListener {
            TimePickerDialog(this, this, hour, minute, true).show()
        }

        expenseAddAttachment_btn.setOnClickListener {
            if(attachment_img.visibility == View.GONE && attachmentDocument_txt.visibility == View.GONE){
                openAttachmentBottomSheetDialog()
            }
            else{
                attachmentDocument_txt.visibility = View.GONE
                attachment_img.visibility = View.GONE
                expenseAddAttachment_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_attachment_24, 0, 0, 0)
                expenseAddAttachment_btn.setTextColor(Color.parseColor("#91919F"))
                expenseAddAttachment_btn.text = "Add Attachment"
                transactionAttachment = false
            }
        }

        expenseRepeatEdit_btn.setOnClickListener {
            openRepeatBottomSheetDialog()
        }

        expenseRepeat_switch.setOnCheckedChangeListener{ _, isChecked ->
            if (isChecked)
                repeat_constraintLayout.visibility = View.VISIBLE
            else
                repeat_constraintLayout.visibility = View.GONE
        }

        attachmentDocument_txt.setOnClickListener {
            // TODO: 6/11/2021 display pdf when clicked 
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        savedDay = String.format("%02d", dayOfMonth)
        savedMonth =String.format("%02d", month + 1)
        savedYear = year.toString()

        expenseDate_edit.text = Editable.Factory.getInstance().newEditable("$savedDay-$savedMonth-$savedYear")
    }

    override fun onTimeSet(view: TimePicker?, hour: Int, minute: Int) {
        savedHour = String.format("%02d", hour)
        savedMinute = String.format("%02d", minute)

        expenseTime_edit.text = Editable.Factory.getInstance().newEditable("$savedHour:$savedMinute")
    }

    private fun addExpenseToDatabase(){
        transactionAmount = expenseAmount_edit.text.toString().toDouble()
        transactionCategory = expenseCategory_ddl.editText?.text.toString()
        transactionDescription = expenseDescription_outlinedTextField.editText?.text.toString()
        transactionAccount = expenseAccount_ddl.editText?.text.toString()

        timestampString= "$savedDay-$savedMonth-$savedYear $savedHour:$savedMinute"
        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm")
        val date : Date = sdf.parse(timestampString)

        transactionTimestamp = Timestamp(date)


        try{
            fStore.collection("transaction/$userID/Transaction_detail")
                .get()
                .addOnCompleteListener {
                    if(it.isSuccessful){

                        transactionNum = it.result.size().inc()

                        val documentReference = fStore.collection("transaction/$userID/Transaction_detail").document("transaction$transactionNum")

                        //transaction details
                        val transactionDetails = Transaction("transaction$transactionNum",
                            transactionAmount, transactionAccount, transactionAttachment,
                            transactionCategory, transactionDescription, TRANSACTION_TYPE,
                            transactionTimestamp)

                        //Insert to database
                        documentReference.set(transactionDetails).addOnCompleteListener {
                            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
                        }

                    }

                    //store attachment if necessary
                    if(transactionAttachment){

                        val storageReference = FirebaseStorage.getInstance().getReference("transaction_images/$userID/transaction$transactionNum")
                        lateinit var uploadTask: UploadTask

                        when (attachmentType){
                            "camera" -> {
                                val baos = ByteArrayOutputStream()
                                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                val data = baos.toByteArray()
                                uploadTask = storageReference.putBytes(data)
                            }
                            "image" -> uploadTask = storageReference.putFile(imageUri)
                            "document" -> uploadTask = storageReference.putFile(documentUri)
                        }

                        uploadTask
                            .addOnSuccessListener {
                                Toast.makeText(this, "Successfully uploaded", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }catch (e: Exception) {
            Log.w(ContentValues.TAG, "Error adding document", e)
        }
    }

    private fun updateBudget(){
        val monthHashMap : HashMap<Int, String> = hashMapOf(
            1 to "January",
            2 to "February",
            3 to "March",
            4 to "April",
            5 to "May",
            6 to "June",
            7 to "July",
            8 to "August",
            9 to "September",
            10 to "October",
            11 to "November",
            12 to "December"
        )

        val budgetMonth = savedMonth.toInt()
        val budgetDate = monthHashMap[budgetMonth]  + " $savedYear"
        val budgetRef = fStore.collection("budget/$userID/budget_detail")

        budgetRef.whereEqualTo("budget_date", budgetDate).whereEqualTo("budget_category", transactionCategory)
            .get()
            .addOnSuccessListener {
                for (document in it.documents)
                if(document.exists()){
                    val budgetID = document.getString("budget_id")!!
                    var budgetSpent = document.data?.getValue("budget_spent").toString().toDouble()
                    budgetSpent += transactionAmount
                    budgetRef.document(budgetID)
                        .update("budget_spent", budgetSpent)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Updated budget", Toast.LENGTH_SHORT).show()
                        }
                }
                else
                    Toast.makeText(this, "There is no budget for this category", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.w(ContentValues.TAG, "Error updating budget", it)
            }
    }

    private fun updateAccount(){
        val accountRef = fStore.collection("accounts/$userID/account_detail").document(transactionAccount)

        accountRef
            .get()
            .addOnSuccessListener { document ->
                if(document.exists()){
                    var accountAmount = document.data?.getValue("account_amount").toString().toDouble()
                    accountAmount -= transactionAmount
                    accountRef
                        .update("account_amount", accountAmount)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Updated account", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Log.w(ContentValues.TAG, "Error updating account amount", it)
            }
    }

    private fun setupDataBase() {
        fAuth = FirebaseAuth.getInstance()
        fStore = FirebaseFirestore.getInstance()
        val currentUser = fAuth.currentUser
        if (currentUser != null) {
            userID = currentUser.uid
        }
    }

    //After get attachment
    override fun onActivityResult(requestCode:Int, resultCode: Int, data:Intent?){

        expenseAddAttachment_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_attachment_red_24, 0, 0, 0)
        expenseAddAttachment_btn.setTextColor(Color.parseColor("#FD3C4A"))
        expenseAddAttachment_btn.text = "Remove Attachment"

        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            attachment_img.visibility = View.VISIBLE
            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
            imageBitmap = takenImage

            // TODO: 5/11/2021 Image orientation
            Toast.makeText(this, photoFile.absolutePath, Toast.LENGTH_SHORT).show()
            attachment_img.setImageBitmap(takenImage)

            transactionAttachment = true
            attachmentType = "camera"

        }
        else if(requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            attachment_img.visibility = View.VISIBLE
            imageUri = data?.data!!
            attachment_img.setImageURI(imageUri)

            transactionAttachment = true
            attachmentType = "image"
        }
        else if(requestCode == DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            attachmentDocument_txt.visibility = View.VISIBLE
            documentUri = data?.data!!
            attachmentDocument_txt.text = getFileName(documentUri)

            transactionAttachment = true
            attachmentType = "document"

        }
        else{
            attachmentDocument_txt.visibility = View.GONE
            attachment_img.visibility = View.GONE
            expenseAddAttachment_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_attachment_24, 0, 0, 0)
            expenseAddAttachment_btn.setTextColor(Color.parseColor("#91919F"))
            expenseAddAttachment_btn.text = "Add Attachment"
        }

    }

    private fun openAttachmentBottomSheetDialog(){
        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(R.layout.bottomsheet_attachment)

        val camera = bottomSheet.findViewById<Button>(R.id.attachementCamera_btn) as Button
        val image = bottomSheet.findViewById<Button>(R.id.attachementImage_btn) as Button
        val document = bottomSheet.findViewById<Button>(R.id.attachementDocument_btn) as Button

        camera.setOnClickListener {

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            photoFile = getPhotoFile(FILE_NAME)

            val fileProvider = FileProvider.getUriForFile(this,"com.example.jom_finance.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)


            if(takePictureIntent.resolveActivity(this.packageManager) != null)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            else
                Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show()

            bottomSheet.dismiss()
        }


        image.setOnClickListener {
            //open image picker
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.type = "image/*"
            startActivityForResult(pickImageIntent, IMAGE_REQUEST_CODE)

            bottomSheet.dismiss()
        }

        document.setOnClickListener {
            //open document picker
            val pickDocumentIntent = Intent(Intent.ACTION_GET_CONTENT)
            pickDocumentIntent.type = "application/pdf"
            startActivityForResult(Intent.createChooser(pickDocumentIntent, "Select a document"), DOCUMENT_REQUEST_CODE)

            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun getPhotoFile(fileName: String): File{
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, "jpg", storageDirectory)
    }

    //get PDF file name
    private fun Context.getFileName(uri: Uri): String? = when(uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun Context.getContentFileName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()

    //REPEAT TRANSACTION
    private fun openRepeatBottomSheetDialog(){
        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(R.layout.bottomsheet_repeat)

        val confirmBtn = bottomSheet.findViewById<Button>(R.id.repeatConfirm_btn) as Button
        val freqRadioGroup = bottomSheet.findViewById<RadioGroup>(R.id.repeatFrequency_radioBtn) as RadioGroup
        val datePicker = bottomSheet.findViewById<DatePicker>(R.id.repeatEnd_datePicker) as DatePicker


        confirmBtn.setOnClickListener {

            val selectedBtnID = freqRadioGroup.checkedRadioButtonId
            val selectedFreq = bottomSheet.findViewById<RadioButton>(selectedBtnID)?.text
            val date: String = "${datePicker.dayOfMonth} / ${datePicker.month} / ${datePicker.year}"

            expenseFrequency_txt.text = selectedFreq
            expenseRepeatEnd_txt.text = date

            bottomSheet.dismiss()

        }

        bottomSheet.show()
    }
}