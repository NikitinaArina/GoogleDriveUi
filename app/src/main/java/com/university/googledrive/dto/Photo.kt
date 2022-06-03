package com.university.googledrive.dto

class Photo {
    var id: Long = 0

    var image: String? = null

    constructor(image: String?) {
        this.image = image
    }

    constructor(id: Long, image: String?) {
        this.id = id
        this.image = image
    }

    constructor(id: Long) {
        this.id = id
    }
}
