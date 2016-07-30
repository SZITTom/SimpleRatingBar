package com.iarcuschin.simpleratingbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

public class SimpleRatingBar extends View {

  public enum Gravity {
    Left(0),
    Right(1);

    int id;
    Gravity(int id) {
      this.id = id;
    }

    static Gravity fromId(int id) {
      for (Gravity f : values()) {
        if (f.id == id) return f;
      }
      // default value
      return Left;
    }
  }

  // Configurable variables
  @ColorInt int starsColor;
  @ColorInt int fillColor;
  @ColorInt int backgroundColor;
  int numberOfStars;
  float starsSeparation;
  float starSize;
  float maxStarSize;
  float rating;
  boolean isIndicator;
  Gravity gravity;
  float borderWidth;

  // Internal variables
  private Paint paintStar;
  private Paint paintStarFill;
  private Paint paintBackground;
  private Path path;
  private float defaultStarSize;
  private OnRatingBarChangeListener listener;
  // Internal variables used to speed up drawing. They all depend on  the value of starSize
  private float bottomFromMargin;
  private float triangleSide;
  private float half;
  private float tipVerticalMargin;
  private float tipHorizontalMargin;
  private float innerUpHorizontalMargin;
  private float innerBottomHorizontalMargin;
  private float innerBottomVerticalMargin;
  private float innerCenterVerticalMargin;
  private float[] starVertex;
  private RectF starsDrawingSpace;

  // in order to delete some drawing, and keep transparency
  // http://stackoverflow.com/a/21865858/2271834
  private Canvas internalCanvas;
  private Bitmap internalBitmap;

  public SimpleRatingBar(Context context) {
    super(context);

    initView();
  }

  public SimpleRatingBar(Context context, AttributeSet attrs) {
    super(context, attrs);

    parseAttrs(attrs);

    initView();
  }

  public SimpleRatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    parseAttrs(attrs);

    initView();
  }

  private void initView() {
    paintStar = new Paint(Paint.ANTI_ALIAS_FLAG);
    path = new Path();
    paintStar.setAntiAlias(true);
    paintStar.setDither(true);
    paintStar.setStyle(Paint.Style.STROKE);
    paintStar.setStrokeJoin(Paint.Join.ROUND);
    paintStar.setStrokeCap(Paint.Cap.ROUND);
    paintStar.setPathEffect(new CornerPathEffect(6));
    paintStar.setStrokeWidth(borderWidth);
    paintStar.setColor(starsColor);

    paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBackground.setStyle(Paint.Style.FILL_AND_STROKE);
    paintBackground.setStrokeWidth(1);
    paintBackground.setColor(backgroundColor);
    if (backgroundColor == Color.TRANSPARENT) {
      paintBackground.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    paintStarFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintStarFill.setStyle(Paint.Style.FILL_AND_STROKE);
    paintStarFill.setStrokeWidth(0);
    paintStarFill.setColor(fillColor);

    defaultStarSize = applyDimension(COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
  }

  private void parseAttrs(AttributeSet attrs) {
    TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.SimpleRatingBar);

    starsColor = arr.getColor(R.styleable.SimpleRatingBar_starsColor, getResources().getColor(R.color.golden_stars));
    fillColor = arr.getColor(R.styleable.SimpleRatingBar_fillColor, starsColor);
    backgroundColor = arr.getColor(R.styleable.SimpleRatingBar_backgroundColor, Color.TRANSPARENT);
    numberOfStars = arr.getInteger(R.styleable.SimpleRatingBar_numberOfStars, 5);

    float starsSeparationDp = arr.getDimension(R.styleable.SimpleRatingBar_starsSeparation, 4);
    starsSeparation = applyDimension(COMPLEX_UNIT_DIP, starsSeparationDp, getResources().getDisplayMetrics());
    maxStarSize = arr.getDimensionPixelSize(R.styleable.SimpleRatingBar_maxStarSize, -1);
    starSize = arr.getDimensionPixelSize(R.styleable.SimpleRatingBar_starSize, -1);
    borderWidth = arr.getInteger(R.styleable.SimpleRatingBar_borderWidth, 5);

    rating = arr.getFloat(R.styleable.SimpleRatingBar_rating, 0f);
    isIndicator = arr.getBoolean(R.styleable.SimpleRatingBar_isIndicator, false);
    gravity = Gravity.fromId(arr.getInt(R.styleable.SimpleRatingBar_gravity, Gravity.Left.id));

    arr.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int width;
    int height;

    //Measure Width
    if (widthMode == MeasureSpec.EXACTLY) {
      //Must be this size
      width = widthSize;
    } else if (widthMode == MeasureSpec.AT_MOST) {
      //Can't be bigger than...
      if (starSize != -1) {
        // user specified a specific star size, so there is a desired width
        int desiredWidth = Math.round(starSize * numberOfStars + starsSeparation * (numberOfStars -1));
        width = Math.min(desiredWidth, widthSize);
      } else if (maxStarSize != -1) {
        // user specified a max star size, so there is a desired width
        int desiredWidth = Math.round(maxStarSize * numberOfStars + starsSeparation * (numberOfStars -1));
        width = Math.min(desiredWidth, widthSize);
      } else {
        // using defaults
        int desiredWidth = Math.round(defaultStarSize * numberOfStars + starsSeparation * (numberOfStars -1));
        width = Math.min(desiredWidth, widthSize);
      }
    } else {
      //Be whatever you want
      if (starSize != -1) {
        // user specified a specific star size, so there is a desired width
        int desiredWidth = Math.round(starSize * numberOfStars + starsSeparation * (numberOfStars -1));
        width = desiredWidth;
      } else if (maxStarSize != -1) {
        // user specified a max star size, so there is a desired width
        int desiredWidth = Math.round(maxStarSize * numberOfStars + starsSeparation * (numberOfStars -1));
        width = desiredWidth;
      } else {
        // using defaults
        int desiredWidth = Math.round(defaultStarSize * numberOfStars + starsSeparation * (numberOfStars -1));
        width = desiredWidth;
      }
    }

    //Measure Height
    if (heightMode == MeasureSpec.EXACTLY) {
      //Must be this size
      height = heightSize;
    } else if (heightMode == MeasureSpec.AT_MOST) {
      //Can't be bigger than...
      if (starSize != -1) {
        // user specified a specific star size, so there is a desired width
        int desiredHeight = Math.round(starSize);
        height = Math.min(desiredHeight, heightSize);
      } else if (maxStarSize != -1) {
        // user specified a max star size, so there is a desired width
        int desiredHeight = Math.round(maxStarSize);
        height = Math.min(desiredHeight, heightSize);
      } else {
        // using defaults
        int desiredHeight = Math.round(defaultStarSize);
        height = Math.min(desiredHeight, heightSize);
      }
    } else {
      //Be whatever you want
      if (starSize != -1) {
        // user specified a specific star size, so there is a desired width
        int desiredHeight = Math.round(starSize);
        height = desiredHeight;
      } else if (maxStarSize != -1) {
        // user specified a max star size, so there is a desired width
        int desiredHeight = Math.round(maxStarSize);
        height = desiredHeight;
      } else {
        // using defaults
        int desiredHeight = Math.round(defaultStarSize);
        height = desiredHeight;
      }
    }

    if (starSize == -1) {
      starSize = calculateBestStarSize(width, height);
    }
    performStarSizeAssociatedCalculations(width, height);
    //MUST CALL THIS
    setMeasuredDimension(width, height);
  }

  private float calculateBestStarSize(int width, int height) {
    if (maxStarSize != -1) {
      float desiredTotalSize = maxStarSize * numberOfStars + starsSeparation * (numberOfStars - 1);
      if (desiredTotalSize > width) {
        // we need to shrink the size of the stars
        return (width - starsSeparation * (numberOfStars - 1)) / numberOfStars;
      } else {
        return maxStarSize;
      }
    } else {
      // expand the most we can
      return (width - starsSeparation * (numberOfStars - 1)) / numberOfStars;
    }
  }

  private void performStarSizeAssociatedCalculations(int width, int height) {
    float totalStarsSize = starSize * numberOfStars + starsSeparation * (numberOfStars -1);
    float startingX = width/2 - totalStarsSize/2;
    float startingY = 0;
    starsDrawingSpace = new RectF(startingX, startingY, startingX + totalStarsSize, startingY + starSize);

    bottomFromMargin = starSize*0.2f;
    triangleSide = starSize*0.35f;
    half = starSize * 0.5f;
    tipVerticalMargin = starSize * 0.05f;
    tipHorizontalMargin = starSize * 0.03f;
    innerUpHorizontalMargin = starSize * 0.38f;
    innerBottomHorizontalMargin = starSize * 0.32f;
    innerBottomVerticalMargin = starSize * 0.55f;
    innerCenterVerticalMargin = starSize * 0.27f;

    starVertex = new float[] {
        tipHorizontalMargin, innerUpHorizontalMargin, // top left
        tipHorizontalMargin + triangleSide, innerUpHorizontalMargin,
        half, tipVerticalMargin, // top tip
        starSize - tipHorizontalMargin - triangleSide, innerUpHorizontalMargin,
        starSize - tipHorizontalMargin, innerUpHorizontalMargin, // top right
        starSize - innerBottomHorizontalMargin, innerBottomVerticalMargin,
        starSize - bottomFromMargin, starSize - tipVerticalMargin, // bottom right
        half, starSize - innerCenterVerticalMargin,
        bottomFromMargin, starSize - tipVerticalMargin, // bottom left
        innerBottomHorizontalMargin, innerBottomVerticalMargin,
        tipHorizontalMargin, innerUpHorizontalMargin, // top left
    };
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (internalBitmap != null) {
      // avoid leaking memory after losing the reference
      internalBitmap.recycle();
    }

    if (w > 0 && h > 0) {
      internalBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      internalBitmap.eraseColor(Color.TRANSPARENT);
      internalCanvas = new Canvas(internalBitmap);
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    int height = getHeight();
    int width = getWidth();

    if (width == 0 || height == 0) {
      return;
    }

    internalCanvas.drawColor(Color.argb(0, 0, 0, 0));

    float remainingTotalRaiting = rating;
    if (gravity == Gravity.Left) {
      float startingX = starsDrawingSpace.left;
      float startingY = starsDrawingSpace.top;
      for (int i = 0; i < numberOfStars; i++) {
        if (remainingTotalRaiting >= 1) {
          drawStar(internalCanvas, startingX, startingY, 1f, gravity);
          remainingTotalRaiting -= 1;
        } else {
          drawStar(internalCanvas, startingX, startingY, remainingTotalRaiting, gravity);
          remainingTotalRaiting = 0;
        }
        startingX += starSize;
        if (i < numberOfStars -1) {
          drawSeparator(internalCanvas, startingX, startingY);
          startingX += starsSeparation;
        }
      }
    } else {
      float startingX = starsDrawingSpace.right - starSize;
      float startingY = starsDrawingSpace.top;
      for (int i = 0; i < numberOfStars; i++) {
        if (remainingTotalRaiting >= 1) {
          drawStar(internalCanvas, startingX, startingY, 1f, gravity);
          remainingTotalRaiting -= 1;
        } else {
          drawStar(internalCanvas, startingX, startingY, remainingTotalRaiting, gravity);
          remainingTotalRaiting = 0;
        }
        if (i < numberOfStars -1) {
          startingX -= starsSeparation;
          drawSeparator(internalCanvas, startingX, startingY);
        }
        startingX -= starSize;
      }
    }

    canvas.drawBitmap(internalBitmap, 0, 0, null);
  }

  private void drawStar(Canvas canvas, float x, float y, float filled, Gravity gravity) {
    // draw fill
    float fill = starSize * filled;
    if (gravity == Gravity.Left) {
      canvas.drawRect(x, y, x + fill, y + starSize, paintStarFill);
      canvas.drawRect(x + fill, y, x + starSize, y + starSize, paintBackground);
    } else {
      canvas.drawRect(x, y, x + starSize - fill, y + starSize, paintBackground);
      canvas.drawRect(x + starSize - fill, y, x + starSize, y + starSize, paintStarFill);
    }

    // clean outside of star
    path.reset();
    path.moveTo(x + starVertex[0], y + starVertex[1]);
    for (int i = 2; i < starVertex.length - 2; i = i + 2) {
      path.lineTo(x + starVertex[i], y + starVertex[i + 1]);
    }
    path.lineTo(x, y + starVertex[starVertex.length - 3]); // reach the closest border
    path.lineTo(x, y + starSize); // bottom left corner
    path.lineTo(x + starSize, y + starSize); // bottom right corner
    path.lineTo(x + starSize, y); // top right corner
    path.lineTo(x, y); // top left corner
    path.lineTo(x, y + starVertex[1]);
    path.close();
    canvas.drawPath(path, paintBackground);

    // finish clean up outside
    path.reset();
    path.moveTo(x + starVertex[0], y + starVertex[1]);
    path.lineTo(x, y + starVertex[1]);
    path.lineTo(x, y + starVertex[starVertex.length - 5]);
    path.lineTo(x + starVertex[starVertex.length - 4], y + starVertex[starVertex.length - 3]);
    path.close();
    canvas.drawPath(path, paintBackground);

    // draw star on top
    path.reset();
    path.moveTo(x + starVertex[0], y + starVertex[1]);
    for(int i = 2; i < starVertex.length; i=i+2) {
      path.lineTo(x + starVertex[i], y + starVertex[i+1]);
    }
    path.close();
    canvas.drawPath(path, paintStar);
  }

  private void drawSeparator(Canvas canvas, float x, float y) {
    canvas.drawRect(x, y, x + starsSeparation, y + starSize, paintBackground);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (isIndicator) {
      return false;
    }

    // check if action is performed above stars
    if (!starsDrawingSpace.contains(event.getX(), event.getY())) {
      return false;
    }

    int action = event.getAction() & MotionEvent.ACTION_MASK;
    switch(action) {
      case MotionEvent.ACTION_DOWN:
        setNewRatingFromTouch(event.getX(), event.getY());
        break;
      case MotionEvent.ACTION_MOVE:
        setNewRatingFromTouch(event.getX(), event.getY());
        break;
      case MotionEvent.ACTION_UP:
        setNewRatingFromTouch(event.getX(), event.getY());
        if (listener != null) {
          listener.onRatingChanged(this, rating, false);
        }
        break;

    }

    invalidate();
    return true;
  }

  private void setNewRatingFromTouch(float x, float y) {
    // normalize x to inside starsDrawinSpace
    if (gravity != Gravity.Left) {
      x = getWidth() - x;
    }
    x = x - starsDrawingSpace.left;
    rating = (float)numberOfStars / starsDrawingSpace.width() * x;
  }


  /* ----------- GETTERS AND SETTERS ----------- */

  /**
   * Sets rating
   * @param rating value between 0 and numberOfStars
   */
  public void setRating(float rating) {
    if (rating > numberOfStars) {
      throw new IllegalArgumentException("rating must be between 0 and numberOfStars");
    }
    this.rating = rating;
    invalidate();
    if (listener != null) {
      listener.onRatingChanged(this, rating, false);
    }
  }

  public float getRating(){
    return rating;
  }



  public boolean isIndicator() {
    return isIndicator;
  }

  public void setIndicator(boolean indicator) {
    isIndicator = indicator;
  }

  public float getMaxStarSize() {
    return maxStarSize;
  }

  public void setMaxStarSize(float maxStarSize) {
    this.maxStarSize = maxStarSize;
    if (starSize > maxStarSize) {
      invalidate();
    }
  }

  public float getStarSize() {
    return starSize;
  }

  public void setStarSize(float starSize) {
    this.starSize = starSize;
    invalidate();
  }

  public float getStarsSeparation() {
    return starsSeparation;
  }

  public void setStarsSeparation(float starsSeparation) {
    this.starsSeparation = starsSeparation;
    invalidate();
  }

  public int getNumberOfStars() {
    return numberOfStars;
  }

  public void setNumberOfStars(int numberOfStars) {
    this.numberOfStars = numberOfStars;
    invalidate();
  }

  public @ColorInt int getBackgroundColor() {
    return backgroundColor;
  }

  @Override public void setBackgroundColor(@ColorInt int backgroundColor) {
    this.backgroundColor = backgroundColor;
    invalidate();
  }

  public @ColorInt int getStarsColor() {
    return starsColor;
  }

  public void setStarsColor(@ColorInt int starsColor) {
    this.starsColor = starsColor;
    invalidate();
  }

  public float getBorderWidth() {
    return borderWidth;
  }

  public void setBorderWidth(float borderWidth) {
    this.borderWidth = borderWidth;
  }

  public @ColorInt int getFillColor() {
    return fillColor;
  }

  public void setFillColor(@ColorInt int fillColor) {
    this.fillColor = fillColor;
  }

  public Gravity getGravity() {
    return gravity;
  }

  public void setGravity(Gravity gravity) {
    this.gravity = gravity;
  }

  public void setOnRatingBarChangeListener(OnRatingBarChangeListener listener) {
    this.listener = listener;
  }

  public interface OnRatingBarChangeListener {

    /**
     * Notification that the rating has changed. Clients can use the
     * fromUser parameter to distinguish user-initiated changes from those
     * that occurred programmatically. This will not be called continuously
     * while the user is dragging, only when the user finalizes a rating by
     * lifting the touch.
     *
     * @param simpleRatingBar The RatingBar whose rating has changed.
     * @param rating The current rating. This will be in the range
     *            0..numStars.
     * @param fromUser True if the rating change was initiated by a user's
     *            touch gesture or arrow key/horizontal trackbell movement.
     */
    void onRatingChanged(SimpleRatingBar simpleRatingBar, float rating, boolean fromUser);

  }
}
