/*
 *  Copyright 2011 Robert Brandner (robert.brandner@gmail.com) 
 */
package panama.android.fingercolors;

import java.io.BufferedOutputStream;
import java.util.Iterator;
import java.util.Stack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * @author ridcully
 *
 */
public class CanvasView extends View {
	
	private Context		mContext;
    private Bitmap  	mBitmap;
    private Bitmap		mPrevBitmap;
    private Bitmap		mUndoBackgroundBitmap;
    private Canvas  	mCanvas;
    private Canvas		mPrevCanvas;
    private Canvas		mUndoBackgroundCanvas;
    private Path    	mPath;
    private MaskFilter	mBlurFilter;
    private Paint   	mBitmapPaint;
    private Paint		mPaint;
    private BrushView 	mBrushView;
    private Toast		mBrushToast;
    
    private int 		mColor = 0xFF000000;
    private int[]		mColorRGB = new int[] {0, 0, 0};	// original RGB values of color, used to change brightness
    private int			mAlpha = 255;
    private int 		mSize = 16;
    private float		mBlur = 0.05f;	// factor to multiply (255-alpha) with for blur-radius	
    private int			mBrightness = 0;
    private Rect 		mDirtyRegion = new Rect(0, 0, 0, 0);
    private float		mLastX = -1;
    private float		mLastY = -1;

    private Stack<UndoRedoStep>	mUndoStack = new Stack<UndoRedoStep>();
    private Stack<UndoRedoStep>	mRedoStack = new Stack<UndoRedoStep>();

    private boolean		mColorPickMode = false;
    
    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint();
        mBitmapPaint.setDither(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mSize);
        setBlur();

        LayoutInflater inflater = ((Main)mContext).getLayoutInflater();
        View layout = inflater.inflate(R.layout.brushtoast, (ViewGroup) findViewById(R.id.brushtoast_layout_root));        
        
    	mBrushToast = new Toast(this.getContext());
    	mBrushToast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, -40);
    	mBrushToast.setDuration(Toast.LENGTH_SHORT);
        mBrushToast.setView(layout);
        
        mBrushView = (BrushView)layout.findViewById(R.id.brushview);
        mBrushView.setPaint(mPaint);
        
        setFocusableInTouchMode(true);	
    }
    
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(width, height);
	}    

	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mPrevBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mUndoBackgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPrevCanvas = new Canvas(mPrevBitmap);
        mUndoBackgroundCanvas = new Canvas(mUndoBackgroundBitmap);
        mBitmap.eraseColor(Color.LTGRAY);
        mPrevBitmap.eraseColor(Color.LTGRAY);
        mUndoBackgroundBitmap.eraseColor(Color.LTGRAY);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
		mCanvas.drawBitmap(mPrevBitmap, 0, 0, mBitmapPaint);
		mCanvas.drawPath(mPath, mPaint);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				undo();
				return true;
		}
		return false;
	}
    
    
	@Override
    public boolean onTouchEvent(MotionEvent event) {
		
		if (mColorPickMode) {
			// hide palette (but button stays pressed)
			View v = ((Main)mContext).findViewById(R.id.colorPalette);
			v.setVisibility(View.GONE);
			return handleColorPickTouchEvent(event);
		}

        float x = event.getX();
        float y = event.getY();
        
        mBrushToast.cancel();

        switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
        		((Main)mContext).hidePalettes();
        		mPath.reset();
        		mPath.moveTo(x, y);
        		mPath.lineTo(x+0.5f, y+0.5f);
        		mDirtyRegion.set((int)x, (int)y, (int)x, (int)y);
            	adjustDirtyRegion(x, y);
                invalidate(mDirtyRegion);
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
            	boolean redraw = false;	// only invalidate if actually something gets drawn
            	int historySize = event.getHistorySize();
            	for (int i = 0; i < historySize; i++) {
            		float historicalX = event.getHistoricalX(i);
            		float historicalY = event.getHistoricalY(i);
            		
                    if (enoughDistance(historicalX, historicalY, mLastX, mLastY)) {
                    	mPath.lineTo(historicalX, historicalY);
                    	adjustDirtyRegion(historicalX, historicalY);	// TODO hier wäre eine einfachere Methode ausreichend (ohne Blur-Ränder usw.) -- vielleicht ein adjustDirtyRegion mit einem Rect als Parameter machen
                    	redraw = true;
                    	mLastX = historicalY;
                    	mLastY = historicalY;
                    }
            	}
                if (enoughDistance(x, y, mLastX, mLastY)) {
                	mPath.lineTo(x, y);
                	adjustDirtyRegion(x, y);
                	redraw = true;
                    mLastX = x;
                    mLastY = y;
                }
                if (redraw) {
                	invalidate(mDirtyRegion);
                }
                break;
            case MotionEvent.ACTION_UP:
            	mPrevCanvas.drawPath(mPath, mPaint);
            	mUndoStack.push(new UndoRedoStep(mPath, mPaint));
            	mRedoStack.clear();
            	mPath.reset();
                invalidate(mDirtyRegion);
                mLastX = mLastY = -1;
                break;
        }
        return true;
    }

	public void reset(int color) {
    	mBitmap.eraseColor(color);
    	mPrevBitmap.eraseColor(color);
    	mUndoBackgroundBitmap.eraseColor(color);
    	mUndoStack.clear();
    	mRedoStack.clear();
    	invalidate();
    }
    
    public void reset(Bitmap bm) {
    	if ((bm.getWidth() > bm.getHeight())) {	// landscape? rotate 
    		float scaleX = ((float)mBitmap.getWidth())/bm.getHeight();
    		float scaleY = ((float)mBitmap.getHeight())/bm.getWidth();
    		float scale = Math.max(scaleX, scaleY);
    		bm = Bitmap.createScaledBitmap(bm, (int)(bm.getWidth()*scale + 0.5), (int)(bm.getHeight()*scale + 0.5), true);
        	Matrix matrix = new Matrix();
        	matrix.postRotate(90f);
        	bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
    	} else {
    		float scaleX = ((float)mBitmap.getWidth())/bm.getWidth();
    		float scaleY = ((float)mBitmap.getHeight())/bm.getHeight();
    		float scale = Math.max(scaleX, scaleY);
    		bm = Bitmap.createScaledBitmap(bm, (int)(bm.getWidth()*scale + 0.5), (int)(bm.getHeight()*scale + 0.5), true);
    	}
    	int left = -(bm.getWidth()-mBitmap.getWidth())/2;
    	int top = -(bm.getHeight()-mBitmap.getHeight())/2;
    	mCanvas.drawBitmap(bm, left, top, mBitmapPaint);
    	mPrevCanvas.drawBitmap(bm, left, top, mBitmapPaint);
    	mUndoBackgroundCanvas.drawBitmap(bm, left, top, mBitmapPaint);
    	mUndoStack.clear();
    	mRedoStack.clear();
    	bm.recycle();
    	invalidate();
    }
    
    public void setColor(int a, int r, int g, int b) {
    	setColor(Color.argb(a, r, g, b));
    }
    
    public void setColor(int color) {
    	mColorRGB[0] = Color.red(color);
    	mColorRGB[1] = Color.green(color);
    	mColorRGB[2] = Color.blue(color);
    	mBrightness = brightness(mColorRGB);
    	mPaint.setColor(color);
    	mPaint.setAlpha(mAlpha);
    	mColor = mPaint.getColor();
    	toastBrush();
    }
    
    public void setBrushSize(int size, boolean show) {
    	size = Math.max(2, size);
    	size = Math.min(128, size);
    	mSize = size;
    	mPaint.setStrokeWidth(size);
    	if (show) {
    		toastBrush();
    	}
    }
    
    public void setBrushSize(int size) {
    	setBrushSize(size, true);
    }
    
    public void increaseBrushSize() {
    	setBrushSize(mSize*2/*SIZE_STEP*/);
    }

    public void decreaseBrushSize() {
    	setBrushSize(mSize/2);
    }
    
    public void setAlpha(int alpha) {
    	mAlpha = alpha;
    	mAlpha = Math.min(255, mAlpha);
    	mAlpha = Math.max(5, mAlpha);
    	mPaint.setAlpha(mAlpha);
    	mColor = mPaint.getColor();
    	setBlur();
    	toastBrush();
    }
    
    public void increaseAlpha(int delta) {
    	setAlpha(mPaint.getAlpha()+delta);
    }
    
    public void decreaseAlpha(int delta) {
    	setAlpha(mPaint.getAlpha()-delta);
    }

    public void setBrightness(int brightness) {
    	mBrightness = brightness;
    	mBrightness = Math.min(mBrightness, 255);
    	mBrightness = Math.max(mBrightness, -255);
    	int[] rgb = new int[3];
    	for (int i=0; i< 3; i++) {
    		rgb[i] = mColorRGB[i] + mBrightness;
        	rgb[i] = Math.min(rgb[i], 255);
        	rgb[i] = Math.max(rgb[i], 0);
    	}
    	mPaint.setARGB(mAlpha, rgb[0], rgb[1], rgb[2]);
    	mColor = mPaint.getColor();
    	toastBrush();
    	
    }
    
    public void increaseBrightness(int delta) {
    	setBrightness(mBrightness+delta);
    }
    
    public void decreaseBrightness(int delta) {
    	setBrightness(mBrightness-delta);
    }
    
    private void toastBrush() {
    	mBrushView.requestLayout();
    	mBrushView.invalidate();
    	mBrushToast.show();
    }
    
    private void setBlur() {
		float blurRadius = (255-mAlpha)*mBlur;
    	if (mAlpha == 255 || blurRadius < 1.0) {
    		mBlurFilter = null;
    		mPaint.setMaskFilter(null);
    	} else {
            mBlurFilter = new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL);
    		mPaint.setMaskFilter(mBlurFilter);
    	}
    }

    private void adjustDirtyRegion(float x, float y) {
		float blurRadius = (255-mAlpha)*mBlur;
    	float strokeWidthHalf = (mSize/2f+blurRadius)+4;	// Hälfte der Strichbreite + blurRadius +4 zur Sicherheit wg. Blur und Antialias
    	mDirtyRegion.left   = (int)FloatMath.floor(Math.min(mDirtyRegion.left, x-strokeWidthHalf));
    	mDirtyRegion.right  = (int)FloatMath.ceil(Math.max(mDirtyRegion.right, x+strokeWidthHalf));
    	mDirtyRegion.top    = (int)FloatMath.floor(Math.min(mDirtyRegion.top, y-strokeWidthHalf));
    	mDirtyRegion.bottom = (int)FloatMath.ceil(Math.max(mDirtyRegion.bottom, y+strokeWidthHalf));
    }

    private void redrawUndoStack() {
    	mPrevCanvas.drawBitmap(mUndoBackgroundBitmap, 0, 0, mBitmapPaint);
    	//long start = SystemClock.uptimeMillis();
    	for (Iterator<UndoRedoStep> it = mUndoStack.iterator(); it.hasNext(); ) {
    		UndoRedoStep step = it.next();
    		mPrevCanvas.drawPath(step.path, step.paint);
    	}
    	//long stop = SystemClock.uptimeMillis();
    	//Log.d(Main.LOG_TAG, "redraw undo stack in "+(stop-start)+" ms");
    	invalidate();
    }
    
    public void undo() {
    	if (mUndoStack.isEmpty()) {
    		return;
    	}
    	mRedoStack.push(mUndoStack.pop());
    	redrawUndoStack();
	}
    
    public void redo() {
    	if (mRedoStack.isEmpty()) {
    		return;
    	}
    	mUndoStack.push(mRedoStack.pop());
    	redrawUndoStack();
	}

	public boolean saveBitmap(BufferedOutputStream outStream) {
		return mBitmap.compress(CompressFormat.PNG, 100, outStream);
	}
    
	public void enableColorPickMode(boolean enabled) {
		mColorPickMode = enabled;
	}
	
    // compute perceived brightness of a color in the range of 0 to 255
    // from http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
    private int brightness(int[] rgb) {
    	return (int)FloatMath.sqrt(rgb[0]*rgb[0]*0.241f+
    						  rgb[1]*rgb[1]*0.691f+
    						  rgb[2]*rgb[2]*0.068f);
    }
    
	/** 
	 * check if distance between points (x1,y1) and (x2, y2) is big enough to draw a line in between. 
	 * tests if quadratic distance is > 2
	 */
    private boolean enoughDistance(float x1, float y1, float x2, float y2) {
		return (x1-x2)*(x1-x2)+(y1-y2)*(y1-y2) > 2;
	}    
    
	private boolean handleColorPickTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        x = Math.max(0, x);
        x = Math.min(x, mBitmap.getWidth()-1);
        y = Math.max(0, y);
        y = Math.min(y, mBitmap.getHeight()-1);
        switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            	int selectedColor = mBitmap.getPixel(x, y);
            	setColor(selectedColor);
                break;
            case MotionEvent.ACTION_UP:
            	mBrushToast.cancel();
            	((Main)mContext).hidePalettes();
            	break;
        }
        return true;
	}    
    
    private class UndoRedoStep {
    	public Path path;
    	public Paint paint;
    	
    	public UndoRedoStep(Path originalPath, Paint originalPaint) {
    		path = new Path(originalPath);
    		paint = new Paint(originalPaint);
    	}
    }
}
