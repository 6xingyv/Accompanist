package miuix.core.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import java.lang.reflect.Method

/* loaded from: classes3.dex */
object MiuiBlurUtils {
    var ENABLE_MIUI_BLUR: Boolean? = null
    var METHOD_ADD_BG_BLEND_COLOR: Method? = null
    var METHOD_CLEAR_BG_BLEND_COLOR: Method? = null
    var METHOD_SET_BG_BLUR_MODE: Method? = null
    var METHOD_SET_BG_BLUR_RADIUS: Method? = null
    var METHOD_SET_VIEW_BLUR_MODE: Method? = null
    var SUPPORT_MIUI_BLUR: Boolean? = null

    init {
        if (Build.VERSION.SDK_INT < 33) {
            SUPPORT_MIUI_BLUR = false
        } else {
            SUPPORT_MIUI_BLUR = true
        }
    }

    val isEnable: Boolean
        get() = SUPPORT_MIUI_BLUR!!

    @Synchronized
    fun isEffectEnable(context: Context?): Boolean {
        synchronized(MiuiBlurUtils::class.java) {
            if (!SUPPORT_MIUI_BLUR!!) {
                return false
            }
            if (context == null) {
                return false
            }
            if (ENABLE_MIUI_BLUR == null) {
                ENABLE_MIUI_BLUR = Settings.Secure.getInt(
                    context.contentResolver,
                    "background_blur_enable",
                    0
                ) == 1
            }
            return ENABLE_MIUI_BLUR!!
        }
    }

    @Synchronized
    fun clearEffectEnable() {
        synchronized(MiuiBlurUtils::class.java) {
            ENABLE_MIUI_BLUR = null
        }
    }

    fun setBackgroundBlur(view: View, i: Int, z: Boolean): Boolean {
        if (!SUPPORT_MIUI_BLUR!! || !isEffectEnable(view.context)) {
            return false
        }
        try {
            if (METHOD_SET_BG_BLUR_MODE == null) {
                METHOD_SET_BG_BLUR_MODE =
                    View::class.java.getMethod("setMiBackgroundBlurMode", Integer.TYPE)
            }
            if (METHOD_SET_BG_BLUR_RADIUS == null) {
                METHOD_SET_BG_BLUR_RADIUS =
                    View::class.java.getMethod("setMiBackgroundBlurRadius", Integer.TYPE)
            }
            METHOD_SET_BG_BLUR_MODE!!.invoke(view, 1)
            METHOD_SET_BG_BLUR_RADIUS!!.invoke(view, i)
            return setViewBlurMode(view, if (z) 2 else 1)
        } catch (unused: Exception) {
            METHOD_SET_BG_BLUR_MODE = null
            METHOD_SET_BG_BLUR_RADIUS = null
            return false
        }
    }

    fun setBackgroundBlurMode(view: View?, i: Int): Boolean {
        if (!SUPPORT_MIUI_BLUR!!) {
            return false
        }
        try {
            if (METHOD_SET_BG_BLUR_MODE == null) {
                METHOD_SET_BG_BLUR_MODE =
                    View::class.java.getMethod("setMiBackgroundBlurMode", Integer.TYPE)
            }
            METHOD_SET_BG_BLUR_MODE!!.invoke(view, i)
            return true
        } catch (unused: Exception) {
            METHOD_SET_BG_BLUR_MODE = null
            return false
        }
    }

    fun clearBackgroundBlur(view: View?): Boolean {
        if (setBackgroundBlurMode(view, 0)) {
            return setViewBlurMode(view, 0)
        }
        return false
    }

    fun setViewBlurMode(view: View?, i: Int): Boolean {
        if (!SUPPORT_MIUI_BLUR!!) {
            return false
        }
        try {
            if (METHOD_SET_VIEW_BLUR_MODE == null) {
                METHOD_SET_VIEW_BLUR_MODE =
                    View::class.java.getMethod("setMiViewBlurMode", Integer.TYPE)
            }
            METHOD_SET_VIEW_BLUR_MODE!!.invoke(view, i)
            return true
        } catch (unused: Exception) {
            METHOD_SET_VIEW_BLUR_MODE = null
            return false
        }
    }

    fun addBackgroundBlenderColor(view: View, i: Int, i2: Int): Boolean {
        if (!SUPPORT_MIUI_BLUR!! || !isEffectEnable(view.context)) {
            return false
        }
        try {
            if (METHOD_ADD_BG_BLEND_COLOR == null) {
                val cls: Class<*> = Integer.TYPE
                METHOD_ADD_BG_BLEND_COLOR =
                    View::class.java.getMethod("addMiBackgroundBlendColor", cls, cls)
            }
            METHOD_ADD_BG_BLEND_COLOR!!.invoke(view, i, i2)
            return true
        } catch (unused: Exception) {
            METHOD_ADD_BG_BLEND_COLOR = null
            return false
        }
    }

    fun clearBackgroundBlenderColor(view: View?): Boolean {
        if (!SUPPORT_MIUI_BLUR!!) {
            return false
        }
        try {
            if (METHOD_CLEAR_BG_BLEND_COLOR == null) {
                METHOD_CLEAR_BG_BLEND_COLOR =
                    View::class.java.getMethod("clearMiBackgroundBlendColor", *arrayOfNulls(0))
            }
            METHOD_CLEAR_BG_BLEND_COLOR!!.invoke(view, *arrayOfNulls(0))
            return true
        } catch (unused: Exception) {
            METHOD_CLEAR_BG_BLEND_COLOR = null
            return false
        }
    }
}