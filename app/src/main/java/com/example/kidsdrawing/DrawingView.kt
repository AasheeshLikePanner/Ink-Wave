package com.example.kidsdrawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.slider.Slider

class DrawingView(context: Context, attr:AttributeSet): View(context, attr) {

    var DrawPath:CustomPath? = null;
    var CanvasBitmap:Bitmap? = null
    var drawpaint: Paint? = null
    var canvasPaint: Paint? = null
    var brushSize:Float = 10f
    var color = Color.WHITE
    var canvas: Canvas? = null
    var paths = ArrayList<CustomPath>()

    init{
        setUpDrawing()
    }

    private fun setUpDrawing() {

        drawpaint = Paint()
        DrawPath = CustomPath(color, brushSize)
        drawpaint!!.color = color
        drawpaint!!.style = Paint.Style.STROKE
        drawpaint!!.strokeJoin = Paint.Join.ROUND
        drawpaint!!.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
        brushSize = 18f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        CanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888 )
        canvas = Canvas(CanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(CanvasBitmap!!, 0f, 0f, canvasPaint)
        for(path in paths){
            DrawPath!!.color = color
            drawpaint!!.strokeWidth = path.brushThickness
            drawpaint!!.color = path.color
            canvas.drawPath(path, drawpaint!!)
        }

        if(!DrawPath!!.isEmpty){
            drawpaint!!.strokeWidth = DrawPath!!.brushThickness
            drawpaint!!.color = DrawPath!!.color
                canvas.drawPath(DrawPath!!, drawpaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event?.x
        val y = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                DrawPath!!.color = color
                DrawPath!!.brushThickness = brushSize

                DrawPath!!.reset()
                DrawPath!!.moveTo(x!!, y!!)
            }
            MotionEvent.ACTION_MOVE ->{
                if (x != null) {
                    if (y != null) {
                        DrawPath!!.lineTo(x, y)
                    }
                }

            }
            MotionEvent.ACTION_UP -> {
                paths.add(DrawPath!!)
                DrawPath = CustomPath(color, brushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setbrushsize(size:Float){
        brushSize = size
    }

    fun setColorChanger(givencolor: Int){
        color = givencolor
    }

    fun UndoLastEntry(){
        if(!paths.isEmpty()){
            paths.removeLast()
            invalidate()
        }
    }


    class CustomPath(var color:Int, var brushThickness:Float): Path() {

    }
}