package com.pavelov.currenciestest.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.LruCache
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*

class ImageLoader {
    companion object {
        // lol
        private const val FLAG_CIRCLE_URL_PREFIX =
            "https://raw.githubusercontent.com/karthiganesan90/Revolut/master/app/src/main/res/mipmap-xhdpi/ic_"
        private const val FLAG_CIRCLE_URL_POSTFIX = "_flag.png"
    }

    private val loadDestinations = mutableMapOf<String, LinkedList<WeakReference<ImageView>>>()
    private val imageLoadingsInProgress = LinkedList<String>()
    private val bitmapMemoryCache: LruCache<String, Bitmap>
    private val locker = Any()

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        bitmapMemoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    fun loadCircleFlagInto(v: ImageView, currencyCode: String, size: Int) {
        val flagUrl =
            FLAG_CIRCLE_URL_PREFIX + currencyCode.toLowerCase(Locale.getDefault()) + FLAG_CIRCLE_URL_POSTFIX
        synchronized(locker) {
            // if this ImageView already waiting for image loading, remove thos waitings
            val viewUsing = loadDestinations.filter { it.value.any { it.get() == v } }
            viewUsing.forEach {
                it.value.forEach {
                    if (it.get() == v) it.clear()
                }
            }

            // add imageView as waiter for url loading
            val currentDestination = loadDestinations[flagUrl]
            if (currentDestination != null) {
                currentDestination.add(WeakReference(v))
            } else {
                loadDestinations[flagUrl] =
                    LinkedList<WeakReference<ImageView>>().apply { add(WeakReference(v)) }
            }

            if (imageLoadingsInProgress.contains(flagUrl)) {
                return
            }

            val cached = bitmapMemoryCache.get(flagUrl)
            if (cached != null) {
                return onImageLoaded(flagUrl, cached, false)
            } else {
                v.setImageDrawable(null)
                LoadUrlImageTask(flagUrl, size).execute()
            }
        }
    }

    private fun onImageLoaded(url: String, bitmap: Bitmap, fromNetwork: Boolean) {
        logd("onImageLoaded: $url")
        synchronized(locker) {
            loadDestinations[url]
                ?.map { it.get() }
                ?.filterNotNull()
                ?.forEach {
                    if (fromNetwork) {
                        animateBitmap(bitmap, it)
                    } else {
                        it.imageAlpha = 255
                        it.setImageBitmap(bitmap)
                    }
                }
            loadDestinations[url]?.clear()
            imageLoadingsInProgress.remove(url)
        }
    }

    private fun imageLoadError(url: String) {
        logd("imageLoadError: $url")
        synchronized(locker) {
            loadDestinations[url]?.clear()
            imageLoadingsInProgress.remove(url)
        }
    }

    private fun animateBitmap(bitmap: Bitmap, v: ImageView) {
        v.clearAnimation()
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                super.applyTransformation(interpolatedTime, t)
                v.imageAlpha = (255 * interpolatedTime).toInt()
            }
        }
        animation.fillAfter = true
        animation.duration = 200
        v.imageAlpha = 0
        v.setImageBitmap(bitmap)
        v.startAnimation(animation)
    }

    inner class LoadUrlImageTask(
        private val urlString: String,
        private val size: Int
    ) : AsyncTask<Void, Nothing, Bitmap>() {
        override fun doInBackground(vararg params: Void?): Bitmap? {
            val cached = bitmapMemoryCache.get(urlString)
            if (cached != null) return cached
            val url = URL(urlString)
            var inputStream: InputStream? = null
            try {
                inputStream = url.openConnection().getInputStream()

                val opts = BitmapFactory.Options()
                opts.outWidth = size
                opts.outHeight = size
                val bitmap = BitmapFactory.decodeStream(inputStream, null, opts)
                bitmapMemoryCache.put(urlString, bitmap)
                return bitmap
            } catch (ex: Exception) {
                return null
            } finally {
                inputStream?.close()
            }
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result == null) {
                imageLoadError(urlString)
            } else {
                onImageLoaded(urlString, result, true)
            }
        }
    }

}