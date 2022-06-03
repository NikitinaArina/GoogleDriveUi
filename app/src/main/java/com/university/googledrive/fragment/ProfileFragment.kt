package com.university.googledrive.fragment

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.beust.klaxon.Klaxon
import com.university.googledrive.activity.MainActivity
import com.university.googledrive.databinding.FragmentProfileBinding
import com.university.googledrive.dto.User
import com.university.googledrive.utils.Constants
import com.university.googledrive.utils.ImageUtils
import com.university.googledrive.utils.Urls
import com.university.googledrive.utils.ValidateInputsUtils
import com.university.googledrive.view.SharedViewModel
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ProfileFragment : Fragment() {
    private val httpClient = OkHttpClient()
    private lateinit var image: ImageView
    private lateinit var username: TextView
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private var imageByteArray: ByteArray? = null
    private var _binding: FragmentProfileBinding? = null
    private val model: SharedViewModel by activityViewModels()
    private lateinit var user: User

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        username = binding.editTextTextPersonName
        password = binding.editTextTextPassword
        confirmPassword = binding.confirmPassword
        image = binding.avatar

        val extras = activity?.intent?.extras

        val stringUser = extras?.get(Constants.USER).toString()

        user = Klaxon().parse(stringUser)!!

        model.selectUser(user)

        username.text = user.username
        getImage(user)

        binding.saveChanges.setOnClickListener {
            saveChanges()
        }

        binding.avatar.setOnClickListener {
            changeImage()
        }

        binding.exit.setOnClickListener {
            exit()
        }

        return binding.root
    }

    private fun getImage(user: User) {
        val urlBuilder = (Urls.BASE_URL + Urls.GET_IMAGE).toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter(Constants.USER_ID, user.id.toString())
        val url: String = urlBuilder.build().toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    imageByteArray = response.body?.bytes()
                    activity?.runOnUiThread {
                        image.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                imageByteArray,
                                0,
                                imageByteArray!!.size
                            )
                        )
                    }
                }
            }
        })
    }

    private fun exit() {
        startActivity(
            Intent(context, MainActivity::class.java)
        )
    }

    private fun changeImage() {
        val options = arrayOf<CharSequence>(Constants.CHOOSE_FROM_GALLERY, Constants.CANCEL)
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Choose a photo")
        builder.setItems(options) { dialog, item ->
            if (options[item] == Constants.CHOOSE_FROM_GALLERY) {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, 0)
            } else if (options[item] == Constants.CANCEL) {
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
            bitmapImage = ImageUtils.getResizedBitmap(bitmapImage, 400)
            image.setImageBitmap(bitmapImage)
            imageByteArray = ImageUtils.bitmapToArray(bitmapImage)
        }
    }

    private fun saveChanges() {
        var multipart: MultipartBody

        if (imageByteArray != null) {
            multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "profile.png", RequestBody.create(
                        "image/png".toMediaTypeOrNull(),
                        imageByteArray!!
                    )
                )
                .addFormDataPart(Constants.USER_ID, user.id.toString())
                .build()

            val savePhotoRequest = Request.Builder()
                .url(Urls.BASE_URL + Urls.SAVE_IMAGE)
                .post(multipart)
                .build()

            httpClient.newCall(savePhotoRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                }
            })
        }
        if ((password.text.isNotBlank() && confirmPassword.text.isBlank()) || (password.text.isBlank() && confirmPassword.text.isNotBlank()) || (password.text.isNotBlank() && confirmPassword.text.isNotBlank()))
            if (ValidateInputsUtils.validatePasswordInput(password, confirmPassword)) {
                user.password = password.text.toString()
                val userJson = Klaxon().toJsonString(user)

                val body =
                    userJson.toRequestBody(Constants.MEDIA_TYPE.toMediaType())

                val request = Request.Builder()
                    .url(Urls.BASE_URL + Urls.UPDATE_USER)
                    .put(body)
                    .build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.code == 200) {
                            user = Klaxon().parse(response.body!!.string())!!
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "User data is updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                                password.text.clear()
                                confirmPassword.text.clear()
                            }
                        }
                    }
                })
            }
    }
}