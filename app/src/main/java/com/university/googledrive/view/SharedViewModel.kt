package com.university.googledrive.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.university.googledrive.dto.Folder
import com.university.googledrive.dto.User

class SharedViewModel: ViewModel() {
    val selectedUser = MutableLiveData<User>()
    val selectedFolder = MutableLiveData<Folder>()

    fun selectUser(user: User) {
        selectedUser.value = user
    }

    fun selectFolder(folder: Folder) {
        selectedFolder.value = folder
    }
}