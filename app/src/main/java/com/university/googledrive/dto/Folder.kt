package com.university.googledrive.dto

class Folder {
    var id: Long = 0
    var user: User? = null
    var folderName: String? = null
    var folder: Folder? = null
    var date: String? = null
    var photo: Photo? = null

    constructor(id: Long, user: User, folderName: String, parentFolder: Folder) {
        this.id = id
        this.user = user
        this.folderName = folderName
        this.folder = parentFolder
    }


    constructor(user: User, folderName: String, parentFolder: Folder) {
        this.user = user
        this.folderName = folderName
        this.folder = parentFolder
    }

    constructor(user: User, folderName: String) {
        this.user = user
        this.folderName = folderName
    }

    constructor()

    constructor(user: User?, folderName: String?, folder: Folder?, photo: Photo?) {
        this.user = user
        this.folderName = folderName
        this.folder = folder
        this.photo = photo
    }

    constructor(user: User?, folderName: String?, photo: Photo?) {
        this.user = user
        this.folderName = folderName
        this.photo = photo
    }
}