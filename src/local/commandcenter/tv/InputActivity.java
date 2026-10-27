package local.commandcenter.tv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.tv.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class InputActivity extends Activity {
    private TvView tv;
    private TextView status;
    private int port;
    private boolean resumed;
    private final Handler handler=new Handler();
    private AudioManager audio;
    private final AudioManager.OnAudioFocusChangeListener focus=change->{if(tv!=null)tv.setStreamVolume(change==AudioManager.AUDIOFOCUS_GAIN?1f:0f);};
    private final Runnable hide=()->status.setVisibility(View.GONE);
    @Override public void onCreate(Bundle state){super.onCreate(state);
        port=getIntent().getIntExtra("port",1);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        tv=new TvView(this);root.addView(tv,new FrameLayout.LayoutParams(-1,-1));
        float density=getResources().getDisplayMetrics().density;
        status=new TextView(this);status.setTextColor(MainActivity.TEXT);status.setTextSize(15);status.setGravity(Gravity.CENTER);status.setPadding((int)(22*density),(int)(14*density),(int)(22*density),(int)(14*density));
        android.graphics.drawable.GradientDrawable surface=new android.graphics.drawable.GradientDrawable();surface.setColor(0xED1D1F22);surface.setCornerRadius(14*density);status.setBackground(surface);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);sp.setMargins((int)(36*density),0,(int)(36*density),(int)(28*density));root.addView(status,sp);setContentView(root);
        audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        tv.setCallback(new TvView.TvInputCallback(){
            @Override public void onVideoAvailable(String id){Log.i("CommandCenter","INPUT_VIDEO_AVAILABLE "+id);show("HDMI "+port+"     ·     ← / →  Switch input     ·     Back to Today");handler.postDelayed(hide,5000);}
            @Override public void onVideoUnavailable(String id,int reason){Log.i("CommandCenter","INPUT_VIDEO_UNAVAILABLE "+id+" reason="+reason);show("HDMI "+port+"  ·  Waiting for signal     ·     ← / →  Switch     ·     Back to Today");}
            @Override public void onConnectionFailed(String id){Log.e("CommandCenter","INPUT_CONNECTION_FAILED "+id);show("Unable to open HDMI "+port+". Press Back to return.");}
            @Override public void onDisconnected(String id){Log.w("CommandCenter","INPUT_DISCONNECTED "+id);show("HDMI disconnected. Press Back to return.");}
        });
    }
    private void show(String s){handler.removeCallbacks(hide);status.setText(s);status.setVisibility(View.VISIBLE);}
    private void tune(){
        String id=null;
        try{for(TvInputInfo i:((TvInputManager)getSystemService(TV_INPUT_SERVICE)).getTvInputList())if(i.getId().contains("Hdmi"+port+"InputService")){id=i.getId();break;}
            if(id==null){show("HDMI "+port+" unavailable. Press Back.");return;}
            tv.reset();show("Opening HDMI "+port+"…   ·   Back for dashboard");
            int result=audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            tv.tune(id,TvContract.buildChannelUriForPassthroughInput(id));tv.setStreamVolume(result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED?1f:0f);
            Log.i("CommandCenter","INPUT_TUNE_REQUEST "+id);
        }catch(Exception e){Log.e("CommandCenter","INPUT_TUNE_FAILED",e);show("Input access unavailable on this TV. Press Back to return.");}
    }
    @Override protected void onResume(){super.onResume();resumed=true;tv.post(()->{if(resumed)tune();});}
    @Override protected void onPause(){resumed=false;tv.reset();audio.abandonAudioFocus(focus);handler.removeCallbacksAndMessages(null);super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(e.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT||e.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){port=(port+(e.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT?0:1))%3+1;tune();}return true;}
        if(e.getKeyCode()==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)finish();return true;}
        return super.dispatchKeyEvent(e);
    }
}
