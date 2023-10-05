package com.sherdle.universal.util.layout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.adsfall.R;

public class RoundedImageView extends AppCompatImageView {

  private Path path;
  private RectF rect;

  public RoundedImageView(Context context) {
    super(context);
    init();
  }

  public RoundedImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    path = new Path();

  }

  @Override
  protected void onDraw(Canvas canvas) {
    rect = new RectF(0, 0, this.getWidth(), this.getHeight());
    float radius = getResources().getDimension(R.dimen.thumbnail_radius);
    path.addRoundRect(rect, radius, radius, Path.Direction.CW);
    canvas.clipPath(path);
    canvas.clipRect(rect);
    super.onDraw(canvas);
  }
}
