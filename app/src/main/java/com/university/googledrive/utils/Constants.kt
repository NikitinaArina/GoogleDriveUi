package com.university.googledrive.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.beust.klaxon.Klaxon
import com.university.googledrive.R
import com.university.googledrive.dto.Folder
import com.university.googledrive.dto.User
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class Constants {
    companion object {
        val USER_ID = "userId"
        val CHOOSE_FROM_GALLERY = "Choose from Gallery"
        val CANCEL = "Cancel"
        val MEDIA_TYPE = "application/json; charset=utf-8"
        val USERNAME = "username"
        val PASSWORD = "password"
        val USER = "user"
        val FOLDER = "folder"
        val FOLDER_ID = "id"
        val PARENT_ID = "parentId"
        val DATE_PATTERN = "yyyy-MM-dd HH:mm"
    }
}