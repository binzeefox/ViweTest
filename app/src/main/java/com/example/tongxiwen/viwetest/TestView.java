package com.example.tongxiwen.viwetest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;
import java.util.logging.SimpleFormatter;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by tong.xiwen on 2017/7/31.
 */
public class TestView extends View {

    public static final int SHOW_PERCENT = 0;
    public static final int SHOW_PROGRASS = 1;
    private int displayStyle;   // 显示模式
    private String maxText; // 最大百分比显示文字

    private int X;  // 触摸X
    private int Y;  // 触摸Y
    private PointF center;  // 圆心

    private RectF mainArea; // 主区域所在矩形
    private Region mainRegion; // 主操作区
    private Paint mPaint; // 主画笔
    private int arc;    // 角度
    private int arcWidth;   // 圆弧宽度
    private int radio;  // 圆弧半径
    private boolean skipCounting;   // 是否跳过计算角度（设定中）
    private boolean isGradient; // 是否为渐变色进度
    private float textWidth;    // 文字宽度

    // 用户数据
    private int max;    // 最大值
    private double cent;    // 百分比乘数

    public TestView(Context context) {
        this(context, null, 0, 0);
    }

    public TestView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public TestView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TestView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        max = 100;
        cent = 0;
        arcWidth = 0;

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);

        displayStyle = SHOW_PERCENT;
        skipCounting = true;
        isGradient = false;

        if (attrs != null){
            setFromAttr(context,attrs);
        }
    }

    /**
     * 通过xml属性初始化
     * @param context
     * @param attrs
     */
    private void setFromAttr(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TestView);
        isGradient = array.getBoolean(R.styleable.TestView_gradient, false);
        maxText = array.getString(R.styleable.TestView_end_text);
        if (maxText == null){
            maxText = "上限";
        }
        switch (array.getInt(R.styleable.TestView_showing_style, -1)){
            case 1:
                displayStyle = SHOW_PROGRASS;
                break;
            default:
                displayStyle = SHOW_PERCENT;
                break;
        }
        arcWidth = array.getInt(R.styleable.TestView_arc_width, arcWidth);
        max = array.getInt(R.styleable.TestView_max, max);
    }

    /**
     * 测量
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            height = width/2;
            setMeasuredDimension(width, height + getPaddingTop() + getPaddingBottom());
        }

        int left = getPaddingLeft();
        int right = width - getPaddingRight();
        int top = getPaddingTop();
        int bottom = height - getPaddingBottom();
        radio = (width - getPaddingLeft() - getPaddingRight()) / 2;

        mainArea = new RectF(left, top, right, bottom);
        mainRegion = new Region((int) mainArea.left, (int) mainArea.top, (int) mainArea.right, radio);
    }

    /**
     * 绘制
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 在画布上绘制扇形
        if (!skipCounting) {    // 如果不跳过计算，则通过点计算角度
            arc = countAcr(X, Y);
        }
//        Log.d("arc", String.valueOf(arc));

        // 扇形
        if (arcWidth == 0){
            arcWidth = (int) (mainArea.right - mainArea.left)/11;
        }
        mPaint.setStrokeWidth(arcWidth);
        Path arcPath = new Path();
        RectF mainRect = new RectF( // 主区域矩形
                mainArea.left + arcWidth / 2,
                mainArea.top + arcWidth / 2,
                mainArea.right - arcWidth / 2,
                mainArea.right - arcWidth / 2);

        if (isGradient) {   // 判断渐变色填充
            LinearGradient gradient = new LinearGradient(
                    mainRect.left,
                    mainRect.bottom,
                    mainRect.right,
                    mainRect.bottom,
                    new int[]{Color.BLUE, Color.RED},
                    null, Shader.TileMode.CLAMP);
            mPaint.setShader(gradient);
        }

        arcPath.addArc(mainRect, -180, arc);    // 从-180度开始到0度
        center = new PointF(mainRect.centerX(), mainRect.centerY());

        // 开启计算角度开关
        skipCounting = false;


        // 绘制文字
        drawDisplay(canvas);

        // 绘制辅助线
        drawGideline(canvas);

        // 绘制扇形
        canvas.drawPath(arcPath, mPaint);
    }

    /**
     * 绘制辅助线
     * @param canvas
     */
    private void drawGideline(Canvas canvas) {
        canvas.save();
        Paint linePaint = new Paint();  //辅助线画笔
        linePaint.setStrokeWidth(2);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLACK);

        for (int i = 0; i < 181; i += 30) {
            canvas.drawLine(mainArea.left+(mainArea.right-mainArea.left) /5, mainArea.bottom, mainArea.left, mainArea.bottom, linePaint);
            canvas.rotate(30, mainArea.centerX(), mainArea.bottom);
        }
        canvas.restore();

        // 绘制底线
        canvas.drawLine(mainArea.left,mainArea.bottom,mainArea.right,mainArea.bottom,linePaint);
        // 绘制扇形区域
        Path outline = new Path();
        RectF outlineRect = new RectF(
                mainArea.left,
                mainArea.top,
                mainArea.right,
                mainArea.bottom*2);

        outline.arcTo(outlineRect, 180, 359);
        canvas.drawPath(outline, linePaint);

        outlineRect.left += arcWidth;
        outlineRect.right -= arcWidth;
        outlineRect.top += arcWidth;
        outlineRect.bottom -= arcWidth;
        outline.arcTo(outlineRect, 180, 359);
        canvas.drawPath(outline, linePaint);
    }

    /**
     * 绘制文字
     *
     * @param canvas
     */
    private void drawDisplay(Canvas canvas) {
        Paint textPaint = new Paint();  // 写字笔
        textPaint.setTextSize(radio / 6);
        DecimalFormat formatter = new DecimalFormat("00.00");
        String str;

        switch (displayStyle) {

            case SHOW_PERCENT:
                str = formatter.format(getCent() * 100) + "%";
                if ("100.00%".equals(str)) {
                    str = maxText;
                }
                break;
            case SHOW_PROGRASS:
                str = String.valueOf(getCurrent());
                if (getCent() == 1){
                    str = "最大值" + str;
                }
                break;
            default:
                str = "";
        }
        textWidth = textPaint.measureText(str);//获得字符串的宽度
        textWidth = textWidth / 2;
        canvas.drawText(str, center.x - textWidth, center.y - radio/24, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == ACTION_UP) {
            if (!mainRegion.contains(x, y)) {
                return true;
            }
        }
        if (event.getAction() == ACTION_MOVE) {
            if (mainRegion.contains(x, y)) {
                X = x;
                Y = y;
                if (X > center.x && Y > center.y) {
                    arc = 359;
                    skipCounting = true;
                } else if (X < center.x && Y > center.y) {
                    arc = 180;
                    skipCounting = true;
                }
            } else {
                int end = arc;
                if (arc < 90) {
                    end = 0;
                } else if (arc > 90) {
                    end = 180;
                }
                ValueAnimator animator = ValueAnimator.ofInt(arc, end);
                animator.setDuration(74);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        arc = (int) valueAnimator.getAnimatedValue();
                        skipCounting = true;
                        invalidate();
                    }
                });
                animator.start();
                return true;
            }
        }
        invalidate();
        return true;
    }

    /**
     * 计算角度
     *
     * @param x
     * @param y
     * @return
     */
    private int countAcr(float x, float y) {

        int result = 0;

        y = y - center.y;
        x = x - center.x;
        double tan = y / x;
        int degree = (int) Math.toDegrees(Math.atan(tan));

        // 判断象限
        if (x > 0 && y > 0) {    // ↘
            result = degree;
        }
        if (x < 0 && y > 0) {    // ↙
            result = degree + 180;
        }
        if (x < 0 && y < 0) {    // ↖
            result = degree + 180;
        }
        if (x > 0 && y < 0) {    // ↗
            result = degree + 360;
        }
        return result - 180;
    }

    /**
     * 存取最大值
     *
     * @param max
     */
    public void setMax(int max) {
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    /**
     * 设置当前值
     *
     * @param current
     */
    public void setCurrent(int current) {
        this.cent = (float)current / (float) max;
        arc = (int) (180 * cent);
        skipCounting = true;
        invalidate();
    }

    /**
     * 返回当前值
     *
     * @return
     */
    public int getCurrent() {
        return (int) (getCent() * max);
    }

    /**
     * 设置比例
     *
     * @param cent
     */
    public void setCent(double cent) {
        arc = (int) (180 * cent);
        invalidate();
    }

    /**
     * 返回比例
     *
     * @return
     */
    public double getCent() {

        double cent = (double) arc / 180d;
        return cent;
    }

    /**
     * 设置弧线宽度
     *
     * @param arcWidth
     */
    public void setArcWidth(int arcWidth) {
        this.arcWidth = arcWidth;
    }

    /**
     * 存取显示方式
     *
     * @param style
     */
    public void setDisplayStyle(int style) {
        this.displayStyle = style;
    }

    public int getDisplayStyle() {
        return displayStyle;
    }

    /**
     * 设置100%时显示文字
     * @param maxText
     */
    public void setMaxText(CharSequence maxText){
        if (maxText == null){
            maxText = "上限";
        }
        this.maxText = (String) maxText;
    }

    /**
     * 设置是否渐变
     * @param isGradient
     */
    public void setGradient(boolean isGradient){
        this.isGradient = isGradient;
    }
}
