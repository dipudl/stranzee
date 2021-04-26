package com.leminect.strangee.adapter

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.leminect.strangee.R
import com.leminect.strangee.model.ChatData
import com.leminect.strangee.model.Message
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.network.BASE_URL
import com.leminect.strangee.network.StrangeeBackData
import com.leminect.strangee.viewmodel.FindStatus
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("detailText")
fun bindDetailText(textView: TextView, data: Strangee?) {
    data?.let {
        val formattedDetail = "${convertMillsToAge(data.birthday)} • ${data.country}"
        textView.text = formattedDetail
    }
}

@BindingAdapter("firstName", "lastName", "showFullName")
fun bindFormattedName(
    textView: TextView,
    firstName: String?,
    lastName: String?,
    showFull: Boolean?,
) {
    if (firstName != null && lastName != null && showFull != null) {
        val shortName = if (showFull) {
            "$firstName $lastName"
        } else {
            "$firstName ${if (lastName.isNotEmpty()) lastName[0] else ""}."
        }
        textView.text = shortName
    }
}

@BindingAdapter("timestamp")
fun bindTimestamp(textView: TextView, timestamp: Long?) {
    timestamp?.let {
        val difference: Long = Date().time - timestamp;

        val sdf: SimpleDateFormat = when {
            difference > 31_536_000_000L -> SimpleDateFormat("MMM yyyy", Locale.US)
            difference > 86400_000L -> SimpleDateFormat("dd MMM", Locale.US)
            else -> SimpleDateFormat("hh:mm a", Locale.US)
        }

        val localDateTime: String = sdf.format(timestamp);
        textView.text = localDateTime
    }
}

@BindingAdapter("imgUrl", "userId", requireAll = false)
fun bindImageUrl(imageView: View, imageUrl: String?, userId: String? = null) {
    imageUrl?.let { url ->
        val imgUri = (BASE_URL + url).toUri().buildUpon().scheme("http").build()
        val glide = Glide.with(imageView.context)
            .load(imgUri)
            .apply(RequestOptions()
                .placeholder(R.drawable.loading_image)
                .error(R.drawable.ic_error))

        userId?.let {
            if (url.contains(userId)) {
                val context = imageView.context
                val key = context.getSharedPreferences(context.getString(R.string.shared_prefs_name),
                    AppCompatActivity.MODE_PRIVATE).getString(context.getString(R.string.prefs_signature), System.currentTimeMillis().toString())
                glide.signature(ObjectKey(key!!))
            }
        }

        glide.into(imageView as ImageView)
    }
}

@BindingAdapter("saveProfileIcon")
fun bindSaveProfileIcon(imageView: ImageView, isSaved: Boolean?) {
    isSaved?.let {
        if (isSaved)
            imageView.setImageResource(R.drawable.ic_unsave_profile)
        else
            imageView.setImageResource(R.drawable.ic_save_profile)
    }
}

@BindingAdapter("onlineOfflineIcon")
fun bindOnlineOfflineIcon(imageView: ImageView, isOnline: Boolean?) {
    isOnline?.let {
        if (isOnline)
            imageView.setImageResource(R.drawable.ic_online)
        else
            imageView.setImageResource(R.drawable.ic_offline)
    }
}

@BindingAdapter("imageVisible")
fun bindImageVisible(imageView: ImageView, show: Boolean?) {
    show?.let {
        if (show)
            imageView.visibility = View.VISIBLE
        else
            imageView.visibility = View.GONE
    }
}

@BindingAdapter("save")
fun bindImageVisible(textView: TextView, save: Boolean?) {
    save?.let {
        if (save)
            textView.text = textView.context.getString(R.string.unsave_profile)
        else
            textView.text = textView.context.getString(R.string.save_profile)
    }
}

@BindingAdapter("chatImage", "chatImageVisible")
fun chatImageBinding(imageView: ImageView, message: Message?, isVisible: Boolean?) {
    if (message != null && isVisible != null) {
        if (message.type == "image" && isVisible) {
            imageView.visibility = View.VISIBLE
            bindImageUrl(imageView, message.imageUrl)
        } else {
            imageView.visibility = View.GONE
        }
    } else {
        imageView.visibility = View.GONE
    }
}

@BindingAdapter("chatText", "chatTextVisible")
fun chatImageBinding(textView: TextView, message: Message?, isVisible: Boolean?) {
    if (message != null && isVisible != null) {
        if (message.type == "text" && isVisible) {
            textView.visibility = View.VISIBLE
            textView.text = message.text
        } else {
            textView.visibility = View.GONE
        }
    } else {
        textView.visibility = View.GONE
    }
}

@BindingAdapter("listData")
fun bindRecyclerView(recyclerView: RecyclerView, data: List<Strangee>?) {
    data?.let {
        val adapter = recyclerView.adapter as StrangeeGridAdapter
        adapter.submitList(data)
    }
}

@BindingAdapter("chatListData")
fun bindChatRecyclerView(recyclerView: RecyclerView, data: List<Message>?) {
    data?.let {
        val adapter = recyclerView.adapter as SingleChatAdapter
        adapter.submitList(data)
    }
}

fun convertMillsToAge(mills: Long): Int {
    val dob: Calendar = Calendar.getInstance().apply { timeInMillis = mills };
    val today: Calendar = Calendar.getInstance();

    var age: Int = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)

    if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
        age--
    }

    return age
}