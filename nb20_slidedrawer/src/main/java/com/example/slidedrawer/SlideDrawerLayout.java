package com.example.slidedrawer;

/*
 * Created by wzhiqiang on 2018/11/13.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by zhy on 15/5/29.
 */
public class SlideDrawerLayout extends ViewGroup {

    private static final String TAG = "SlideDrawerLayout";
    // 中东语言(用于决定布局方向)
    private String[] languages = {"ar", "he", "iw", "ur", "fa"};
    private static final int MIN_FLING_VELOCITY = 400; // 加速度参数
    // 阴影颜色
    public static final int DEFAULT_SHADOW_COLOR = Color.parseColor("#66000000");
    // 默认从左边弹出
    public static final int EDGE_DIRECTION_LEFT = 0;
    public static final int EDGE_DIRECTION_RIGHT = 1;
    // 默认drawer占父布局的百分比
    public static final float DEFAULT_DRAWER_WIDTH_PERCENT = 60;
    // menu显示出来的占自身的百分比(0 ~ 100)
    private float mMenuFliPercent;
    // menu的总宽度占父布局的百分比
    private float percent;
    private View mMenuView;
    private View mContentView;
    // 底部蒙层View
    private View mMaskView;
    private ViewDragHelper mHelper;
    private Context mContext;
    // 自身的宽度
    private int parentWidth;
    private int parentHeight;

    private final int shadowColor;
    private int edgeDirection;

    public SlideDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlideDrawerLayout);
        percent = typedArray.getFraction(R.styleable.SlideDrawerLayout_drawerPercent, 1, 1, DEFAULT_DRAWER_WIDTH_PERCENT);
        edgeDirection = typedArray.getInt(R.styleable.SlideDrawerLayout_edgeDirection, EDGE_DIRECTION_LEFT);
        edgeDirection = getAbsoluteGravity(edgeDirection);
        shadowColor = typedArray.getColor(R.styleable.SlideDrawerLayout_shadowColor, DEFAULT_SHADOW_COLOR);
        mHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
                if (edgeDirection == EDGE_DIRECTION_LEFT) {
                    return Math.max(-child.getWidth(), Math.min(left, 0));
                } else {
                    final int width = getWidth();
                    return Math.max(width - child.getWidth(), Math.min(left, width));
                }
            }

            @Override
            public boolean tryCaptureView(@NonNull View child, int pointerId) {
                return child == mMenuView;
            }

            @Override
            public void onEdgeDragStarted(int edgeFlags, int pointerId) {
                mHelper.captureChildView(mMenuView, pointerId);
            }

            @Override
            public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
                final int childWidth = releasedChild.getWidth();
                int width = getWidth();
                if (edgeDirection == EDGE_DIRECTION_LEFT) {
                    float offset = (childWidth + releasedChild.getLeft()) * 1.0f / childWidth;
                    mHelper.settleCapturedViewAt(xvel > 0 || xvel == 0 && offset > 0.5f ? 0 : -childWidth, releasedChild.getTop());
                } else {
                    float offset = (width - releasedChild.getLeft()) * 1.0f / childWidth;
                    mHelper.settleCapturedViewAt(xvel < 0 || xvel == 0 && offset > 0.5f ? width - childWidth : width, releasedChild.getTop());
                }
                invalidate();
            }

            @Override
            public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {

                final int childWidth = changedView.getWidth();
                float offset;
                if (edgeDirection == EDGE_DIRECTION_LEFT) {
                    offset = (float) (childWidth + left) / childWidth;
                } else {
                    offset = (float) (getWidth() - left) / childWidth;
                }
                mMenuFliPercent = offset;
                changedView.setVisibility(offset == 0 ? View.INVISIBLE : View.VISIBLE);

                if (mMenuFliPercent > 0) {
                    switchMaskView(true);
                } else {
                    switchMaskView(false);
                }
                setMaskWidth((int) (parentWidth - childWidth * offset));
                if (edgeDirection == EDGE_DIRECTION_LEFT) {
                    setMaskMargin(offset, (int) (childWidth * offset), 0, 0, 0);
                } else {
                    setMaskMargin(offset, 0, 0, (int) (childWidth * offset), 0);
                }

                invalidate();
            }

            @Override
            public int getViewHorizontalDragRange(@NonNull View child) {
                return mMenuView == child ? child.getWidth() : 0;
            }
        });

        //设置edge方向
        mHelper.setEdgeTrackingEnabled(edgeDirection == 0 ? ViewDragHelper.EDGE_LEFT : ViewDragHelper.EDGE_RIGHT);
        //设置minVelocity，加速度
        float density = getResources().getDisplayMetrics().density;
        float minVel = MIN_FLING_VELOCITY * density;
        mHelper.setMinVelocity(minVel);
        setFocusable(true);
        setFocusableInTouchMode(true);
        typedArray.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContentView = getChildAt(0);
        mMenuView = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 设置测量宽高
        parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
        // menu离父容器右边的最小外边距
        int mMinDrawerMargin = (int) (parentWidth * (1 - percent));
        // 绘制Menu视图
        View menuView = getChildAt(1);
        MarginLayoutParams lp = (MarginLayoutParams) menuView.getLayoutParams();
        final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec, mMinDrawerMargin + lp.leftMargin + lp.rightMargin, lp.width);
        final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec, lp.topMargin + lp.bottomMargin, lp.height);
        menuView.measure(drawerWidthSpec, drawerHeightSpec);
        ViewCompat.setLayoutDirection(menuView, GravityCompat.START);
        // 绘制内容视图
        View contentView = getChildAt(0);
        lp = (MarginLayoutParams) contentView.getLayoutParams();
        final int contentWidthSpec = MeasureSpec.makeMeasureSpec(parentWidth - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
        final int contentHeightSpec = MeasureSpec.makeMeasureSpec(parentHeight - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
        contentView.measure(contentWidthSpec, contentHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View menuView = mMenuView;
        View contentView = mContentView;

        MarginLayoutParams lp = (MarginLayoutParams) contentView.getLayoutParams();
        contentView.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + contentView.getMeasuredWidth(), lp.topMargin + contentView.getMeasuredHeight());

        lp = (MarginLayoutParams) menuView.getLayoutParams();

        final int childWidth = menuView.getMeasuredWidth();
        final int width = r - l;
        int childLeft;
        if (edgeDirection == EDGE_DIRECTION_LEFT) {
            childLeft = -childWidth + (int) (childWidth * mMenuFliPercent);
        } else {
            childLeft = width - (int) (childWidth * mMenuFliPercent);
        }
        menuView.layout(childLeft, lp.topMargin, childLeft + childWidth, lp.topMargin + menuView.getMeasuredHeight());
    }

    /**
     * 添加蒙层
     *
     * @param activity 上下文
     */
    private void attachMaskToMenuView(Activity activity) {
        if (mMaskView != null) {
            return;
        }
        mMaskView = new View(activity);
        mMaskView.setBackgroundColor(shadowColor);
        mMaskView.setVisibility(View.GONE);
        mMaskView.setClickable(true);
        mMaskView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SlideDrawerLayout.this.isShow()) {
                    SlideDrawerLayout.this.closeDrawer();
                }
            }
        });
        ViewGroup contentFrameLayout = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        contentFrameLayout.addView(mMaskView, parentWidth, parentHeight);
        setMaskMargin(0, 0, 0, 0, 0);
    }


    /**
     * 设置灰化阴影宽度
     *
     * @param width 灰化阴影部分宽度
     */
    private void setMaskWidth(int width) {
        LayoutParams layoutParams = mMaskView.getLayoutParams();
        layoutParams.width = width;
        mMaskView.setLayoutParams(layoutParams);
    }

    /**
     * 根据滑动过程动态设置灰化阴影压缩距离
     *
     * @param alpha 透明度
     * @param l     左边距
     * @param t     上边距
     * @param r     右边距
     * @param b     下边距
     */
    private void setMaskMargin(float alpha, int l, int t, int r, int b) {
        if (mMaskView.getLayoutParams() instanceof MarginLayoutParams) {
            MarginLayoutParams p = (MarginLayoutParams) mMaskView.getLayoutParams();
            p.setMargins(l, t, r, b);
            mMaskView.setLayoutParams(p);
            mMaskView.setAlpha(alpha);
            mMaskView.requestLayout();
        }
    }

    /**
     * 蒙层显示开关
     */
    private void switchMaskView(boolean bShow) {
        attachMaskToMenuView((Activity) mContext);
        if (bShow) {
            mMaskView.setVisibility(View.VISIBLE);
        } else {
            if (mMaskView != null) {
                mMaskView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 打开menu
     */
    public void openDrawer() {
        View menuView = mMenuView;
        mMenuFliPercent = 1.0f;
        if (isEdgeLeft()) {
            mHelper.smoothSlideViewTo(menuView, 0, menuView.getTop());
        } else {
            mHelper.smoothSlideViewTo(menuView, getWidth() - menuView.getWidth(), menuView.getTop());
        }
        switchMaskView(true);
        invalidate();
    }

    /**
     * 关闭menu
     */
    public void closeDrawer() {
        View menuView = mMenuView;
        mMenuFliPercent = 0.f;
        if (isEdgeLeft()) {
            mHelper.smoothSlideViewTo(menuView, -menuView.getWidth(), menuView.getTop());
        } else {
            mHelper.smoothSlideViewTo(menuView, getWidth(), menuView.getTop());
        }
        switchMaskView(false);
        invalidate();
    }

    /**
     * 是否从左侧滑出
     *
     * @return EDGE_DIRECTION_LEFT: 从左侧
     * EDGE_DIRECTION_RIGHT: 从右侧
     */
    private boolean isEdgeLeft() {
        return edgeDirection == EDGE_DIRECTION_LEFT;
    }

    /**
     * 根据系统语言获取menu真实应该从哪里滑出来
     *
     * @param edgeDirection 方向
     * @return 0:左边 1:右边
     */
    private int getAbsoluteGravity(int edgeDirection) {
        if (isLeftToRight()) {
            return edgeDirection;
        } else {
            return edgeDirection == EDGE_DIRECTION_LEFT ? EDGE_DIRECTION_RIGHT : EDGE_DIRECTION_LEFT;
        }
    }

    /**
     * 判断界面是默认从左到右，还是从右到左
     *
     * @return T:判断是否从左侧滑出
     */
    private boolean isLeftToRight() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage().toLowerCase();
        List<String> lanList = Arrays.asList(languages);
        return !lanList.contains(language);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isShow()) {
                closeDrawer();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mHelper.shouldInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * 判断menu是否完全划出来
     *
     * @return 1.0f: 完全划出来
     */
    private boolean isShow() {
        return mMenuFliPercent == 1.0f;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }
}
