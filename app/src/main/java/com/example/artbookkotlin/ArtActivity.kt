package com.example.artbookkotlin

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.artbookkotlin.databinding.ActivityArtBinding
import com.example.artbookkotlin.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import kotlin.math.max

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    var selectedBitmap : Bitmap? = null
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")

        //art'in yeni ve ya kohne olmasini bilmesi
        if (info.equals("new")) {

            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.button.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.web)

        } else {
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id", 1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }

            cursor.close()
        }


    }


    fun save(view : View) {
/*Database islerini burada edirik*/

        //edit txtlerdeki inputlari almaq
        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            //create db
            try {

                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)
                statement.execute()

            } catch (e : Exception) {
                e.printStackTrace()
            }
        }



        val intent = Intent(this@ArtActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }


    private fun makeSmallerBitmap(image : Bitmap, maximumSize : Int) : Bitmap {

        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1) {
            //landscape image = yatay gorsel
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        } else {
            //portrait image = dikey gorsel
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()

        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }


    fun selectImage(view : View) {

        /*  ICAZE ISTEME  */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //android 33 ve sonrasi ucun icaze alma kontrolu -> READ_MEDIA_IMAGES

            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                //icaze almaq mentiqini gosterirsen, yeni icazenin ne ucun lazim oldugunu SnackBar vasitesile   //rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {

                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", View.OnClickListener {
                        //snackbar ile icaze isteyirik tezeden
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()

                } else {
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }

            } else {

                //icaze verilibse
                //intent to gallery
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        } else {
            //android 32 ve alti -> READ_EXTERNAL_STORAGE

            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                //icaze almaq mentiqini gosterirsen, yeni icazenin ne ucun lazim oldugunu SnackBar vasitesile   //rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", View.OnClickListener {
                        //snackbar ile icaze isteyirik tezeden
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()

                } else {
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            } else {

                //icaze verilibse
                //intent to gallery
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }




    private fun registerLauncher() {

        //bu resultLauncherlarin meqsedi odur ki harasa gedib oradan neylese qayidirlar. Meselen galleryden sekille
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data

                if (intentFromResult != null) {
                    val imageData = intentFromResult.data
                    //binding.imageView.setImageURI(imageData)

                    if (imageData != null) {

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(this@ArtActivity.contentResolver, imageData!!)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)

                            } else {
                                selectedBitmap = MediaStore.Images.Media.getBitmap(this@ArtActivity.contentResolver, imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }

                        } catch (e : Exception) {
                            e.printStackTrace()
                        }
                    }


                }
            }
        }

        //permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->

            if (result) {
                //icaze verildi
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            } else {
                //icaze verilmedi
                Toast.makeText(this@ArtActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }


        }
    }

}


