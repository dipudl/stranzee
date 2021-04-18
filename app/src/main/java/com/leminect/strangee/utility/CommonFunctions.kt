package com.leminect.strangee.utility

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.loader.content.CursorLoader
import com.leminect.strangee.R
import com.leminect.strangee.model.User
import java.io.File
import java.util.*


fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun emailCheck(email: String): Boolean {
    val regex: Regex = Regex("^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$")
    return email.matches(regex)
}

fun saveUserToSharedPrefs(context: Context, user: User, token: String, refreshProfileImageSignature: Boolean = true) {
    val prefsEditor =
        context.getSharedPreferences(context.getString(R.string.shared_prefs_name),
            AppCompatActivity.MODE_PRIVATE).edit()
    prefsEditor.putString(context.getString(R.string.prefs_token), token)
    prefsEditor.putString(context.getString(R.string.prefs_user_id), user.userId)
    prefsEditor.putString(context.getString(R.string.prefs_firstName),
        user.firstName)
    prefsEditor.putString(context.getString(R.string.prefs_lastName),
        user.lastName)
    prefsEditor.putString(context.getString(R.string.prefs_image_url),
        user.imageUrl)
    prefsEditor.putString(context.getString(R.string.prefs_country),
        user.country)
    prefsEditor.putString(context.getString(R.string.prefs_gender), user.gender)
    prefsEditor.putLong(context.getString(R.string.prefs_birthday),
        user.birthday)
    prefsEditor.putString(context.getString(R.string.prefs_about_me),
        user.aboutMe)
    prefsEditor.putString(context.getString(R.string.prefs_email), user.email)
    prefsEditor.putString(context.getString(R.string.prefs_interested_in),
        user.interestedIn.joinToString(separator = ","))

    if(refreshProfileImageSignature) {
        prefsEditor.putString(context.getString(R.string.prefs_signature),
            System.currentTimeMillis().toString())
    }

    prefsEditor.commit()
}

fun getFromSharedPreferences(context: Context): Pair<String, User> {
    val prefs = context.getSharedPreferences(context.getString(R.string.shared_prefs_name),
        AppCompatActivity.MODE_PRIVATE)

    val token = prefs.getString(context.getString(R.string.prefs_token), "")
    val user: User = User(
        prefs.getString(context.getString(R.string.prefs_firstName), "")!!,
        prefs.getString(context.getString(R.string.prefs_lastName), "")!!,
        prefs.getString(context.getString(R.string.prefs_image_url), "")!!,
        prefs.getString(context.getString(R.string.prefs_country), "")!!,
        prefs.getString(context.getString(R.string.prefs_gender), "")!!,
        prefs.getString(context.getString(R.string.prefs_interested_in), "")!!.split(","),
        prefs.getLong(context.getString(R.string.prefs_birthday), 0),
        prefs.getString(context.getString(R.string.prefs_about_me), "")!!,
        prefs.getString(context.getString(R.string.prefs_email), "")!!,
        "",
        prefs.getString(context.getString(R.string.prefs_user_id), "")!!
        )

    return Pair(token!!, user)
}

fun File.getMimeType(fallback: String = "image/*"): String {
    return MimeTypeMap.getFileExtensionFromUrl(toString())
        ?.run { MimeTypeMap.getSingleton().getMimeTypeFromExtension(toLowerCase(Locale.ROOT)) }
        ?: fallback // You might set it to */*
}