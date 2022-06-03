package com.university.googledrive.fragment

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.beust.klaxon.Klaxon
import com.university.googledrive.R
import com.university.googledrive.databinding.FragmentRegisterBinding
import com.university.googledrive.dto.User
import com.university.googledrive.utils.Constants.Companion.CANCEL
import com.university.googledrive.utils.Constants.Companion.CHOOSE_FROM_GALLERY
import com.university.googledrive.utils.Constants.Companion.MEDIA_TYPE
import com.university.googledrive.utils.Constants.Companion.PASSWORD
import com.university.googledrive.utils.Constants.Companion.USERNAME
import com.university.googledrive.utils.Constants.Companion.USER_ID
import com.university.googledrive.utils.ImageUtils
import com.university.googledrive.utils.ImageUtils.Companion.bitmapToArray
import com.university.googledrive.utils.ImageUtils.Companion.getResizedBitmap
import com.university.googledrive.utils.Urls
import com.university.googledrive.utils.Urls.Companion.BASE_URL
import com.university.googledrive.utils.Urls.Companion.CREATE_USER
import com.university.googledrive.utils.Urls.Companion.SAVE_IMAGE
import com.university.googledrive.utils.ValidateInputsUtils
import com.university.googledrive.utils.ValidateInputsUtils.Companion.validatePasswordInput
import com.university.googledrive.utils.ValidateInputsUtils.Companion.validateUsernameInput
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class RegisterFragment : Fragment() {
    private lateinit var image: ImageView
    private var imageByteArray: ByteArray? = null
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private val httpClient = OkHttpClient()
    private var _binding: FragmentRegisterBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        username = binding.editTextTextPersonName
        password = binding.editTextTextPassword
        confirmPassword = binding.confirmPassword
        image = binding.avatar

        binding.signup.setOnClickListener {
            signup()
        }

        binding.backToLogin.setOnClickListener {
            back()
        }

        binding.avatar.setOnClickListener {
            changeImage()
        }

        return binding.root
    }

    private fun back() {
        fragmentManager?.beginTransaction()
            ?.replace(R.id.frame, LoginFragment())
            ?.commit()
    }

    private fun signup() {

        if (validateUsernameInput(username) && validatePasswordInput(password, confirmPassword)) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            if (imageByteArray == null) {
                val drawable = image.drawable as BitmapDrawable
                drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                imageByteArray = byteArrayOutputStream.toByteArray()
            }

            val jsonObject = JSONObject()
            jsonObject.put(USERNAME, username.text.toString())
            jsonObject.put(PASSWORD, password.text.toString())

            val body =
                jsonObject.toString().toRequestBody(MEDIA_TYPE.toMediaType())

            var user: User
            var multipart: MultipartBody

            val request = Request.Builder()
                .url(BASE_URL + CREATE_USER)
                .post(body)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code == 200) {
                        user = Klaxon().parse(response.body!!.string())!!

                        multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                "file", "profile.png", RequestBody.create(
                                    "image/png".toMediaTypeOrNull(),
                                    imageByteArray!!
                                )
                            )
                            .addFormDataPart(USER_ID, user.id.toString())
                            .build()

                        val savePhotoRequest = Request.Builder()
                            .url(BASE_URL + SAVE_IMAGE)
                            .post(multipart)
                            .build()

                        httpClient.newCall(savePhotoRequest).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                e.printStackTrace()
                            }

                            override fun onResponse(call: Call, response: Response) {
                                if (response.code == 200) {
                                }
                            }
                        })
                        back()
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "User is already exist", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            })
        }
    }

    private fun changeImage() {
        val options = arrayOf<CharSequence>(CHOOSE_FROM_GALLERY, CANCEL)
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Choose a photo")
        builder.setItems(options) { dialog, item ->
            if (options[item] == CHOOSE_FROM_GALLERY) {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, 0)
            } else if (options[item] == CANCEL) {
                dialog.dismiss()
            }
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == 0) {
            val selectedImage: Uri? = data?.data
            image.setImageURI(selectedImage)
            var bitmapImage =
                MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedImage)
            bitmapImage = getResizedBitmap(bitmapImage, 400)
            image.setImageBitmap(bitmapImage)
            imageByteArray = bitmapToArray(bitmapImage)
        }
    }
}