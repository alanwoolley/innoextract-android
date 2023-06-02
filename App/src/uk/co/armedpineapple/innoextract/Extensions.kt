package uk.co.armedpineapple.innoextract

import android.content.Context
import android.widget.Toast

fun Context.toast(message: CharSequence, duration: Int) =
    Toast.makeText(this, message, duration).show()

fun Context.toast(resId: Int, duration: Int) = Toast.makeText(this, resId, duration).show()

