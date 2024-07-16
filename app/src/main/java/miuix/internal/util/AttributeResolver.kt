package miuix.internal.util

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue

/* loaded from: classes3.dex */
object AttributeResolver {
    val TYPED_VALUE: TypedValue = TypedValue()
    val TYPED_VALUE_THREAD_LOCAL: ThreadLocal<TypedValue> = ThreadLocal()

    fun getTypedValue(context: Context): TypedValue {
        if (context.mainLooper.thread === Thread.currentThread()) {
            return TYPED_VALUE
        }
        val threadLocal = TYPED_VALUE_THREAD_LOCAL
        val typedValue = threadLocal.get()
        if (typedValue != null) {
            return typedValue
        }
        val typedValue2 = TypedValue()
        threadLocal.set(typedValue2)
        return typedValue2
    }

    fun resolve(context: Context, i: Int): Int {
        val typedValue = getTypedValue(context)
        if (context.theme.resolveAttribute(i, typedValue, true)) {
            return typedValue.resourceId
        }
        return -1
    }

    fun resolveDrawable(context: Context, i: Int): Drawable? {
        val typedValue = getTypedValue(context)
        if (!context.theme.resolveAttribute(i, typedValue, true)) {
            return null
        }
        if (typedValue.resourceId > 0) {
            if (Build.VERSION.SDK_INT >= 21) {
                return context.resources.getDrawable(typedValue.resourceId, context.theme)
            }
            return context.resources.getDrawable(typedValue.resourceId)
        }
        val i2 = typedValue.type
        if (i2 < 28 || i2 > 31) {
            return null
        }
        return ColorDrawable(typedValue.data)
    }

    fun resolveColor(context: Context, i: Int): Int {
        val innerResolveColor =
            innerResolveColor(context, i)!!
        if (innerResolveColor != null) {
            return innerResolveColor
        }
        return context.resources.getColor(-1)
    }

    fun resolveColor(context: Context, i: Int, i2: Int): Int {
        val innerResolveColor = innerResolveColor(context, i)
        return innerResolveColor ?: i2
    }

    fun innerResolveColor(context: Context, i: Int): Int? {
        val typedValue = getTypedValue(context)
        if (!context.theme.resolveAttribute(i, typedValue, true)) {
            return null
        }
        if (typedValue.resourceId > 0) {
            return context.resources.getColor(typedValue.resourceId)
        }
        val i2 = typedValue.type
        if (i2 < 28 || i2 > 31) {
            return null
        }
        return typedValue.data
    }

    fun resolveBoolean(context: Context, i: Int, z: Boolean): Boolean {
        val typedValue = getTypedValue(context)
        if (!context.theme.resolveAttribute(i, typedValue, true) || typedValue.type != 18) {
            return z
        }
        if (typedValue.resourceId > 0) {
            return context.resources.getBoolean(typedValue.resourceId)
        }
        return typedValue.data != 0
    }

    fun resolveDimensionPixelSize(context: Context, i: Int): Int {
        val resolveTypedValue = resolveTypedValue(context, i)
        if (resolveTypedValue == null || resolveTypedValue.type != 5) {
            return 0
        }
        if (resolveTypedValue.resourceId > 0) {
            return context.resources.getDimensionPixelSize(resolveTypedValue.resourceId)
        }
        return TypedValue.complexToDimensionPixelSize(
            resolveTypedValue.data,
            context.resources.displayMetrics
        )
    }

    fun resolveInt(context: Context, i: Int, i2: Int): Int {
        val typedValue = getTypedValue(context)
        if (!context.theme.resolveAttribute(i, typedValue, true)) {
            return i2
        }
        if (typedValue.resourceId > 0) {
            return context.resources.getInteger(typedValue.resourceId)
        }
        val i3 = typedValue.type
        return if ((i3 < 16 || i3 > 31)) i2 else typedValue.data
    }

    fun resolveFloat(context: Context, i: Int, f2: Float): Float {
        val typedValue = getTypedValue(context)
        if (!context.theme.resolveAttribute(i, typedValue, true) || typedValue.type != 4) {
            return f2
        }
        if (typedValue.resourceId > 0) {
            return if (Build.VERSION.SDK_INT >= 29) context.resources.getFloat(typedValue.resourceId) else f2
        }
        return typedValue.data.toFloat()
    }

    fun resolveTypedValue(context: Context, i: Int): TypedValue? {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(i, typedValue, true)) {
            return typedValue
        }
        return null
    }
}