package com.africastalking.android.ui.voice;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.africastalking.AfricasTalking;
import com.africastalking.AfricasTalkingException;
import com.africastalking.services.voice.CallInfo;
import com.africastalking.services.voice.CallListener;
import com.africastalking.services.voice.VoiceBackgroundService;
import com.africastalking.services.voice.VoiceBackgroundService.VoiceServiceBinder;
import com.africastalking.android.R;

public class IncomingCallActivity extends AppCompatActivity {


    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.btnPickUp)
    Button pickUp;

    @BindView(R.id.btnHold)
    Button hold;

    private boolean held = false;

    private VoiceBackgroundService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            VoiceServiceBinder binder = (VoiceServiceBinder) service;
            mService = binder.getService();

            CallInfo info = mService.getCallInfo();
            if (mService.isCallInProgress()) {
                title.setText(info.getDisplayName());
                pickUp.setVisibility(View.GONE);
                hold.setVisibility(View.VISIBLE);
                mService.setCallListener(new CallListener() {
                    @Override
                    public void onCallEnded(CallInfo callInfo) {
                        finish();
                    }
                });
            } else {
                title.setText(info.getDisplayName() + " Calling...");
                pickUp.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_incoming);
        ButterKnife.bind(this);

        bindService(new Intent(this, VoiceBackgroundService.class), mConnection, 0);
    }

    @OnClick(R.id.btnPickUp)
    public void onPickUp() {

        if (mService == null) {
            Log.e("Service Not Bound!", "");
            return;
        }

        try {
            mService.pickCall(new CallListener() {
                @Override
                public void onError(CallInfo call, int errorCode, String errorMessage) {
                    Log.e("Error making call", errorMessage + "(" + errorCode + ")");
                }

                @Override
                public void onRinging(final CallInfo call) {
                    Log.e("Ringing", call.getDisplayName());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            title.setText("Ringing " + call.getDisplayName() + "...");
                        }
                    });
                }

                @Override
                public void onCallEstablished(final CallInfo call) {
                    Log.e("Starting call", "");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            title.setText(call.getDisplayName());

                            hold.setVisibility(View.VISIBLE);
                            pickUp.setVisibility(View.GONE);
                        }
                    });
                    mService.startAudio();
                    mService.setSpeakerMode(false);
                }
                @Override
                public void onCallEnded(CallInfo call) {
                    Log.e("Call Ended", "");
                    finish();
                }
            });
        } catch (AfricasTalkingException e) {
            e.printStackTrace();
        }

    }

    @OnClick(R.id.btnHangUp)
    public void onHangUp() {
        try {

            if (mService == null) {
                Log.e("Service Not Bound!", "");
                return;
            }

            mService.endCall();
            finish();
        } catch (AfricasTalkingException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.btnHold)
    public void onHold() {
        try {

            if (mService == null) {
                Log.e("Service Not Bound!", "");
                return;
            }
            if (held) {
                mService.resumeCall();
                held = false;
            } else {
                mService.holdCall();
                held = true;
            }
        } catch (AfricasTalkingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        AfricasTalking.unbindVoiceBackgroundService(this, mConnection);
        super.onDestroy();
    }
}
