package com.university.googledrive.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.university.googledrive.R
import com.university.googledrive.activity.ProfileActivity
import com.university.googledrive.databinding.FragmentLoginBinding
import com.university.googledrive.utils.Constants.Companion.MEDIA_TYPE
import com.university.googledrive.utils.Constants.Companion.PASSWORD
import com.university.googledrive.utils.Constants.Companion.USER
import com.university.googledrive.utils.Constants.Companion.USERNAME
import com.university.googledrive.utils.Urls
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginFragment : Fragment() {
    private val httpClient = OkHttpClient()
    private var _binding: FragmentLoginBinding? = null
    private lateinit var username: EditText
    private lateinit var password: EditText

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        username = binding.editTextTextPersonName
        password = binding.editTextTextPassword

        binding.loginButton.setOnClickListener {
            login()
        }

        binding.signupButton.setOnClickListener {
            signup()
        }

        return binding.root
    }

    fun login() {
        val login = username.text.toString()
        val pass = password.text.toString()

        if (login.isBlank()) {
            username.error = "Please, enter a username!"
        } else {
            username.error = null
        }

        if (pass.isNotBlank()) {
            password.error = null
        } else {
            password.error = "Please, enter a password!"
        }

        if (login.isNotBlank() && pass.isNotBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(USERNAME, login)
            jsonObject.put(PASSWORD, pass)

            val body =
                jsonObject.toString().toRequestBody(MEDIA_TYPE.toMediaType())

            val request = Request.Builder()
                .url(Urls.BASE_URL + Urls.GET_USER)
                .post(body)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code == 200) {
                        val body = response.body
                        startActivity(
                            Intent(context, ProfileActivity::class.java).putExtra(
                                USER,
                                body?.string()
                            )
                        )
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                "Username or password incorrect",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        }
    }

    fun signup() {
        fragmentManager?.beginTransaction()
            ?.replace(R.id.frame, RegisterFragment())
            ?.commit()
    }
}