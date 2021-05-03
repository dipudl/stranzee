package com.leminect.strangee.view

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.leminect.strangee.R
import com.leminect.strangee.databinding.ActivityImageShowBinding
import com.leminect.strangee.network.BASE_URL

class ImageShowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityImageShowBinding = DataBindingUtil.setContentView(this,
            R.layout.activity_image_show)

        binding.imageLoadingTextView.text = getString(R.string.loading)
        binding.imageLoadingTextView.visibility = View.VISIBLE

        val imageUrl: String? = intent.getStringExtra("image")

        if(imageUrl != null && imageUrl != "") {
            imageUrl?.let { url ->
                val imgUri = (BASE_URL + url).toUri().buildUpon().scheme("https").build()
                Glide.with(this)
                    .load(imgUri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean,
                        ): Boolean {
                            binding.imageLoadingTextView.text =
                                getString(R.string.glide_image_error)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean,
                        ): Boolean {
                            binding.imageLoadingTextView.visibility = View.GONE
                            return false
                        }

                    })
                    .into(binding.fullScreenImageView)
            }
        } else {
            binding.imageLoadingTextView.text =
                getString(R.string.glide_image_error)
        }

        binding.goBackButton.setOnClickListener{
            finish()
        }
    }
}