package com.university.googledrive.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.university.googledrive.R
import com.university.googledrive.activity.CloudActivity
import com.university.googledrive.dto.Folder
import com.university.googledrive.fragment.CloudFragment
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class CloudAdapter(private val cloudList: ArrayList<Folder>) :
    RecyclerView.Adapter<CloudAdapter.ViewHolder>() {

    var context: Context? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    private var data: Folder? = null
    val cloudFragment = CloudFragment()
    private var v: View? = null
    private var imageByteArray: ByteArray? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        v = LayoutInflater.from(parent.context)
            .inflate(R.layout.items_cloud, parent, false)
        context = parent.context
        adapter = this

        return ViewHolder(v!!)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DetachAndAttachSameFragment")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        data = cloudList[position]
        holder.name.text = data!!.folderName
        holder.date.text = data!!.date

        if (data!!.photo != null) {
            holder.info.text = "Image"
        } else {
            holder.info.text = "Folder"
        }

        holder.layout.setOnClickListener {
            data = cloudList[position]
            if (data!!.photo?.id == null) {
                val folder = Klaxon().toJsonString(data)

                val intent = Intent(context, CloudActivity::class.java)
                intent.putExtra("folder", folder)
                context!!.startActivity(intent)
            } else {
                val bitmap = getBitmap(data!!.photo?.image!!)
                showImage(bitmap)
            }
        }
    }

    private fun showImage(bitmap: Bitmap) {
        val builder = AlertDialog.Builder(context)

        builder.setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
        }

        val inflater: LayoutInflater =
            (context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
        val dialogLayout = inflater.inflate(R.layout.custom_dialog, null)
        val image = dialogLayout.findViewById<ImageView>(R.id.showingImage)
        image.setImageBitmap(bitmap)

        builder.setView(dialogLayout)
        builder.show()
    }

    private fun enterFolderName(position: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Enter a folder name")

        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.text_input_name, v as ViewGroup?, false)

        val input = viewInflated.findViewById(R.id.folderName) as EditText

        builder.setView(viewInflated)

        builder.setPositiveButton(
            android.R.string.ok
        ) { dialog, _ ->
            dialog.dismiss()
            val folderName = input.text.toString()
            cloudFragment.renameFolder(cloudList[position], folderName)
            cloudList[position].folderName = folderName
            notifyDataSetChanged()
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    override fun getItemCount(): Int {
        return cloudList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        val name: TextView = itemView.findViewById(R.id.name)
        val date: TextView = itemView.findViewById(R.id.date)
        val layout: LinearLayout = itemView.findViewById(R.id.itemsLayout)
        val imageButton: ImageButton = itemView.findViewById(R.id.menuPopup)
        val info: TextView = itemView.findViewById(R.id.info)

        init {
            imageButton.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            showPopupMenu(p0!!)
        }

        private fun showPopupMenu(view: View) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.popupmenu)
            if (info.text == "Image") {
                popupMenu.menu.findItem(R.id.download).isVisible = true
            }
            popupMenu.setOnMenuItemClickListener(this)
            popupMenu.show()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("NotifyDataSetChanged")
        override fun onMenuItemClick(p0: MenuItem?): Boolean {
            return when (p0?.itemId) {
                R.id.edit -> {
                    enterFolderName(adapterPosition)
                    notifyDataSetChanged()
                    true
                }
                R.id.delete -> {
                    cloudFragment.deleteFolder(cloudList[adapterPosition])
                    cloudList.remove(cloudList[adapterPosition])
                    notifyDataSetChanged()
                    true
                }
                R.id.download -> {
                    val bitmap = getBitmap(cloudList[adapterPosition].photo?.image!!)
                    viewToBitmap(bitmap)
                    true
                }
                else -> false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getBitmap(image: String): Bitmap {
        imageByteArray = Base64.getDecoder().decode(image)
        return BitmapFactory.decodeByteArray(
            imageByteArray,
            0,
            Base64.getDecoder().decode(image).size
        )
    }

    fun viewToBitmap(bitmap: Bitmap): Bitmap? {
        try {
            val tempFolder = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/" + Environment.DIRECTORY_DOWNLOADS
            )
            if (!tempFolder.exists()) tempFolder.mkdirs()
            val tempFile = File.createTempFile("tempFile", ".jpg", tempFolder)
            val output = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.close()
            Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            Toast.makeText(context, "Error1", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: IOException) {
            Toast.makeText(context, "Error2", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return bitmap
    }
}