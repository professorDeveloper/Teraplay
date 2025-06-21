package com.saikou.teraplay.utils

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.saikou.teraplay.R
import com.saikou.teraplay.app.MyApp
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

//
fun View.alphaAnim() {
    val anim = AnimationUtils.loadAnimation(
        MyApp.context,
        R.anim.alpha_anim
    ).apply {
        duration = 1800L

        fillAfter = true
    }

    startAnimation(anim)

}

fun View.visible() {
    this.visibility = View.VISIBLE
}


fun View.gone() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

private val SMS_PERMISSION_CODE = 101

fun Fragment.checkSmsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
}

fun Fragment.requestSmsPermission() {
    ActivityCompat.requestPermissions(
        requireActivity(),
        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
        SMS_PERMISSION_CODE
    )
}

fun initActivity(a: Activity) {
    val window = a.window
    WindowCompat.setDecorFitsSystemWindows(window, false)
//    manageThemeAndRefresh(readData<Int>("current_theme", a) ?: 0)

}


fun Activity.hideSystemBars() {
    window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
}

fun <T> readData(fileName: String, context: Context? = null, toast: Boolean = true): T? {
    val a = context ?: MyApp.context
    try {
        if (a?.fileList() != null)
            if (fileName in a.fileList()) {
                val fileIS: FileInputStream = a.openFileInput(fileName)
                val objIS = ObjectInputStream(fileIS)
                val data = objIS.readObject() as T
                objIS.close()
                fileIS.close()
                return data
            }
    } catch (e: Exception) {
        if (toast) snackString("Error loading data $fileName")
        e.printStackTrace()
    }
    return null
}

fun saveData(fileName: String, data: Any?, context: Context? = null) {
    tryWith {
        val a = context ?: MyApp.context
        if (a != null) {
            val fos: FileOutputStream = a.openFileOutput(fileName, Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            os.writeObject(data)
            os.close()
            fos.close()
        }
    }
}
fun snackString(s: String?, activity: Activity? = null, clipboard: String? = null) {
    if (s != null) {
        (activity)?.apply {
            runOnUiThread {
                val snackBar = Snackbar.make(
                    window.decorView.findViewById(android.R.id.content),
                    s,
                    Snackbar.LENGTH_LONG
                )
                snackBar.view.apply {
                    updateLayoutParams<FrameLayout.LayoutParams> {
                        gravity = (Gravity.CENTER_HORIZONTAL or Gravity.TOP)
                        width = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    translationY = (24f + 32f)
                    translationZ = 32f
                    val shapeDrawable = ShapeDrawable()
                    shapeDrawable.paint.setColor(activity.getColor(R.color.chip_background))// Set the background color if needed
                    shapeDrawable.paint.style = Paint.Style.FILL
                    shapeDrawable.shape = RoundRectShape(
                        floatArrayOf(120f, 120f, 120f, 120f, 120f, 120f, 120f, 120f),
                        null,
                        null
                    )

                    this.background = shapeDrawable
                    setOnClickListener {
                        snackBar.dismiss()
                    }
                    setOnLongClickListener {
                        true
                    }
                }
                snackBar.show()
            }
        }
    }
}

fun animationTransactionClearStack(clearFragmentID: Int): NavOptions.Builder {
    val navBuilder = NavOptions.Builder()
    navBuilder.setEnterAnim(R.anim.from_right).setExitAnim(R.anim.to_left)
        .setPopEnterAnim(R.anim.from_left).setPopExitAnim(R.anim.to_right)
        .setPopUpTo(clearFragmentID, true)
    return navBuilder
}

fun animationTransaction(): NavOptions.Builder {
    val navBuilder = NavOptions.Builder()
    navBuilder.setEnterAnim(R.anim.from_right).setExitAnim(R.anim.to_left)
        .setPopEnterAnim(R.anim.from_left).setPopExitAnim(R.anim.to_right)
    return navBuilder
}

fun <T> tryWith(post: Boolean = false, snackbar: Boolean = true, call: () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Throwable) {
        null
    }
}


fun ImageView.loadImage(file: String?, size: Int = 0) {
    tryWith {
        val glideUrl = GlideUrl(file)
        Glide.with(this.context).load(glideUrl)
            .transition(DrawableTransitionOptions.withCrossFade()).override(size).into(this)
    }
}


fun BottomNavigationView.showWithAnimation(fragmentContainerView: View) {
    if (this.visibility == View.VISIBLE) return
    this.visible()
    this.animateTranslationY(0f, 66f, 700)
    fragmentContainerView.animateMarginBottom(0f, 700)
}

fun BottomNavigationView.hideWithoutAnimation(fragmentContainerView: View) {
    if (this.visibility == View.GONE) return
    this.gone()

    val params =
        fragmentContainerView.layoutParams as ConstraintLayout.LayoutParams
    params.setMargins(
        params.leftMargin,
        params.topMargin,
        params.rightMargin,
        0
    )
    fragmentContainerView.layoutParams = params

}

fun BottomNavigationView.hideWithAnimation(fragmentContainerView: View) {
    if (this.visibility == View.GONE) return
    this.animateTranslationY(66f, 0f, 700)
}

fun View.animateTranslationY(animateFrom: Float, animateTo: Float, duration: Long) {
    val animator =
        ObjectAnimator.ofFloat(
            this, "translationY", TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                animateTo,
                resources.displayMetrics
            ), TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                animateFrom,
                resources.displayMetrics
            )
        )
    animator.duration = duration
    if (animateTo == 0f) {
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                this@animateTranslationY.gone()
            }
        })
    }
    animator.start()

}


fun View.animateMarginBottom(size: Float, duration: Long) {
    val dpToPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        size,
        resources.displayMetrics
    )


    val params =
        this.layoutParams as ConstraintLayout.LayoutParams
    val animator = ValueAnimator.ofInt(params.bottomMargin, dpToPx.toInt())
    animator.addUpdateListener {
        val value = it.animatedValue as Int
        params.setMargins(
            params.leftMargin,
            params.topMargin,
            params.rightMargin,
            value
        )
        this.layoutParams = params
    }
    animator.duration = duration
    animator.start()
}
