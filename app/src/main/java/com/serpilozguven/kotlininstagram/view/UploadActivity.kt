package com.serpilozguven.kotlininstagram.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.serpilozguven.kotlininstagram.databinding.ActivityUploadBinding
import java.util.UUID

class UploadActivity : AppCompatActivity() {

    private lateinit var binding : ActivityUploadBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    var selectedPicture : Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        registerLauncher()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()



    }
    fun upload(view: View) {

        //universal unique id

        val uuid = UUID.randomUUID()

        val imageName = "$uuid.jpg"


        val reference = storage.reference
        val imageReference = reference.child("images").child(imageName)

        if (selectedPicture != null) {

            imageReference.putFile(selectedPicture!!).addOnSuccessListener{

            //download url -> firestore
                val uploadPictureReference = storage.reference.child("images").child(imageName)
                uploadPictureReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()

                    if(auth.currentUser != null) {
                        val postMap = hashMapOf<String, Any>()
                        postMap.put("downloadUrl",downloadUrl)
                        postMap.put("userEmail",auth.currentUser!!.email!!)
                        postMap.put("comment",binding.commentText.text.toString())
                        postMap.put("date",Timestamp.now())


                        firestore.collection("Posts").add(postMap).addOnSuccessListener {
                            finish()

                        }.addOnFailureListener {
                            Toast.makeText(this@UploadActivity,it.localizedMessage,Toast.LENGTH_LONG ).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this,it.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }

    }
    fun selectImage(view : View) {

        //Bu izni aldık mı diye kontrol ediyoruz.
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Eşit değil olduğu için izin isteyeceğiz.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //true dönerse göstermemiz gerekiyor.
                Snackbar.make(view,"Permission needed for gallery!",Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson") {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            } else {
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        } else {
            //izni aldık ise direkt intent yapıyorduk.
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //start activity for result
            activityResultLauncher.launch(intentToGallery)
        }

    }
    private fun registerLauncher() {
        //StartActivityForResult ile kullanıcını seçtiği görselin nerede kayıtlı oduğunu yani URI'ını alıyor.
        //Birde bize callback veriyor.
        //Callback(ActivitiyResultCallBack) ise seçildikten sonra ne olacak
        //sadece ActivityResulCallback yazarız yada süslü parantez açarız.ActivityResultu bize verecektir.
        //ActivityResult'ı it ile değilde değişken tanımlayarak lambda gösterimi ile (result ->) olarakta yaparız.
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intenFromResult = result.data
                if (intenFromResult != null) {
                    selectedPicture = intenFromResult.data
                    selectedPicture?.let {
                        binding.imageView.setImageURI(it)

                    }
                }
                permissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { result ->
                    if (result) {
                        //permission granted
                        val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        activityResultLauncher.launch(intentToGallery)

                    } else {
                        //permission denied
                        Toast.makeText(this@UploadActivity, "Permission needed!", Toast.LENGTH_LONG)
                            .show()

                    }
                }
            }
        }
    }
}