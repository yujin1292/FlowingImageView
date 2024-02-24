package com.example.flowimageview

import android.content.Context
import android.util.TypedValue

object ConvertUtil {

    fun dpToPixel(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
    }

}