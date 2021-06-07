package it.polito.mad.carpooling_09.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withTranslation
import com.google.firebase.firestore.GeoPoint
import it.polito.mad.carpooling_09.R
import it.polito.mad.carpooling_09.data.Stop
import it.polito.mad.carpooling_09.utils.convertUTCToString
import it.polito.mad.carpooling_09.utils.formatSimpleAddress

class StopsView(
    context: Context,
    attrSet: AttributeSet?,
    defStyle: Int
) : View(context, attrSet, defStyle) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrSet: AttributeSet?) : this(context, attrSet, 0)

    private val paint: Paint = Paint()
    private val paint2: Paint = Paint()
    private val paint3: Paint = Paint()
    private val paint4: Paint = Paint()
    private val paint5: Paint = Paint()
    private val paint6: Paint = Paint()

    private val d = context.resources.displayMetrics.density

    private var compact: Boolean
    private var listStop: List<Stop>
    private var number: Int

    private var indexDep: Int?
    private var indexArr: Int?

    fun setListStop(listStops: List<Stop>, departure: GeoPoint? = null, arrival: GeoPoint? = null) {

        // If we select the compact option, we want to show only the start and end destination
        // without the intermediate stops
        if (compact) {
            number = 2
            listStop = listOf(listStops[0], listStops[listStops.size - 1])
        } else {
            listStop = listStops
            number = if (listStop.size > 1) listStop.size else 0
        }

        if (departure != null && arrival != null) {
            listStops.forEachIndexed { index, stop ->
                if (stop.location == departure) {
                    indexDep = index
                } else if (stop.location == arrival) {
                    indexArr = index
                }

            }
        }
    }

    init {

        paint.style = Paint.Style.FILL_AND_STROKE
        paint.color = Color.rgb(140, 140, 140)
        paint.strokeWidth = 3 * d
        paint.isAntiAlias = true

        paint6.style = Paint.Style.FILL_AND_STROKE
        paint6.color =  Color.BLUE
        paint6.strokeWidth = 3 * d
        paint6.isAntiAlias = true

        paint2.style = Paint.Style.FILL
        paint2.textSize = 14 * d
        paint2.isAntiAlias = true

        paint3.style = Paint.Style.FILL
        paint3.textSize = 16 * d
        paint3.isFakeBoldText = true;
        paint3.isAntiAlias = true

        paint4.style = Paint.Style.FILL
        paint4.textSize = 14 * d
        paint4.color = Color.BLUE
        paint4.isAntiAlias = true

        paint5.style = Paint.Style.FILL
        paint5.textSize = 16 * d
        paint5.color = Color.BLUE
        paint5.isFakeBoldText = true;
        paint5.isAntiAlias = true

        // read attributes from xml
        val attributes = context.obtainStyledAttributes(attrSet, R.styleable.StopsView)
        compact = attributes.getBoolean(R.styleable.StopsView_compact, false)
        attributes.recycle()

        listStop = mutableListOf<Stop>()
        number = 0

        indexDep = null
        indexArr = null

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredHeight =
            suggestedMinimumHeight + paddingTop + paddingBottom + (45 * number * d).toInt()

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)


        //Measure Height
        val height = if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            Math.min(desiredHeight, heightSize)
        } else {
            //Be whatever you want
            desiredHeight
        }

        //MUST CALL THIS
        setMeasuredDimension(widthSize, height)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.withTranslation(7 * d + paddingLeft, height * .1f + paddingTop) {

            if (number == 0)
                drawText("There aren't enough data", 10 * d, 5 * d + 40 * d, paint2)

            for (i in 0 until number) {

                var paintHour : Paint
                var paintAddress : Paint
                var paintLine: Paint

                if(indexArr!=null && indexDep!=null && i>= indexDep!! && i <= indexArr!!){
                    paintHour = paint5
                    paintAddress = paint4
                    paintLine = paint6
                } else {
                    paintHour = paint3
                    paintAddress = paint2
                    paintLine = paint
                }

                drawCircle(0f, 0f + 45 * d * i, 5 * d, paintLine)


                if (i != number - 1 && indexArr==i)
                    drawLine(0f, 6 * d + 45 * d * i, 0f, 45 * d + 45 * d * i, paint)
                else if (i != number - 1)
                    drawLine(0f, 6 * d + 45 * d * i, 0f, 45 * d + 45 * d * i, paintLine)

                drawText(
                    "${convertUTCToString(listStop[i].time, "HH:mm")}",
                    10 * d,
                    5 * d + 45 * d * i,
                    paintHour
                )

                drawText(
                    formatSimpleAddress(
                        context,
                        listStop[i].location?.latitude,
                        listStop[i].location?.longitude,
                        false
                    ),
                    10 * d,
                    25 * d + 45 * d * i,
                    paintAddress
                )
            }
        }
    }

}