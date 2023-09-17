package com.example.kidsdrawing

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.nvt.color.ColorPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {


    var slider:Slider? = null
    var drawingView:DrawingView? = null
    var colorOnColorDialog:Int? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            run {
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val img:ImageView = findViewById(R.id.BackgroundImage)
                    img.setImageURI(result.data?.data)
                }
            }
        }

    val imagepermission:ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            run {
                permissions.entries.forEach {
                    if (it.value) {

                        val intent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(intent)
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        colorOnColorDialog = Color.BLACK

        // Taking drawing view so, use it function to change brush size and color
        drawingView = findViewById(R.id.drawingView)

        // Implementing Brush Size Button
        var brushSizeButton:AppCompatButton = findViewById(R.id.Bursh_Size_Button)
        brushSizeButton.setOnClickListener{
            BrushDailog()
        }

        // Color Changing Button
        var ColorChangerBut:AppCompatButton = findViewById(R.id.ColorChangerbut)
        // Color Picker Code to take custom color and change color
        ColorChangerBut.setOnClickListener {
            val colorPicker = ColorPickerDialog(
                this,
                colorOnColorDialog!!, // color init
                true, // true is show alpha
                object : ColorPickerDialog.OnColorPickerListener {
                    override fun onCancel(dialog: ColorPickerDialog?) {
                        // handle click button Cancel
                    }

                    override fun onOk(dialog: ColorPickerDialog?, colorPicker: Int) {
                        drawingView!!.setColorChanger(colorPicker)
                        colorOnColorDialog = colorPicker
                    }
                })
            colorPicker.show()
        }

        val ImageButton: AppCompatButton = findViewById(R.id.ImageImportingButton)

        ImageButton.setOnClickListener {
            requestPermission()
        }

        val UndoButton:AppCompatButton = findViewById(R.id.UndoButton)
        UndoButton.setOnClickListener {
            drawingView?.UndoLastEntry()
        }

        val saveButton:AppCompatButton = findViewById(R.id.SaveButton)

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                val flDrawingView:FrameLayout = findViewById(R.id.framelayout)
                saveBitmapFile(returnBitmap(flDrawingView))
            }
        }

    }

    private fun requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            showNotpermissionDailog("Storage Not focusable", "You denied to give the permission for storage and this app needs permission to import images")
        }
        else{
            imagepermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }


    private fun showNotpermissionDailog(s: String, s1: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(s).setMessage(s1)
            .setPositiveButton("Cancel"){
                    dialog, _-> dialog.dismiss()
            }
        builder.show();
    }

    // Custom Dialog for Brush Size
    fun BrushDailog(){
        val dailog = Dialog(this)
        dailog.setContentView(R.layout.activity_brush_dailog)
        dailog.setTitle("Change Brush Size")
        slider = dailog.findViewById(R.id.slider)

        slider!!.addOnChangeListener{slider , value, fromuser ->
            val insideDone = dailog.findViewById<AppCompatButton>(R.id.Brush_Size_Done)
            insideDone.setOnClickListener {
                this.drawingView!!.setbrushsize(value * 100)
                dailog.dismiss()
            }
        }
        dailog.show()
    }

    fun returnBitmap(view:View):Bitmap{
        val returnBit = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnBit)
        val bigdraw = view.background
        if(bigdraw!= null){
            bigdraw.draw(canvas)
        }
        else{
            canvas.drawColor(Color.BLACK)
        }
        view.draw(canvas)
        return  returnBit
    }

    suspend fun saveBitmapFile(bitmap:Bitmap?):String{
        var result = ""
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.activity_progress_bar)
        dialog.show()
        withContext(Dispatchers.IO){
            if(bitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90,bytes)

                    val fName = File(externalCacheDir, "MyPaint_" + (System.currentTimeMillis()/1000) + ".jpg")
                    val fo = FileOutputStream(fName)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    val uri = FileProvider.getUriForFile(this@MainActivity, "com.example.kidsdrawing.fileprovider", fName)
                    result = fName.absolutePath
                    runOnUiThread {
                        dialog.dismiss()

                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "File Saved succelfy in + $result",Toast.LENGTH_SHORT).show()
                            setUpEnablingFeatures(uri)

                        }
                        else{

                            Toast.makeText(this@MainActivity,
                                "File not saved",Toast.LENGTH_SHORT).show()

                        }

                    }
                }
                catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun setUpEnablingFeatures(uri:Uri){

        try {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.type = "image/jpeg"
            startActivity(Intent.createChooser(intent, "Share image via"))
        } catch (e: Exception) {
            Log.e(TAG, "Error in setUpEnablingFeatures: ${e.message}")

        }
    }

}