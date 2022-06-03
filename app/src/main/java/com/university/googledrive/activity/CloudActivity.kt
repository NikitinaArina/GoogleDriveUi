package com.university.googledrive.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.university.googledrive.R
import com.university.googledrive.dto.Folder
import com.university.googledrive.dto.Photo
import com.university.googledrive.dto.User
import com.university.googledrive.utils.Constants
import com.university.googledrive.utils.ImageUtils
import com.university.googledrive.utils.Urls
import com.university.googledrive.view.CloudAdapter
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class CloudActivity : AppCompatActivity() {
    private lateinit var cloud: RecyclerView
    private lateinit var cloudAdapter: CloudAdapter
    private var folderName: String? = null
    private val folderList = ArrayList<Folder>()
    private var parentFolder: Folder? = null
    private val httpClient = OkHttpClient()
    private lateinit var user: User
    private lateinit var menuButton: FloatingActionButton
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud)

        parentFolder = Klaxon().parse<Folder>(intent.extras?.get("folder").toString())

        user = parentFolder?.user!!
        cloud = findViewById(R.id.cloudRecycler)
        menuButton = findViewById(R.id.menuButton)
        backButton = findViewById(R.id.backButton)

        menuButton.setOnClickListener {
            registerForContextMenu(menuButton)
            menuButton.showContextMenu()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        getFiles()
        cloudAdapter = CloudAdapter(folderList)
        cloud.adapter = cloudAdapter
        cloud.layoutManager = LinearLayoutManager(applicationContext)
    }

    private fun getFiles() {
        val urlBuilder = (Urls.BASE_URL + Urls.GET_FILES).toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter(Constants.USER_ID, user.id.toString())
        if (parentFolder != null) {
            urlBuilder.addQueryParameter(Constants.PARENT_ID, parentFolder!!.id.toString())
        } else urlBuilder.addQueryParameter(Constants.PARENT_ID, "0")

        val url: String = urlBuilder.build().toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    val string = response.body?.string()
                    folderList.addAll(Klaxon().parseArray<Folder>(string.toString()) as ArrayList<Folder>)
                    runOnUiThread {
                        cloudAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        if (v.id == R.id.menuButton) {
            menu.add(0, v.id, 0, "Create folder")
            menu.add(0, v.id, 0, "Upload")
            menu.add(0, v.id, 0, "Use camera")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.title) {
            "Create folder" -> {
                enterFolderName()
                return true
            }
            "Upload" -> {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, 3)
                return true
            }
            "Use camera" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, 100)
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var imageByteArray: ByteArray
        if (data?.data == null) {
            val bitmapImage = data?.extras?.get("data") as Bitmap
            imageByteArray = ImageUtils.bitmapToArray(bitmapImage)
        } else {
            val selectedImage: Uri? = data.data
            var bitmapImage =
                MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            bitmapImage = ImageUtils.getResizedBitmap(bitmapImage, 400)
            imageByteArray = ImageUtils.bitmapToArray(bitmapImage)
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "photo", ".png", RequestBody.create(
                    "image/png".toMediaTypeOrNull(),
                    imageByteArray
                )
            )
            .build()

        val savePhotoRequest = Request.Builder()
            .url(Urls.BASE_URL + Urls.SAVE_PHOTO)
            .post(multipart)
            .build()

        httpClient.newCall(savePhotoRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    val response = Photo(response.body?.string().toString().toLong())

                    folderName = "Photo " + user.username + "" + response.id

                    val folder = if (parentFolder!!.id != 0L) {
                        Folder(user, folderName, parentFolder, response)
                    } else Folder(user, folderName, response)

                    val jsonObject = Klaxon().toJsonString(folder)

                    val body =
                        jsonObject.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(Urls.BASE_URL + Urls.CREATE_FOLDER)
                        .post(body)
                        .build()

                    httpClient.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            TODO("Not yet implemented")
                        }

                        @SuppressLint("NotifyDataSetChanged")
                        override fun onResponse(call: Call, response: Response) {
                            if (response.code == 200) {
                                val folder =
                                    Klaxon().parse<Folder>(response.body?.string().toString())

                                if (folder != null) {
                                    folderList.add(folder)
                                    runOnUiThread {
                                        cloudAdapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                    })
                }
            }
        })

    }

    private fun enterFolderName() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Enter a folder name")

        val viewInflated: View = LayoutInflater.from(this)
            .inflate(R.layout.text_input_name, findViewById(R.id.content), false)

        val input = viewInflated.findViewById(R.id.folderName) as EditText

        builder.setView(viewInflated)

        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, _ ->
            dialog.dismiss()
            folderName = input.text.toString()
            createFolder()
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun createFolder() {
        val folder = if (parentFolder != null) {
            Folder(user, folderName.toString(), parentFolder!!)
        } else Folder(user, folderName.toString())

        val jsonObject = Klaxon().toJsonString(folder)

        val body =
            jsonObject.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(Urls.BASE_URL + Urls.CREATE_FOLDER)
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    val folder = Klaxon().parse<Folder>(response.body?.string().toString())

                    if (folder != null) {
                        folderList.add(folder)
                        runOnUiThread {
                            cloudAdapter.notifyDataSetChanged()
                        }
                    }
                } else Toast.makeText(applicationContext, "Folder is already exist", Toast.LENGTH_SHORT).show()
            }
        })
    }
}