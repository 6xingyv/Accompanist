package miuix.view

import android.R
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import miuix.core.util.MiuiBlurUtils
import miuix.internal.util.AttributeResolver

/* loaded from: classes3.dex */
class MiuiBlurUiHelper(
    val mContext: Context,
    val viewApplyBlur: View,
    val mIsSpecialShape: Boolean,
    val mCallback: BlurStateCallback
) :
    BlurableWidget {
    var isSupportBlur: Boolean = false
    var mNeedEnableBlur: Boolean = false
    var mIsEnableBlur: Boolean = false
    var isApplyBlur: Boolean = false
    var mApplyBlur: Boolean = false
    var mBlurBlendColors: IntArray? = null
    var mBlurBlendColorModes: IntArray? = null
    var mBlurEffect: Int = 0

    /* loaded from: classes3.dex */
    interface BlurStateCallback {
        fun onBlurApplyStateChanged(z: Boolean)

        fun onBlurEnableStateChanged(z: Boolean)

        fun onCreateBlurParams(miuiBlurUiHelper: MiuiBlurUiHelper?)
    }

    var isEnableBlur: Boolean
        get() = this.mNeedEnableBlur
        set(z) {
            if (this.isSupportBlur) {
                this.mNeedEnableBlur = z
                if (MiuiBlurUtils.isEffectEnable(this.mContext)) {
                    setEnableBlurInternal(this.mNeedEnableBlur)
                }
            }
        }

    fun setEnableBlurInternal(z: Boolean) {
        if (this.mIsEnableBlur != z) {
            if (!z) {
                this.isApplyBlur = this.isApplyBlur
                applyBlurInternal(false)
            }
            this.mIsEnableBlur = z
            mCallback.onBlurEnableStateChanged(z)
            if (z && this.isApplyBlur) {
                applyBlurInternal(true)
            }
        }
    }

    // miuix.view.BlurableWidget
    override fun applyBlur(z: Boolean) {
        this.isApplyBlur = z
        applyBlurInternal(z)
    }

    fun applyBlurInternal(z: Boolean) {
        var f2: Float
        if (!this.isSupportBlur || !this.mIsEnableBlur || this.mApplyBlur == z) {
            return
        }
        this.mApplyBlur = z
        var i = 0
        if (z) {
            if (this.mBlurBlendColors == null) {
                mCallback.onCreateBlurParams(this)
            }
            mCallback.onBlurApplyStateChanged(true)
            f2 = try {
                viewApplyBlur.context.resources.displayMetrics.density
            } catch (unused: Exception) {
                2.75f
            }
            MiuiBlurUtils.setBackgroundBlur(
                this.viewApplyBlur,
                (this.mBlurEffect * f2).toInt(),
                this.mIsSpecialShape
            )
            while (true) {
                val iArr = this.mBlurBlendColors
                if (i >= iArr!!.size) {
                    return
                }
                MiuiBlurUtils.addBackgroundBlenderColor(
                    this.viewApplyBlur, iArr[i],
                    mBlurBlendColorModes!![i]
                )
                i++
            }
        } else {
            MiuiBlurUtils.clearBackgroundBlur(this.viewApplyBlur)
            MiuiBlurUtils.clearBackgroundBlenderColor(this.viewApplyBlur)
            mCallback.onBlurApplyStateChanged(false)
        }
    }

    fun setBlurParams(iArr: IntArray?, iArr2: IntArray?, i: Int) {
        this.mBlurBlendColors = iArr
        this.mBlurBlendColorModes = iArr2
        this.mBlurEffect = i
    }

    fun onConfigChanged() {
        resetBlurParams()
        if (!MiuiBlurUtils.isEffectEnable(this.mContext)) {
            setEnableBlurInternal(false)
        } else if (MiuiBlurUtils.isEnable && MiuiBlurUtils.isEffectEnable(this.mContext) && isEnableBlur) {
            setEnableBlurInternal(true)
        }
    }

    fun resetBlurParams() {
        this.mBlurBlendColors = null
        this.mBlurBlendColorModes = null
        this.mBlurEffect = 0
    }

    fun refreshBlur() {
        if (!this.mApplyBlur) {
            return
        }
        if (this.mBlurBlendColors == null) {
            MiuiBlurUtils.clearBackgroundBlur(this.viewApplyBlur)
            MiuiBlurUtils.clearBackgroundBlenderColor(this.viewApplyBlur)
            mCallback.onCreateBlurParams(this)
        }
        var f2 = try {
            viewApplyBlur.context.resources.displayMetrics.density
        } catch (unused: Exception) {
            2.75f
        }
        mCallback.onBlurApplyStateChanged(true)
        MiuiBlurUtils.setBackgroundBlur(
            this.viewApplyBlur,
            (this.mBlurEffect * f2).toInt(),
            this.mIsSpecialShape
        )
        var i = 0
        while (true) {
            val iArr = this.mBlurBlendColors
            if (i >= iArr!!.size) {
                return
            }
            MiuiBlurUtils.addBackgroundBlenderColor(
                this.viewApplyBlur, iArr[i],
                mBlurBlendColorModes!![i]
            )
            i++
        }
    }

    companion object {
        fun getFinalBlendColorForViewByBackgroundColor(
            context: Context?,
            drawable: Drawable?,
            iArr: IntArray
        ): IntArray {
            return getFinalBlendColorForViewByBackgroundColor(
                context,
                if ((drawable == null || drawable !is ColorDrawable)) 0 else drawable.color,
                iArr
            )
        }

        fun getFinalBlendColorForViewByBackgroundColor(
            context: Context?,
            i: Int,
            iArr: IntArray
        ): IntArray {
            var i = i
            val length = iArr.size
            val iArr2 = IntArray(length)
            System.arraycopy(iArr, 0, iArr2, 0, length)
            if (i == 0) {
                val resolveDrawable: Drawable? =
                    context?.let { AttributeResolver.resolveDrawable(it, R.attr.windowBackground) }
                if (resolveDrawable is ColorDrawable) {
                    i = resolveDrawable.color
                }
            }
            if (i != 0) {
                iArr2[1] = (16777215 and i) or ((-16777216) and iArr[1])
            }
            return iArr2
        }
    }
}