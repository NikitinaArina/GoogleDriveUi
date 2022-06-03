package com.university.googledrive.dto

import java.io.Serializable

class User(
    val id: Long,
    val username: String,
    var password: String
) : Serializable {

}