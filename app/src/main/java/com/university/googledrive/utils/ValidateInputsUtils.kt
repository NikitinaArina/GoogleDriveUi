package com.university.googledrive.utils

import android.text.BoringLayout
import android.widget.EditText

class ValidateInputsUtils {
    companion object {
        fun validatePasswordInput(password: EditText, confirmPassword: EditText): Boolean{
            if (ImageUtils.isValidPassword(password.text.toString())) {
                password.error = null
                return if (!(password.text.toString().equals(confirmPassword.text.toString()))) {
                    confirmPassword.error = "Passwords should be equals"
                    false
                } else {
                    confirmPassword.error = null
                    true
                }
            } else {
                password.error = "Password should contains at least six symbols" +
                        "\nDigit must occur at least once\n" +
                        "A lower case letter must occur at least once\n" +
                        "An upper case letter must occur at least once\n" +
                        "A special character must occur at least once "
                return false
            }
        }

        fun validateUsernameInput(username: EditText): Boolean {
            return if (username.text.toString().isBlank()) {
                username.error = "Please, enter a username!"
                false
            } else {
                username.error = null
                true
            }
        }

    }
}