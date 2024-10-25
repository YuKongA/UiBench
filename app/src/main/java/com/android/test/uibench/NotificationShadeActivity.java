/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.test.uibench;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;

public class NotificationShadeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new FakeNotificationStackView(this));
    }

    private static class FakeNotificationStackView extends LinearLayout {
        private static final int INITIAL_ELEVATION = 40;

        private final LayoutInflater mLayoutInflater;
        private final GestureDetector mGestureDetector;
        private final Scroller mScroller;
        private final ArrayList<View> mChildren = new ArrayList<>();
        private final int mScaledTouchSlop;
        private int mChildrenCount = 0;
        private int mFullHeight = 0;
        private final Runnable mUpdateAction = new Runnable() {
            @Override
            public void run() {
                if (mScroller.computeScrollOffset()) {
                    updateState(mScroller.getCurrY());
                    postOnAnimation(this);
                }
            }
        };

        public FakeNotificationStackView(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            mLayoutInflater = LayoutInflater.from(getContext());
            GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(@NonNull MotionEvent motionEvent) {
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                    assert e1 != null;
                    if (Math.abs(e1.getY() - e2.getY()) <= mScaledTouchSlop) {
                        return false;
                    }
                    mScroller.fling(0, mFullHeight, 0, (int) vY, 0, 0, 0, getHeight());
                    postOnAnimation(mUpdateAction);
                    return true;
                }
            };
            mGestureDetector = new GestureDetector(getContext(), mGestureListener);
            mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            mScroller = new Scroller(getContext());
        }

        private void generateNextView() {
            View view = mLayoutInflater.inflate(R.layout.notification, this, false);
            boolean even = mChildren.size() % 2 == 0;
            Context context = getContext();
            ((TextView) view.findViewById(R.id.title)).setText(even ?
                    "Very important notification" : "Next video to watch");
            ((TextView) view.findViewById(R.id.line2)).setText(even ?
                    "Wifi nearby" : "Amazing cats");
            TextView infoView = view.findViewById(R.id.info);
            Drawable drawable = AppCompatResources.getDrawable(context, even ? R.drawable.ic_menu_manage : R.drawable.ic_menu_slideshow);
            int size = context.getResources().getDimensionPixelSize(R.dimen.notification_icon_size);
            assert drawable != null;
            drawable.setBounds(0, 0, size, size);
            infoView.setCompoundDrawables(drawable, null, null, null);
            infoView.setText(even ? "Android System" : "Youtube");
            mChildren.add(view);
        }

        private int lastChildHeight() {
            return (int) mChildren.get(mChildrenCount - 1).getTag();
        }

        private void updateState(int expectedHeight) {
            if (expectedHeight == 0 && mChildrenCount == 0) {
                return;
            }
            for (View v : mChildren) {
                v.setTranslationY(0);
                v.setElevation(INITIAL_ELEVATION);
            }
            if (mChildrenCount != 0 && expectedHeight < mFullHeight - lastChildHeight()) {
                while (mChildrenCount > 0) {
                    if (expectedHeight > mFullHeight - lastChildHeight()) {
                        break;
                    }
                    mFullHeight -= lastChildHeight();
                    removeView(mChildren.get(mChildrenCount - 1));
                    mChildrenCount--;
                }
            } else if (expectedHeight > mFullHeight) {
                while (expectedHeight > mFullHeight) {
                    if (mChildrenCount == mChildren.size()) {
                        generateNextView();
                    }
                    mChildrenCount++;
                    View child = mChildren.get(mChildrenCount - 1);
                    child.setElevation(INITIAL_ELEVATION);
                    int widthSpec = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                    int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    child.measure(widthSpec, heightSpec);
                    addView(child);
                    int measuredHeight = child.getMeasuredHeight();
                    child.setTag(measuredHeight);
                    mFullHeight += measuredHeight;
                }
            }
            if (mChildrenCount == 0) {
                return;
            }
            View lastChild = mChildren.get(mChildrenCount - 1);
            int translationY = expectedHeight - mFullHeight;
            lastChild.setTranslationY(translationY);
            float p = -((float) translationY) / lastChildHeight();
            lastChild.setElevation((1 - p) * INITIAL_ELEVATION);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            return mGestureDetector.onTouchEvent(ev);
        }
    }
}
