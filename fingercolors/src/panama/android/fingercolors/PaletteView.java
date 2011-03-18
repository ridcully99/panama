/*
 * Copyright 2011 Robert Brandner (robert.brandner@gmail.com)
 */
package panama.android.fingercolors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author ridcully
 *
 */
public class PaletteView extends View {

	private final static int COLOR_BAND_HEIGHT = 192;
	private final static int[] COLORS = new int[] { 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF};
	
	
    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Paint   mBitmapPaint;
    private Paint	mPaint;
    private Paint	mMarkerPaint;
    private int		mSelectedColor = Color.BLACK;
    private int		mSelectionX = 0;
    private int		mSelectionY = 0;
    
	public PaletteView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        mBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mMarkerPaint = new Paint();
        mMarkerPaint.setAntiAlias(true);
        mMarkerPaint.setStyle(Paint.Style.STROKE);
        mMarkerPaint.setStrokeWidth(2);
        
        mPaint = new Paint();
        mPaint.setDither(true);
	}
	
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int colorWidth = (w*7)/8;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(Color.BLACK);
        mPaint.setStyle(Style.FILL);
        // vertical gray band
        mPaint.setShader(new LinearGradient(0, 0, 0, h, 0xFFFFFFFF, 0xFF000000, TileMode.CLAMP));
        mCanvas.drawRect(colorWidth+2, 0, w, h, mPaint);
        // horizontal rainbow gradient
        mPaint.setShader(new LinearGradient(0, 0, colorWidth, 0, COLORS, null, TileMode.CLAMP));
        mCanvas.drawRect(0, 0, colorWidth, h, mPaint);
        // vertical white to invisible gradient to half
        mPaint.setShader(new LinearGradient(0, 0, 0, COLOR_BAND_HEIGHT/2, 0xFFFFFFFF, 0x00FFFFFF, TileMode.CLAMP));
        mCanvas.drawRect(0, 0, colorWidth, COLOR_BAND_HEIGHT/2, mPaint);
        // vertical invisible to black gradient form half
        mPaint.setShader(new LinearGradient(0, COLOR_BAND_HEIGHT/2, 0, h, 0x00000000, 0xFF000000, TileMode.CLAMP));
        mCanvas.drawRect(0, COLOR_BAND_HEIGHT/2, colorWidth, h, mPaint);
        
        mSelectionX = 0;
        mSelectionY = h;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        mMarkerPaint.setColor(0x80000000);	// transparent black
        mMarkerPaint.setStrokeWidth(5);
        canvas.drawCircle(mSelectionX, mSelectionY, 8, mMarkerPaint);
        mMarkerPaint.setColor(0xFFEEEEEE);
        mMarkerPaint.setStrokeWidth(3);
        canvas.drawCircle(mSelectionX, mSelectionY, 8, mMarkerPaint);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
    	setMeasuredDimension(parentWidth, COLOR_BAND_HEIGHT);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        x = Math.max(0, x);
        x = Math.min(x, mBitmap.getWidth()-1);
        y = Math.max(0, y);
        y = Math.min(y, mBitmap.getHeight()-1);
    	CanvasView canvas = (CanvasView)((Main)this.getContext()).findViewById(R.id.canvas);	// TODO slow?
        switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            	mSelectedColor = mBitmap.getPixel(x, y);
            	mSelectionX = x;
            	mSelectionY = y;
            	canvas.setColor(mSelectedColor);
            	invalidate();
                break;
            case MotionEvent.ACTION_UP:
            	canvas.setColor(mSelectedColor);
            	canvas.enableColorPickMode(false);
            	((Main)this.getContext()).hidePalettes();
                break;
        }
        return true;
    }

}