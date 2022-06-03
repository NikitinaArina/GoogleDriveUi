package com.university.googledrive.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.university.googledrive.R
import com.university.googledrive.databinding.FragmentCloudBinding
import com.university.googledrive.dto.Folder
import com.university.googledrive.dto.Photo
import com.university.googledrive.dto.User
import com.university.googledrive.utils.Constants.Companion.FOLDER_ID
import com.university.googledrive.utils.Constants.Companion.PARENT_ID
import com.university.googledrive.utils.Constants.Companion.USER_ID
import com.university.googledrive.utils.ImageUtils
import com.university.googledrive.utils.Urls.Companion.BASE_URL
import com.university.googledrive.utils.Urls.Companion.CREATE_FOLDER
import com.university.googledrive.utils.Urls.Companion.DELETE
import com.university.googledrive.utils.Urls.Companion.GET_FILES
import com.university.googledrive.utils.Urls.Companion.SAVE_PHOTO
import com.university.googledrive.utils.Urls.Companion.UPDATE
import com.university.googledrive.view.CloudAdapter
import com.university.googledrive.view.SharedViewModel
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit


class CloudFragment : Fragment() {
    private var _binding: FragmentCloudBinding? = null
    private val binding get() = _binding!!
    private val httpClient = OkHttpClient()
    private val model: SharedViewModel by activityViewModels()
    private lateinit var cloud: RecyclerView
    private lateinit var cloudAdapter: CloudAdapter
    private var folderName: String? = null
    private val folderList = ArrayList<Folder>()
    private lateinit var user: User
    private var parentFolder: Folder = Folder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloudBinding.inflate(inflater, container, false)

        val bundle = this.arguments
        if (bundle != null) {
            parentFolder = Klaxon().parse<Folder>(bundle.get("folder").toString())!!
        }

        user = model.selectedUser.value!!
        cloud = binding.cloudRecycler

        initRecyclerView()
        getFiles()

        registerForContextMenu(cloud)

        binding.menuButton.setOnClickListener {
            registerForContextMenu(binding.menuButton)
            binding.menuButton.showContextMenu()
        }

        httpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return binding.root
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
            val selectedImage: Uri? = data?.data
            var bitmapImage =
                MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedImage)
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
            .url(BASE_URL + SAVE_PHOTO)
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

                    val folder = if (parentFolder.id != 0L) {
                        Folder(user, folderName, parentFolder, response)
                    } else Folder(user, folderName, response)

                    val jsonObject = Klaxon().toJsonString(folder)

                    val body =
                        jsonObject.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(BASE_URL + CREATE_FOLDER)
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
                                    activity?.runOnUiThread {
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

    private fun getFiles() {
        val urlBuilder = (BASE_URL + GET_FILES).toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter(USER_ID, user.id.toString())
        urlBuilder.addQueryParameter(PARENT_ID, parentFolder.id.toString())

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
                    activity?.runOnUiThread {
                        cloudAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun initRecyclerView() {
        cloudAdapter = CloudAdapter(folderList)
        cloud.adapter = cloudAdapter
        cloud.layoutManager = LinearLayoutManager(context)
    }

    private fun enterFolderName() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Enter a folder name")

        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.text_input_name, view as ViewGroup?, false)

        val input = viewInflated.findViewById(R.id.folderName) as EditText

        builder.setView(viewInflated)

        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, _ ->
            dialog.dismiss()
            folderName = input.text.toString()
            createFolder(parentFolder, folderName!!)
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun createFolder(parentFolder: Folder, folderName: String) {
        val folder = if (parentFolder.id != 0L) {
            Folder(user, folderName, parentFolder)
        } else Folder(user, folderName)

        val jsonObject = Klaxon().toJsonString(folder)

        val body =
            jsonObject.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL + CREATE_FOLDER)
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
                        activity?.runOnUiThread {
                            cloudAdapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Folder is already exist", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        })
    }

    fun renameFolder(folder: Folder, folderName: String) {
        folder.folderName = folderName
        val jsonObject = Klaxon().toJsonString(folder)

        val body =
            jsonObject.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL + UPDATE)
            .patch(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    val folder = Klaxon().parse<Folder>(response.body?.string().toString())

                    if (folder != null) {
                        folderList.removeIf { x ->
                            x.id == folder.id
                        }
                        folderList.add(folder)
                        activity?.runOnUiThread {
                            cloudAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })
    }

    fun deleteFolder(folder: Folder) {
        val urlBuilder = (BASE_URL + DELETE).toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter(FOLDER_ID, folder.id.toString())

        val url: String = urlBuilder.build().toString()

        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    activity?.runOnUiThread {
                        cloudAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }
}