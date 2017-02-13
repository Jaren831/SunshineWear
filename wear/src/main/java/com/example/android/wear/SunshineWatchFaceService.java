/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            DataApi.DataListener {
        //Member variables
        private Typeface WATCH_TEXT_TYPEFACE = Typeface.createFromAsset( getAssets(), "font/Roboto-Light.ttf" );

        private static final int MSG_UPDATE_TIME_ID = 42;
        private long mUpdateRateMs = 1000;

        private Time mDisplayTime;

        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsInMuteMode;
        private boolean mIsLowBitAmbient;

        String tempMin;
        String temppMax;
        String weatherId;

        private float mXOffset;
        private float mYOffset;

        float weatherXOffset;
        float weatherYOffset;

        GoogleApiClient googleApiClient;

        private int mBackgroundColor = Color.parseColor( "#03A9F4" );
        private int mTextColor = Color.parseColor( "white" );

        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear( intent.getStringExtra( "time-zone" ) );
                mDisplayTime.setToNow();
            }
        };

        private final Handler mTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch( msg.what ) {
                    case MSG_UPDATE_TIME_ID: {
                        invalidate();
                        if( isVisible() && !isInAmbientMode() ) {
                            long currentTimeMillis = System.currentTimeMillis();
                            long delay = mUpdateRateMs - ( currentTimeMillis % mUpdateRateMs );
                            mTimeHandler.sendEmptyMessageDelayed( MSG_UPDATE_TIME_ID, delay );
                        }
                        break;
                    }
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            setWatchFaceStyle( new WatchFaceStyle.Builder( SunshineWatchFaceService.this )
                    .setBackgroundVisibility( WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE )
                    .setCardPeekMode( WatchFaceStyle.PEEK_MODE_VARIABLE )
                    .setShowSystemUiTime( false )
                    .build()
            );

            mDisplayTime = new Time();

            googleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            googleApiClient.connect();

            initBackground();
            initDisplayText();
        }

        @Override
        public void onConnected(Bundle bundle) {
        }

        @Override
        public void onConnectionSuspended(int i) {
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    if (path.equals("/weather_data")) {
                        tempMin = dataMap.getString("tempMin");
                        temppMax = dataMap.getString("tempMax");
                        weatherId = dataMap.getString("weatherId");
                    }
                }
            }

        }

        private void initBackground() {
            mBackgroundColorPaint = new Paint();
            mBackgroundColorPaint.setColor( mBackgroundColor );
        }

        private void initDisplayText() {
            mTextColorPaint = new Paint();
            mTextColorPaint.setColor( mTextColor );
            mTextColorPaint.setTypeface( WATCH_TEXT_TYPEFACE );
            mTextColorPaint.setAntiAlias( true );
            mTextColorPaint.setTextSize( getResources().getDimension( R.dimen.digital_text_size ) );
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            if( properties.getBoolean( PROPERTY_BURN_IN_PROTECTION, false ) ) {
                mIsLowBitAmbient = properties.getBoolean( PROPERTY_LOW_BIT_AMBIENT, false );
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();

        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            if( inAmbientMode ) {
                mBackgroundColorPaint.setColor( Color.parseColor( "black" ) );
            } else {
                mBackgroundColorPaint.setColor( Color.parseColor( "#03A9F4" ) );
            }

            if( mIsLowBitAmbient ) {
                mTextColorPaint.setAntiAlias( !inAmbientMode );
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */

            mDisplayTime.setToNow();

            drawBackground( canvas, bounds );
            drawTimeText(canvas, bounds );
        }
        private void drawTimeText( Canvas canvas, Rect bounds ) {
            String timeText = getHourString() + ":" + String.format( "%02d", mDisplayTime.minute );
            timeText += ( mDisplayTime.hour < 12 ) ? " AM" : " PM";

            canvas.drawText( timeText, bounds.centerX() - mTextColorPaint.measureText(timeText) / 2, bounds.centerY(), mTextColorPaint );
            canvas.drawText( getWeatherText(), bounds.centerX() - mTextColorPaint.measureText(getWeatherText()) / 2, bounds.centerY() + mTextColorPaint.measureText(timeText) / 4, mTextColorPaint );
        }
        private String getHourString() {
            if( mDisplayTime.hour % 12 == 0 )
                return "12";
            else if( mDisplayTime.hour <= 12 )
                return String.valueOf( mDisplayTime.hour );
            else
                return String.valueOf( mDisplayTime.hour - 12 );
        }

        private String getWeatherText() {
            return tempMin;
        }

        private void drawBackground( Canvas canvas, Rect bounds ) {
            canvas.drawRect( 0, 0, bounds.width(), bounds.height(), mBackgroundColorPaint );
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if( visible ) {
                if( !mHasTimeZoneReceiverBeenRegistered ) {

                    IntentFilter filter = new IntentFilter( Intent.ACTION_TIMEZONE_CHANGED );
                    SunshineWatchFaceService.this.registerReceiver( mTimeZoneBroadcastReceiver, filter );

                    mHasTimeZoneReceiverBeenRegistered = true;
                }

                mDisplayTime.clear( TimeZone.getDefault().getID() );
                mDisplayTime.setToNow();
            } else {
                if( mHasTimeZoneReceiverBeenRegistered ) {
                    SunshineWatchFaceService.this.unregisterReceiver( mTimeZoneBroadcastReceiver );
                    mHasTimeZoneReceiverBeenRegistered = false;
                }
            }

            updateTimer();
        }

        private void updateTimer() {
            mTimeHandler.removeMessages( MSG_UPDATE_TIME_ID );
            if( isVisible() && !isInAmbientMode() ) {
                mTimeHandler.sendEmptyMessage( MSG_UPDATE_TIME_ID );
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mYOffset = getResources().getDimension( R.dimen.digital_y_offset );
            weatherYOffset = getResources().getDimension(R.dimen.weather_digital_y_offset);

            if( insets.isRound() ) {
                mXOffset = getResources().getDimension( R.dimen.digital_x_offset_round );
                weatherXOffset = getResources().getDimension( R.dimen.weather_digital_x_offset_round );
            } else {
                mXOffset = getResources().getDimension( R.dimen.weather_digital_x_offset );
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean isDeviceMuted = ( interruptionFilter == android.support.wearable.watchface.WatchFaceService.INTERRUPTION_FILTER_NONE );
            if( isDeviceMuted ) {
                mUpdateRateMs = TimeUnit.MINUTES.toMillis( 1 );
            } else {
            }

            if( mIsInMuteMode != isDeviceMuted ) {
                mIsInMuteMode = isDeviceMuted;
                int alpha = ( isDeviceMuted ) ? 100 : 255;
                mTextColorPaint.setAlpha( alpha );
                invalidate();
                updateTimer();
            }
        }
    }
}
