package local.commandcenter.tv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(16,17,19), CARD=Color.rgb(29,31,34), TEXT=Color.rgb(241,239,232), MUTED=Color.rgb(162,165,168), ACCENT=Color.rgb(168,202,181);
    private final Handler handler=new Handler();
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private TextView clock, period, greeting, date, weather, weatherDetail, weatherRange, weatherTime, price, change, quoteTime;
    private DashboardGlyph weatherGlyph;
    private TrendChart weatherChart,stockChart;
    private TextView weatherChartLabel,stockChartLabel;
    private boolean visible=false;
    private final FeedState weatherFeed=new FeedState(60000),quoteFeed=new FeedState(30000);
    private long weatherSource=0,quoteSource=0,regularStart=0,regularEnd=0;
    private TextView marketDetail,daylight,forecastStatus;
    private final TextView[] forecastDays=new TextView[5],forecastTemps=new TextView[5],forecastConditions=new TextView[5],forecastRain=new TextView[5];
    private final DashboardGlyph[] forecastIcons=new DashboardGlyph[5];
    private ForecastDay[] forecast=new ForecastDay[0];
    private static final class ForecastDay {
        long date,sunrise,sunset;double high,low;int code,rain;
    }
    private final Runnable feeds=new Runnable(){public void run(){
        if(!visible)return;
        refreshDue();updateFreshness();handler.postDelayed(this,5000);
    }};
    private Dialog activePanel;
    private View panelContent;
    private boolean panelClosing=false;
    private LinearLayout dashboard;
    private final Runnable tick=new Runnable(){ public void run(){
        long now=System.currentTimeMillis();
        updateValue(clock,format("h:mm",now));period.setText(format("a",now));date.setText(format("EEEE, MMMM d",now));
        int hour=Integer.parseInt(format("H",now));greeting.setText(hour<12?"Good morning.":hour<18?"Good afternoon.":"Good evening.");
        handler.postDelayed(this,60000-System.currentTimeMillis()%60000);
    }};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(getPreferences(0).getBoolean("keep_awake",true))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout root=column();dashboard=root;root.setFocusable(true);root.setFocusableInTouchMode(true);root.setPadding(dp(44),dp(20),dp(44),dp(18));root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout header=row();
        DashboardGlyph mark=new DashboardGlyph(this,"home",TEXT);header.addView(mark,new LinearLayout.LayoutParams(dp(20),dp(20)));
        TextView title=text("Today",17,TEXT);title.setPadding(dp(10),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView location=label("CHARLOTTE, NC  /  28277");header.addView(location);
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout hero=row();
        LinearLayout time=column();time.setGravity(Gravity.CENTER_VERTICAL);
        greeting=text("",17,MUTED);time.addView(greeting);
        LinearLayout timeLine=row();timeLine.setGravity(Gravity.BOTTOM);
        clock=text("",100,TEXT);clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));clock.setLetterSpacing(-0.055f);clock.setFontFeatureSettings("tnum");
        timeLine.addView(clock);period=text("",16,MUTED);period.setPadding(dp(9),0,0,dp(18));timeLine.addView(period);time.addView(timeLine);
        date=text("",18,TEXT);date.setPadding(dp(3),dp(2),0,0);time.addView(date);
        daylight=text("Daylight · checking…",12,MUTED);daylight.setPadding(dp(3),dp(14),0,0);time.addView(daylight,new LinearLayout.LayoutParams(-1,dp(48)));
        hero.addView(time,new LinearLayout.LayoutParams(0,-1,1.25f));
        LinearLayout weatherPanel=column();weatherPanel.setGravity(Gravity.CENTER_VERTICAL);weatherPanel.setPadding(dp(20),0,dp(10),0);
        weatherPanel.addView(label("WEATHER"));
        LinearLayout tempLine=row();weather=text("—",50,TEXT);weather.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));weather.setFontFeatureSettings("tnum");tempLine.addView(weather);
        weatherGlyph=new DashboardGlyph(this,"weather",MUTED);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(30),dp(30));gp.setMargins(dp(10),0,0,0);tempLine.addView(weatherGlyph,gp);
        LinearLayout.LayoutParams valueLine=new LinearLayout.LayoutParams(-1,dp(66));weatherPanel.addView(tempLine,valueLine);
        weatherDetail=text("Loading weather…",16,TEXT);weatherPanel.addView(weatherDetail);
        weatherRange=text("",13,MUTED);weatherRange.setPadding(0,dp(8),0,0);weatherPanel.addView(weatherRange,new LinearLayout.LayoutParams(-1,dp(38)));
        weatherChartLabel=text("Today · hourly model · dashed = ahead",10,MUTED);weatherPanel.addView(weatherChartLabel);
        weatherChart=new TrendChart(this,true);weatherPanel.addView(weatherChart,new LinearLayout.LayoutParams(-1,dp(84)));
        weatherTime=text("Open-Meteo",10,MUTED);weatherTime.setPadding(0,dp(4),0,0);weatherPanel.addView(weatherTime);
        hero.addView(weatherPanel,new LinearLayout.LayoutParams(0,-1,1.1f));
        LinearLayout stock=column();stock.setGravity(Gravity.CENTER_VERTICAL);stock.setPadding(dp(24),0,0,0);
        stock.addView(label("SALESFORCE  /  CRM"));
        price=text("—",46,TEXT);price.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));price.setFontFeatureSettings("tnum");price.setGravity(Gravity.CENTER_VERTICAL);stock.addView(price,new LinearLayout.LayoutParams(-1,dp(66)));
        change=text("Loading quote…",16,TEXT);stock.addView(change);
        marketDetail=text("Today · USD",13,MUTED);marketDetail.setPadding(0,dp(8),0,0);stock.addView(marketDetail,new LinearLayout.LayoutParams(-1,dp(38)));
        stockChartLabel=text("Today · 1-min close",10,MUTED);stock.addView(stockChartLabel);
        stockChart=new TrendChart(this,false);stock.addView(stockChart,new LinearLayout.LayoutParams(-1,dp(84)));
        quoteTime=text("Yahoo Finance · may be delayed",10,MUTED);quoteTime.setPadding(0,dp(4),0,0);stock.addView(quoteTime);
        hero.addView(stock,new LinearLayout.LayoutParams(0,-1,1.25f));
        root.addView(hero,new LinearLayout.LayoutParams(-1,0,1));
        View rule=new View(this);rule.setBackgroundColor(0x20FFFFFF);
        LinearLayout.LayoutParams ruleParams=new LinearLayout.LayoutParams(-1,dp(1));ruleParams.setMargins(0,0,0,dp(10));root.addView(rule,ruleParams);
        LinearLayout forecastHeader=row();forecastHeader.addView(label("5-DAY OUTLOOK"),new LinearLayout.LayoutParams(0,-2,1));
        forecastStatus=text("Charlotte · Fahrenheit",11,MUTED);forecastHeader.addView(forecastStatus);
        root.addView(forecastHeader,new LinearLayout.LayoutParams(-1,dp(22)));
        LinearLayout forecastRow=row();
        for(int i=0;i<5;i++){
            LinearLayout day=column();day.setPadding(0,dp(6),dp(18),0);
            forecastDays[i]=text("—",14,TEXT);day.addView(forecastDays[i]);
            LinearLayout values=row();forecastIcons[i]=new DashboardGlyph(this,"weather",MUTED);forecastIcons[i].setVisibility(View.INVISIBLE);
            values.addView(forecastIcons[i],new LinearLayout.LayoutParams(dp(22),dp(22)));
            forecastTemps[i]=text("— / —",24,TEXT);forecastTemps[i].setFontFeatureSettings("tnum");forecastTemps[i].setPadding(dp(10),0,0,0);values.addView(forecastTemps[i]);
            day.addView(values,new LinearLayout.LayoutParams(-1,dp(38)));
            forecastConditions[i]=text("Loading forecast…",12,MUTED);day.addView(forecastConditions[i]);
            forecastRain[i]=text("",12,MUTED);forecastRain[i].setPadding(0,dp(5),0,0);day.addView(forecastRain[i]);
            forecastRow.addView(day,new LinearLayout.LayoutParams(0,-1,1));
        }
        root.addView(forecastRow,new LinearLayout.LayoutParams(-1,dp(96)));
        TextView help=text("Menu or Select · Apps & inputs",12,MUTED);help.setGravity(Gravity.RIGHT);
        root.addView(help,new LinearLayout.LayoutParams(-1,dp(24)));
        restore();root.requestFocus();
        Log.i("CommandCenter","READY Android "+Build.VERSION.RELEASE+" inputs="+inputs().size());
    }
    static String format(String pattern,long time){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setTimeZone(TimeZone.getTimeZone("America/New_York"));return f.format(new Date(time));}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));return t;}
    TextView label(String value){TextView t=text(value,11,MUTED);t.setLetterSpacing(0.12f);return t;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    GradientDrawable surface(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    void addAction(LinearLayout parent,String title,String kind,boolean app,Runnable action){
        LinearLayout b=row();b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setClickable(true);b.setContentDescription(title);b.setBackground(surface(app?CARD:Color.TRANSPARENT,0));
        DashboardGlyph icon=new DashboardGlyph(this,kind,app?TEXT:MUTED);b.addView(icon,new LinearLayout.LayoutParams(dp(app?23:18),dp(app?23:18)));
        TextView name=text(title,app?17:13,app?TEXT:MUTED);name.setPadding(dp(app?11:8),0,0,0);b.addView(name);
        b.setOnFocusChangeListener((v,focus)->{b.setBackground(surface(focus?TEXT:app?CARD:Color.TRANSPARENT,0));name.setTextColor(focus?BG:app?TEXT:MUTED);icon.setColor(focus?BG:app?TEXT:MUTED);});
        b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(0,0,dp(app?12:4),0);parent.addView(b,p);
    }
    private void showControls(){
        if(activePanel!=null)return;
        LinearLayout content=column();content.setPadding(dp(24),dp(20),dp(24),dp(20));content.setBackground(surface(CARD,0));
        LinearLayout appsHeading=row();appsHeading.addView(text("Your apps",14,MUTED),new LinearLayout.LayoutParams(0,-2,1));appsHeading.addView(text("Select to open",12,MUTED));
        content.addView(appsHeading,new LinearLayout.LayoutParams(-1,dp(34)));
        LinearLayout appRow=row();
        addAction(appRow,"Netflix","netflix",true,()->closePanel(()->launchApp("com.netflix.ninja")));
        addAction(appRow,"Apple TV","tv",true,()->closePanel(()->launchApp("com.apple.atve.amazon.appletv")));
        addAction(appRow,"Prime Video","play",true,()->closePanel(()->launchApp("com.amazon.firebat")));
        addAction(appRow,"Silk","globe",true,()->closePanel(()->launchApp("com.amazon.cloud9")));
        content.addView(appRow,new LinearLayout.LayoutParams(-1,dp(62)));
        View divider=new View(this);divider.setBackgroundColor(0x20FFFFFF);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,dp(1));line.setMargins(0,dp(22),0,dp(12));content.addView(divider,line);
        LinearLayout inputRow=row();TextView inputsTitle=text("Inputs",13,MUTED);inputRow.addView(inputsTitle,new LinearLayout.LayoutParams(dp(60),-2));
        for(int i=1;i<=3;i++){ final int port=i;addAction(inputRow,"HDMI "+i,"input",false,()->closePanel(()->openInput(port))); }
        View space=new View(this);inputRow.addView(space,new LinearLayout.LayoutParams(dp(30),1));
        addAction(inputRow,"Refresh","refresh",false,()->closePanel(()->{refresh();}));
        addAction(inputRow,"Options","options",false,()->closePanel(()->showOptions()));content.addView(inputRow,new LinearLayout.LayoutParams(-1,dp(44)));
        TextView help=text("Back or Menu · Return to Today",12,MUTED);help.setPadding(0,dp(14),0,0);content.addView(help);
        presentPanel(content,872,true,appRow.getChildAt(0));
    }
    private boolean motionEnabled(){
        return !getPreferences(0).getBoolean("reduce_motion",false)
            && android.provider.Settings.Global.getFloat(getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f;
    }
    private void updateValue(TextView view,CharSequence value){
        if(view.getText().toString().equals(value.toString()))return;
        boolean animate=visible&&view.getText().length()>0&&!view.getText().toString().equals("—")&&motionEnabled();
        view.animate().cancel();view.setText(value);view.setAlpha(animate?0.72f:1f);
        if(animate)view.animate().alpha(1f).setDuration(200).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }
    private void presentPanel(LinearLayout content,int width,boolean bottom,View first){
        Dialog dialog=new Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(content);
        Window window=dialog.getWindow();window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setWindowAnimations(0);
        WindowManager.LayoutParams params=window.getAttributes();params.dimAmount=0.55f;
        if(bottom){params.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;params.y=dp(24);}window.setAttributes(params);
        dialog.setOnKeyListener((d,key,event)->{
            if(key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_MENU){
                if(event.getAction()==KeyEvent.ACTION_UP)closePanel(null);return true;
            }
            return panelClosing;
        });
        dialog.setOnDismissListener(d->{content.animate().cancel();if(activePanel==dialog){activePanel=null;panelContent=null;panelClosing=false;dashboard.requestFocus();}});
        activePanel=dialog;panelContent=content;panelClosing=false;
        boolean motion=motionEnabled();content.setAlpha(motion?0f:1f);content.setTranslationY(motion?dp(12):0);
        dialog.show();window.setLayout(dp(width),WindowManager.LayoutParams.WRAP_CONTENT);if(first!=null)first.requestFocus();
        if(motion)content.animate().alpha(1f).translationY(0).setDuration(220).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        Log.i("CommandCenter","PANEL_OPEN motion="+motion);
    }
    private void closePanel(Runnable after){
        if(panelClosing)return;
        if(activePanel==null){if(after!=null)after.run();return;}
        panelClosing=true;final Dialog dialog=activePanel;final View content=panelContent;
        content.animate().cancel();
        Runnable finish=()->{
            // Dialog dismissal callbacks are asynchronous; release the old panel before opening its successor.
            activePanel=null;panelContent=null;panelClosing=false;
            dialog.dismiss();dashboard.requestFocus();
            if(!isFinishing()&&!isDestroyed()&&after!=null)after.run();
        };
        if(motionEnabled())content.animate().alpha(0f).translationY(dp(6)).setDuration(140).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(finish).start();
        else finish.run();
    }
    void launchApp(String pkg){
        Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(pkg);
        if(intent==null){
            showPanel("Install this app", "This app isn't on your TV yet. You can download it from the Amazon Appstore.",new String[]{"Open Appstore","Cancel"},new Runnable[]{()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("amzn://apps/android?p="+pkg)));}catch(Exception e){Toast.makeText(this,"Open the Fire TV Appstore to install this app.",Toast.LENGTH_LONG).show();}},()->{}});return;
        }
        try{startActivity(intent);Log.i("CommandCenter","LAUNCH_REQUESTED "+pkg);}catch(Exception e){Log.e("CommandCenter","LAUNCH_FAILED "+pkg,e);Toast.makeText(this,"This app could not be opened.",Toast.LENGTH_LONG).show();}
    }
    List<TvInputInfo> inputs(){try{return ((TvInputManager)getSystemService(TV_INPUT_SERVICE)).getTvInputList();}catch(Exception e){return Collections.emptyList();}}
    void openInput(int port){
        String id=null;for(TvInputInfo info:inputs())if(info.getId().contains("Hdmi"+port+"InputService")){id=info.getId();break;}
        if(id==null){Toast.makeText(this,"HDMI "+port+" is unavailable.",Toast.LENGTH_LONG).show();return;}
        startActivity(new Intent(this,InputActivity.class).putExtra("port",port));
    }
    private void restore(){
        try{String v=getPreferences(0).getString("weather",null);if(v!=null)renderWeather(new JSONObject(v),true);}catch(Exception ignored){}
        try{String v=getPreferences(0).getString("quote",null);if(v!=null)renderQuote(new JSONObject(v),true);}catch(Exception ignored){}
    }
    private void refresh(){
        if(!visible)return;
        long now=SystemClock.elapsedRealtime();
        // Manual refresh bypasses the normal cadence, but respects failure backoff.
        if(!weatherFeed.failed)weatherFeed.nextDue=0;
        if(!quoteFeed.failed)quoteFeed.nextDue=0;
        refreshDue();
    }
    private void refreshDue(){
        long now=SystemClock.elapsedRealtime();
        if(weatherFeed.due(now))fetch(weatherFeed,true);
        if(quoteFeed.due(now))fetch(quoteFeed,false);
    }
    private void fetch(FeedState feed,boolean isWeather){
        feed.begin(SystemClock.elapsedRealtime());
        worker.execute(()->{
            try{
                JSONObject d=get(isWeather?
                    "https://api.open-meteo.com/v1/forecast?latitude=35.22709&longitude=-80.84313&current=temperature_2m,apparent_temperature,weather_code&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code,sunrise,sunset&temperature_unit=fahrenheit&timezone=America%2FNew_York&forecast_days=5":
                    "https://query1.finance.yahoo.com/v8/finance/chart/CRM?interval=1m&range=1d&includePrePost=false");
                handler.post(()->{
                    if(isDestroyed())return;
                    try{
                        if(isWeather)renderWeather(d,false);else renderQuote(d,false);
                        getPreferences(0).edit().putString(isWeather?"weather":"quote",d.toString()).apply();
                        feed.success(SystemClock.elapsedRealtime());
                        Log.i("CommandCenter",(isWeather?"WEATHER":"QUOTE")+"_CHECK_OK");
                    }catch(Exception e){feed.failure(SystemClock.elapsedRealtime());Log.w("CommandCenter","INVALID_FEED "+e);}
                    updateFreshness();
                });
            }catch(Exception e){
                Log.w("CommandCenter",(isWeather?"WEATHER":"QUOTE")+"_FAILED "+e);
                handler.post(()->{if(!isDestroyed()){feed.failure(SystemClock.elapsedRealtime());updateFreshness();}});
            }
        });
    }
    private void updateFreshness(){
        long now=System.currentTimeMillis(),elapsed=SystemClock.elapsedRealtime();
        boolean weatherOld=weatherSource>0&&now-weatherSource>1800000;
        weatherTime.setText("Open-Meteo · 15-min model"+
            (weatherSource>0?"\nAs of "+format("MMM d, h:mm a",weatherSource)+" ET":"")+
            "\n"+(weatherOld?"Stale · ":"")+weatherFeed.status(elapsed,weatherSource>0));
        weatherTime.setTextColor(weatherFeed.failed||weatherOld?Color.rgb(220,184,130):MUTED);
        boolean regular=now>=regularStart&&now<regularEnd;
        boolean old=quoteSource>0&&now-quoteSource>60000;
        String session=regular?"Closes in "+remaining(regularEnd-now)+" · "+format("h:mm a",regularEnd):
            regularStart>now?"Opens "+format("EEE h:mm a",regularStart):
            regularEnd>0&&format("yyyy-MM-dd",regularEnd).equals(format("yyyy-MM-dd",now))?"Session ended "+format("h:mm a",regularEnd):"";
        marketDetail.setText((regular?(old?"Quote over 1 min old · USD":"Regular session · USD"):"Last regular-session quote · USD")+(session.isEmpty()?"":"\n"+session));
        marketDetail.setTextColor(regular&&old?Color.rgb(220,184,130):MUTED);
        quoteTime.setText("Yahoo · may be delayed"+
            (quoteSource>0?"\nQuote "+format("MMM d, h:mm:ss a",quoteSource)+" ET":"")+
            "\n"+quoteFeed.status(elapsed,quoteSource>0));
        quoteTime.setTextColor(quoteFeed.failed?Color.rgb(220,184,130):MUTED);
        updateOutlook(now,weatherOld);weatherChart.time(now);stockChart.time(now);
    }
    static String remaining(long millis){long minutes=Math.max(1,(millis+59999)/60000);return minutes>=60?minutes/60+"h "+minutes%60+"m":minutes+"m";}
    private void updateOutlook(long now,boolean stale){
        String today=format("yyyy-MM-dd",now);ForecastDay current=null,next=null;
        for(int i=0;i<forecast.length;i++){
            ForecastDay d=forecast[i];String key=format("yyyy-MM-dd",d.date);
            forecastDays[i].setText(key.equals(today)?"Today":format("EEE d",d.date));
            if(key.equals(today))current=d;
            if(d.sunrise>now&&(next==null||d.sunrise<next.sunrise))next=d;
        }
        forecastStatus.setText((weatherFeed.failed?"Saved forecast · ":stale?"Stale forecast · ":weatherFeed.lastSuccess<0&&forecast.length>0?"Cached forecast · ":"")+"High / low · °F");
        forecastStatus.setTextColor(weatherFeed.failed||stale?Color.rgb(220,184,130):MUTED);
        if(current==null){daylight.setText("Daylight · awaiting today's forecast");return;}
        String prefix=weatherFeed.failed||stale?"Saved · ":"";
        if(now<current.sunrise)daylight.setText(prefix+"Sunrise "+format("h:mm a",current.sunrise)+" · in "+remaining(current.sunrise-now));
        else if(now<current.sunset)daylight.setText(prefix+"Sunset "+format("h:mm a",current.sunset)+"\n"+remaining(current.sunset-now)+" of daylight left");
        else daylight.setText(prefix+"Sunset was "+format("h:mm a",current.sunset)+(next==null?"":"\nNext sunrise "+format("h:mm a",next.sunrise)));
    }
    private ForecastDay[] parseForecast(JSONObject daily,boolean cached)throws Exception{
        // Old app caches contain one day without conditions/sunrise. Keep current weather until a new fetch arrives.
        if(cached&&!daily.has("sunrise"))return new ForecastDay[0];
        JSONArray dates=daily.getJSONArray("time");if(dates.length()<5)throw new IOException("Incomplete five-day forecast");
        SimpleDateFormat dayFormat=new SimpleDateFormat("yyyy-MM-dd",Locale.US),sunFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.US);
        for(SimpleDateFormat f:new SimpleDateFormat[]{dayFormat,sunFormat}){f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("America/New_York"));}
        ForecastDay[] result=new ForecastDay[5];
        for(int i=0;i<5;i++){
            ForecastDay d=new ForecastDay();d.date=dayFormat.parse(dates.getString(i)).getTime();
            d.high=daily.getJSONArray("temperature_2m_max").getDouble(i);d.low=daily.getJSONArray("temperature_2m_min").getDouble(i);
            d.code=daily.getJSONArray("weather_code").getInt(i);d.rain=daily.getJSONArray("precipitation_probability_max").getInt(i);
            d.sunrise=sunFormat.parse(daily.getJSONArray("sunrise").getString(i)).getTime();d.sunset=sunFormat.parse(daily.getJSONArray("sunset").getString(i)).getTime();
            if(!Double.isFinite(d.high)||!Double.isFinite(d.low)||d.high<d.low||d.rain<0||d.rain>100||d.sunrise>=d.sunset||!format("yyyy-MM-dd",d.sunrise).equals(dates.getString(i))||!format("yyyy-MM-dd",d.sunset).equals(dates.getString(i))||i>0&&d.date<=result[i-1].date)throw new IOException("Invalid forecast day");
            result[i]=d;
        }
        return result;
    }
    private void renderOutlook(ForecastDay[] days){
        forecast=days;
        for(int i=0;i<days.length;i++){
            ForecastDay d=days[i];String high=Math.round(d.high)+"°",low=" / "+Math.round(d.low)+"°";
            android.text.SpannableString temperatures=new android.text.SpannableString(high+low);
            temperatures.setSpan(new android.text.style.ForegroundColorSpan(MUTED),high.length(),temperatures.length(),0);
            temperatures.setSpan(new android.text.style.RelativeSizeSpan(0.75f),high.length(),temperatures.length(),0);
            forecastTemps[i].setText(temperatures);forecastIcons[i].setWeather(d.code);forecastIcons[i].setVisibility(View.VISIBLE);
            forecastConditions[i].setText(condition(d.code));forecastRain[i].setText("Precip. "+d.rain+"%");
        }
    }
    private TrendSeries temperatureTrend(JSONObject response){
        try{
            JSONObject hourly=response.optJSONObject("hourly");if(hourly==null)return null;
            JSONArray times=hourly.getJSONArray("time"),values=hourly.getJSONArray("temperature_2m");
            if(times.length()!=values.length())throw new IOException("Mismatched hourly samples");
            SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.US);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("America/New_York"));
            long start=f.parse(response.getJSONObject("current").getString("time").substring(0,10)+"T00:00").getTime();
            Calendar end=Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));end.setTimeInMillis(start);end.add(Calendar.DATE,1);
            ArrayList<Long> ts=new ArrayList<>();ArrayList<Double> vs=new ArrayList<>();
            for(int i=0;i<times.length();i++){
                long t=f.parse(times.getString(i)).getTime();
                if(t>=start&&t<=end.getTimeInMillis()){ts.add(t);vs.add(values.optDouble(i,Double.NaN));}
            }
            long[] t=new long[ts.size()];double[] v=new double[vs.size()];for(int i=0;i<t.length;i++){t[i]=ts.get(i);v[i]=vs.get(i);}
            return new TrendSeries(t,v,start,end.getTimeInMillis(),5400000);
        }catch(Exception e){Log.w("CommandCenter","WEATHER_TREND_UNAVAILABLE "+e);return null;}
    }
    private TrendSeries quoteTrend(JSONObject result,JSONObject meta){
        try{
            if(!"1m".equals(meta.optString("dataGranularity")))return null;
            JSONArray times=result.optJSONArray("timestamp");if(times==null||times.length()==0)return null;
            JSONArray values=result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
            if(times.length()!=values.length())throw new IOException("Mismatched quote samples");
            long[] t=new long[times.length()];double[] v=new double[times.length()];long first=0,last=0;
            for(int i=0;i<t.length;i++){
                t[i]=times.getLong(i)*1000;v[i]=values.optDouble(i,Double.NaN);
                if(v[i]<=0||t[i]>System.currentTimeMillis())v[i]=Double.NaN;
                if(Double.isFinite(v[i])){if(first==0)first=t[i];last=t[i];}
            }
            if(first==0)return null;
            String day=format("yyyy-MM-dd",last);
            for(int i=0;i<t.length;i++)if(!day.equals(format("yyyy-MM-dd",t[i])))v[i]=Double.NaN;
            JSONObject periods=meta.optJSONObject("currentTradingPeriod"),r=periods==null?null:periods.optJSONObject("regular");
            long start=r==null?0:r.optLong("start",0)*1000,end=r==null?0:r.optLong("end",0)*1000;
            if(start==0||end<=start||!day.equals(format("yyyy-MM-dd",start))){start=first;end=Math.max(first+60000,last);}
            return new TrendSeries(t,v,start,end,90000);
        }catch(Exception e){Log.w("CommandCenter","QUOTE_TREND_UNAVAILABLE "+e);return null;}
    }
    private String trendDay(long stamp){return format("yyyy-MM-dd",stamp).equals(format("yyyy-MM-dd",System.currentTimeMillis()))?"Today":format("MMM d",stamp);}
    JSONObject get(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setUseCaches(false);c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("User-Agent","Mozilla/5.0 CommandCenter/0.6");
        try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"))){StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null){s.append(line);if(s.length()>1000000)throw new IOException("Response too large");}return new JSONObject(s.toString());}}finally{c.disconnect();}
    }
    void renderWeather(JSONObject d,boolean cached)throws Exception{
        JSONObject c=d.getJSONObject("current"),daily=d.getJSONObject("daily");
        double temperature=c.getDouble("temperature_2m"),feels=c.getDouble("apparent_temperature");
        double high=daily.getJSONArray("temperature_2m_max").getDouble(0),low=daily.getJSONArray("temperature_2m_min").getDouble(0);
        int rain=daily.getJSONArray("precipitation_probability_max").getInt(0),code=c.getInt("weather_code");
        SimpleDateFormat sourceDate=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.US);sourceDate.setLenient(false);sourceDate.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        long stamp=sourceDate.parse(c.getString("time")).getTime();
        if(!Double.isFinite(temperature)||!Double.isFinite(feels)||!Double.isFinite(high)||!Double.isFinite(low)||rain<0||rain>100||stamp<weatherSource||stamp>System.currentTimeMillis()+60000)throw new IOException("Invalid weather values/time");
        ForecastDay[] days=parseForecast(daily,cached);
        TrendSeries trend=temperatureTrend(d);
        weatherSource=stamp;renderOutlook(days);weatherChart.data(trend,0xFFB5C5D8);
        weatherChartLabel.setText(trendDay(stamp)+" · hourly model · dashed = ahead");
        Log.i("CommandCenter","WEATHER_TREND_POINTS "+(trend==null?0:trend.count));
        updateValue(weather,Math.round(temperature)+"°");weatherGlyph.setWeather(code);weatherDetail.setText(condition(code));
        weatherRange.setText("Feels "+Math.round(feels)+"° · High "+Math.round(high)+"° / Low "+Math.round(low)+"°\nRain "+rain+"% · Fahrenheit");
        updateFreshness();Log.i("CommandCenter","WEATHER_OK "+c.getString("time"));
    }
    void renderQuote(JSONObject d,boolean cached)throws Exception{
        JSONObject result=d.getJSONObject("chart").getJSONArray("result").getJSONObject(0),m=result.getJSONObject("meta");
        double value=m.getDouble("regularMarketPrice"),prev=m.optDouble("previousClose",m.optDouble("chartPreviousClose",Double.NaN));
        long stamp=m.getLong("regularMarketTime")*1000;
        if(!Double.isFinite(value)||value<=0||stamp<=0||stamp<quoteSource||stamp>System.currentTimeMillis()+60000)throw new IOException("Invalid quote values/time");
        JSONObject periods=m.optJSONObject("currentTradingPeriod"),regular=periods==null?null:periods.optJSONObject("regular");
        regularStart=regular==null?0:regular.optLong("start",0)*1000;regularEnd=regular==null?0:regular.optLong("end",0)*1000;
        TrendSeries trend=quoteTrend(result,m);
        quoteSource=stamp;stockChart.data(trend,Double.isFinite(prev)&&value<prev?Color.rgb(226,158,153):ACCENT);
        stockChartLabel.setText(trendDay(trend==null?stamp:trend.start)+" · 1-min close");
        Log.i("CommandCenter","QUOTE_TREND_POINTS "+(trend==null?0:trend.count));
        android.text.SpannableString amount=new android.text.SpannableString(String.format(Locale.US,"$%,.2f",value));
        amount.setSpan(new android.text.style.RelativeSizeSpan(0.6f),0,1,0);updateValue(price,amount);
        if(Double.isFinite(prev)&&prev>0){double delta=value-prev;updateValue(change,String.format(Locale.US,"%+.2f  (%+.2f%%)",delta,delta/prev*100));change.setTextColor(delta>=0?ACCENT:Color.rgb(226,158,153));}else{change.setText("Change unavailable");change.setTextColor(MUTED);}
        updateFreshness();Log.i("CommandCenter","QUOTE_OK "+m.getLong("regularMarketTime"));
    }
    static String condition(int code){if(code==0)return "Clear skies";if(code==1)return "Mostly clear";if(code==2)return "Partly cloudy";if(code==3)return "Overcast";if(code<=48)return "Fog";if(code<=57)return "Drizzle";if(code<=67)return "Rain";if(code<=77)return "Snow";if(code<=82)return "Rain showers";if(code<=86)return "Snow showers";return "Thunderstorms";}
    @Override protected void onResume(){super.onResume();visible=true;handler.post(tick);handler.post(feeds);}
    @Override protected void onPause(){visible=false;handler.removeCallbacks(tick);handler.removeCallbacks(feeds);super.onPause();}
    @Override protected void onDestroy(){if(activePanel!=null){panelContent.animate().cancel();activePanel.dismiss();}handler.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy();}
    private void showPanel(String title,String message,String[] labels,Runnable[] actions){
        if(activePanel!=null)return;
        LinearLayout content=column();content.setPadding(dp(24),dp(24),dp(24),dp(20));content.setBackground(surface(CARD,0));
        TextView heading=text(title,24,TEXT);heading.setPadding(0,0,0,dp(16));content.addView(heading);
        if(message!=null){TextView description=text(message,15,MUTED);description.setLineSpacing(dp(4),1);description.setPadding(0,0,0,dp(16));content.addView(description);}
        TextView first=null;
        for(int i=0;i<labels.length;i++){
            final Runnable action=actions[i];TextView item=text(labels[i],16,TEXT);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(16),0,dp(16),0);item.setFocusable(true);item.setClickable(true);item.setBackground(surface(BG,0));
            item.setOnFocusChangeListener((v,focused)->{item.setBackground(surface(focused?TEXT:BG,0));item.setTextColor(focused?BG:TEXT);});
            item.setOnClickListener(v->closePanel(action));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,0,0,dp(8));content.addView(item,p);if(first==null)first=item;
        }
        TextView hint=text("Back to close",11,MUTED);hint.setPadding(0,dp(8),0,0);content.addView(hint);
        presentPanel(content,440,false,first);
    }
    private void showOptions(){
        final boolean keep=(getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0;
        final boolean reduced=getPreferences(0).getBoolean("reduce_motion",false);
        showPanel("Options",null,new String[]{reduced?"Enable subtle motion":"Reduce motion",keep?"Allow TV to sleep normally":"Keep dashboard awake","Refresh weather and CRM","About this dashboard"},new Runnable[]{
            ()->{getPreferences(0).edit().putBoolean("reduce_motion",!reduced).apply();Toast.makeText(this,reduced?"Subtle motion enabled":"Motion reduced",Toast.LENGTH_SHORT).show();},
            ()->{if(keep)getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getPreferences(0).edit().putBoolean("keep_awake",!keep).apply();},
            ()->{refresh();},
            ()->showPanel("About Today","Weather: Open-Meteo (CC BY 4.0), Charlotte area for ZIP 28277.\n\nQuotes: Yahoo Finance; may be delayed. Daily change is relative to the previous close. CRM is checked every 30 seconds and weather every minute. Weather uses 15-minute model data. Check times do not guarantee fresh source data. Failed checks retry with backoff. Quotes cover the regular session; no extended-hours feed.\n\nCommand Center 0.6",new String[]{"Done"},new Runnable[]{()->{}})
        });
    }
    private boolean isControlsKey(int key){return key==KeyEvent.KEYCODE_MENU||key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER;}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(isControlsKey(key))return true;return super.onKeyDown(key,event);}
    @Override public boolean onKeyUp(int key,KeyEvent event){if(isControlsKey(key)){if(!event.isCanceled())showControls();return true;}return super.onKeyUp(key,event);}
}
