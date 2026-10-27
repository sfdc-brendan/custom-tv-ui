package local.commandcenter.tv;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** Lightweight chart with a real time axis; future weather is dashed, missing prices are gaps. */
final class TrendChart extends View {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean weather;
    private TrendSeries series;
    private long now;
    private int lineColor=MainActivity.ACCENT;
    private final float density;
    TrendChart(Context context,boolean weather){super(context);this.weather=weather;density=getResources().getDisplayMetrics().density;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void data(TrendSeries value,int color){series=value;lineColor=color;setContentDescription(value==null||value.count==0?"Trend unavailable":(weather?"Hourly modeled temperature":"One-minute CRM closing prices")+", "+value.count+" samples");invalidate();}
    void time(long value){if(value/60000!=now/60000){now=value;invalidate();}}
    private float d(float v){return v*density;}
    private String amount(double v){return weather?String.format(Locale.US,"%.0f°",v):String.format(Locale.US,"$%.2f",v);}
    private void dashedLine(Canvas c,float x1,float y1,float x2,float y2){
        // Draw short segments explicitly: older Fire TV GPUs can ignore dashed Paint effects on lines.
        float dx=x2-x1,dy=y2-y1,length=(float)Math.hypot(dx,dy);
        for(float at=0;at<length;at+=d(7)){
            float until=Math.min(length,at+d(3));
            c.drawLine(x1+dx*at/length,y1+dy*at/length,x1+dx*until/length,y1+dy*until/length,ink);
        }
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);ink.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));ink.setTextSize(d(9));ink.setColor(MainActivity.MUTED);ink.setStyle(Paint.Style.FILL);ink.setPathEffect(null);
        if(series==null||series.count==0){c.drawText("Trend unavailable",d(1),d(35),ink);return;}
        float left=d(weather?28:46),right=getWidth()-d(5),top=d(7),bottom=getHeight()-d(22);
        if(right<=left||bottom<=top)return;
        double spread=series.max-series.min,pad=spread==0?(weather?1:Math.max(.05,series.min*.001)):spread*.08;
        double low=series.min-pad,high=series.max+pad;
        for(int j=0;j<3;j++){
            float y=top+(bottom-top)*j/2;
            ink.setColor(0x20FFFFFF);ink.setStrokeWidth(d(.6f));c.drawLine(left,y,right,y,ink);
            if(j!=1){ink.setColor(MainActivity.MUTED);c.drawText(amount(j==0?high:low),0,y+d(3),ink);}
            long t=series.start+(series.end-series.start)*j/2;
            String label=MainActivity.format("h:mm",t)+(MainActivity.format("a",t).equals("AM")?"a":"p");
            ink.setColor(MainActivity.MUTED);float width=ink.measureText(label),x=j==0?left:j==1?(left+right-width)/2:right-width;
            c.drawText(label,x,getHeight()-d(4),ink);
        }
        int last=-1;ink.setStrokeWidth(d(1.7f));ink.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<series.times.length;i++){
            if(!series.valid(i))continue;
            float x=left+(float)((series.times[i]-series.start)/(double)(series.end-series.start))*(right-left);
            float y=bottom-(float)((series.values[i]-low)/(high-low))*(bottom-top);
            if(series.joins(i)){
                float px=left+(float)((series.times[i-1]-series.start)/(double)(series.end-series.start))*(right-left);
                float py=bottom-(float)((series.values[i-1]-low)/(high-low))*(bottom-top);
                // Split the crossing segment at now, preserving the original straight line.
                if(weather&&series.times[i]>now){
                    float sx=px,sy=py;
                    if(series.times[i-1]<now){float fraction=(float)((now-series.times[i-1])/(double)(series.times[i]-series.times[i-1]));sx=px+(x-px)*fraction;sy=py+(y-py)*fraction;ink.setColor(lineColor);ink.setPathEffect(null);c.drawLine(px,py,sx,sy,ink);}
                    ink.setColor(0xFFA2A5A8);ink.setPathEffect(null);dashedLine(c,sx,sy,x,y);
                }else{ink.setColor(lineColor);ink.setPathEffect(null);c.drawLine(px,py,x,y,ink);}
            }
            if(!series.joins(i)&&(i+1>=series.times.length||!series.joins(i+1))){
                ink.setPathEffect(null);ink.setColor(weather&&series.times[i]>now?MainActivity.MUTED:lineColor);c.drawCircle(x,y,d(1.8f),ink);
            }
            last=i;
        }
        ink.setPathEffect(null);
        if(weather&&now>=series.start&&now<=series.end){
            float x=left+(float)((now-series.start)/(double)(series.end-series.start))*(right-left);
            ink.setColor(0x60FFFFFF);ink.setStrokeWidth(d(.8f));c.drawLine(x,top,x,bottom,ink);
            ink.setColor(MainActivity.MUTED);String tag="Now";float tx=Math.max(left,Math.min(x-d(8),right-ink.measureText(tag)));c.drawText(tag,tx,top+d(10),ink);
        }else if(!weather&&last>=0){
            float x=left+(float)((series.times[last]-series.start)/(double)(series.end-series.start))*(right-left);
            float y=bottom-(float)((series.values[last]-low)/(high-low))*(bottom-top);
            ink.setColor(lineColor);c.drawCircle(x,y,d(2.5f),ink);
        }
    }
}
