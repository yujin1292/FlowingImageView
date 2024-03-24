package com.example.flowimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.flowimageview.ConvertUtil.dpToPixel
import kotlin.math.abs


/**
 * Flow image view
 *
 * @since 2023. 08
 * @author yujini_us
 */
class FlowImageView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setCustomAttribute(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setCustomAttribute(attrs)
    }

    // view 에 그릴 bitmap
    private var bitmap: Bitmap? = null

    // bitmap 을 그릴 paint
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1 frame 노출 시간 당 offset 이동 거리 (1초동안 이동하는 pixel 양)
    private var speed: Int = dpToPixel(context, 100)

    // 무한히 보여줄 이미지 리스트
    private var imageUrlList = mutableListOf<String>()

    // 이미지 리스트와 매칭되는 비트맵 + 로드 상태가 담긴 리스트
    private var imageBitmapList = mutableListOf<FlowItemImage>()

    // 마지막 frame 노출 nano seconds
    private var lastFrameNanoseconds: Long = -1

    // Frame 출력 시간 (nano seconds)
    private var frameTimeNanos: Long = -1

    // AttributeSet
    private var itemWidth = 0
    private var itemHeight = 0
    private var gapBetweenItem = 0
    private var itemWidthWithGap = 0


    // 기준 좌표
    private var startCoordinate = 0f
    private var endCoordinate = 0f

    private fun setCustomAttribute(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FlowImageView, 0, 0)

        itemWidth = typedArray.getDimension(R.styleable.FlowImageView_item_width, 0f).toInt()
        itemHeight = typedArray.getDimension(R.styleable.FlowImageView_item_height, 0f).toInt()
        gapBetweenItem = typedArray.getDimension(R.styleable.FlowImageView_gap_between_item, 0f).toInt()

        itemWidthWithGap = itemWidth + gapBetweenItem
    }

    fun setData(imageUrlList: MutableList<String>, pixelPerSecond: Int) {
        this.imageUrlList = imageUrlList
        imageBitmapList = MutableList(imageUrlList.size) { FlowItemImage() }
        loadBitmapIntoBitmapList(R.drawable.cookie)
        speed = pixelPerSecond
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = itemHeight + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
        endCoordinate = (width - 1).toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (bitmap != null) {
            val frameTimeNanos = checkTime()
            val distance = (abs(speed) / NANOS_PER_SECOND * frameTimeNanos).toFloat()
            moveToNextCoordinate(distance)
        }

        setFlowBitmap()
        bitmap?.let {
            canvas?.drawBitmap(it, 0f, 0f, paint)
        }
    }

    /**
     * 현재 시간 초기화, 지난 설정과 현재의 차이 반환
     */
    private fun checkTime(): Long {
        if (lastFrameNanoseconds == -1L) {
            lastFrameNanoseconds = System.nanoTime()
        }

        val currentNanoTime = System.nanoTime()
        frameTimeNanos = currentNanoTime - lastFrameNanoseconds
        lastFrameNanoseconds = currentNanoTime

        return frameTimeNanos
    }


    /**
     * 기준 좌표를 offset 만큼 움직임
     *
     * @param offset 움직일 Pixel 값
     */
    private fun moveToNextCoordinate(offset: Float) {
        startCoordinate += offset
        endCoordinate += offset
        if (startCoordinate > (itemWidthWithGap * imageUrlList.size)) {
            startCoordinate %= (itemWidthWithGap * imageUrlList.size)
            endCoordinate = startCoordinate + width
        }
    }


    /**
     * 비트맵 세팅
     */
    private fun setFlowBitmap() {
        // 뷰에 보여줄 이미지 인덱스
        val startImageIndex: Int = (startCoordinate / itemWidthWithGap).toInt()
        val lastImageIndex: Int = (endCoordinate / itemWidthWithGap).toInt()

        // 보여지는 이미지 마지막 기준으로 하나 더 먼저 로드함
        updateBitmapList(0, lastImageIndex + 1)

        val initialBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(initialBitmap)

        var count = 0

        for (imageIndex in startImageIndex..lastImageIndex) {
            val idx = if (imageBitmapList.size <= imageIndex) imageIndex % imageBitmapList.size
            else imageIndex

            val currentBitmap: Bitmap = imageBitmapList[idx].bitmap ?: continue

            val left = (itemWidthWithGap * count).toFloat() - (startCoordinate % itemWidthWithGap)
            canvas.drawBitmap(currentBitmap, left, paddingTop.toFloat(), paint)

            count++
        }

        bitmap = initialBitmap
        postInvalidateOnAnimation()
    }

    private fun loadBitmapIntoBitmapList(idx: Int, imageUrl: String) {
        imageBitmapList[idx].status = FlowImageStatus.LOADING

        Glide.with(this)
            .asBitmap()
            .override(itemWidth, itemHeight)
            .centerCrop()
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    imageBitmapList[idx].apply {
                        status = FlowImageStatus.LOADED
                        bitmap = resource
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    imageBitmapList[idx].apply {
                        status = FlowImageStatus.DEFAULT_BITMAP
                    }
                }
            })
    }

    private fun loadBitmapIntoBitmapList(resourceId: Int) {

        Glide.with(this)
            .asBitmap()
            .override(itemWidth, itemHeight)
            .centerCrop()
            .load(resourceId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    imageBitmapList.forEach {
                        it.bitmap = resource
                    }
                }
            })
    }

    private fun updateBitmapList(start: Int, end: Int) {
        for (pos in start..end) {
            if (pos !in 0 until imageUrlList.size) break

            if (imageBitmapList[pos].status != FlowImageStatus.LOADED) {
                loadBitmapIntoBitmapList(pos, imageUrlList[pos])
            }
        }
    }

    data class FlowItemImage(
        var status: FlowImageStatus = FlowImageStatus.DEFAULT_BITMAP,
        var bitmap: Bitmap? = null,
    )

    enum class FlowImageStatus {
        LOADED, DEFAULT_BITMAP, LOADING
    }

    companion object {
        // nano seconds -> seconds 변환을 위한 상수값
        private const val NANOS_PER_SECOND = 1e9 // 1e9 = 1 * 10^9 = 1000000000
    }
}