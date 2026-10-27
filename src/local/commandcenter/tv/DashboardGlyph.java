package local.commandcenter.tv;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Small native outline icons; no bitmap assets or animation cost. */
public final class DashboardGlyph extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String kind;
    private int color,weatherCode=0;
    public DashboardGlyph(Context context,String kind,int color){super(context);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void setColor(int value){color=value;invalidate();}
    public void setWeather(int code){weatherCode=code;invalidate();}
    private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,paint);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/24f,getHeight()/24f);
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.6f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        if(kind.equals("home")){
            for(int x=0;x<2;x++)for(int y=0;y<2;y++)canvas.drawRoundRect(new RectF(3+x*11,3+y*11,10+x*11,10+y*11),1.5f,1.5f,paint);
        }else if(kind.equals("tv")){
            canvas.drawRoundRect(new RectF(2,4,22,18),2,2,paint);line(canvas,8,22,16,22);line(canvas,12,18,12,22);
        }else if(kind.equals("play")){
            Path p=new Path();p.moveTo(8,4);p.lineTo(21,12);p.lineTo(8,20);p.close();canvas.drawPath(p,paint);
        }else if(kind.equals("netflix")){
            paint.setStrokeWidth(3.4f);line(canvas,6,21,6,3);line(canvas,6,3,18,21);line(canvas,18,21,18,3);
        }else if(kind.equals("globe")){
            canvas.drawCircle(12,12,9,paint);canvas.drawOval(new RectF(8,3,16,21),paint);line(canvas,3,12,21,12);
        }else if(kind.equals("input")){
            Path p=new Path();p.moveTo(3,6);p.lineTo(21,6);p.lineTo(21,13);p.lineTo(17,18);p.lineTo(7,18);p.lineTo(3,13);p.close();canvas.drawPath(p,paint);
            for(int i=0;i<4;i++)line(canvas,7+i*3.3f,9,7+i*3.3f,11);
        }else if(kind.equals("refresh")){
            canvas.drawArc(new RectF(4,4,20,20),35,285,false,paint);line(canvas,19,3,19,8);line(canvas,19,8,14,8);
        }else if(kind.equals("options")){
            line(canvas,4,6,20,6);line(canvas,4,12,20,12);line(canvas,4,18,20,18);
            paint.setStyle(Paint.Style.FILL);canvas.drawCircle(9,6,2.3f,paint);canvas.drawCircle(16,12,2.3f,paint);canvas.drawCircle(8,18,2.3f,paint);
        }else if(kind.equals("weather")){
            if(weatherCode==0||weatherCode==1){
                canvas.drawCircle(12,12,4,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(canvas,12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10);}
            }else if(weatherCode==45||weatherCode==48){
                line(canvas,4,7,20,7);line(canvas,2,12,17,12);line(canvas,6,17,22,17);
            }else{
                Path p=new Path();p.moveTo(6,18);p.cubicTo(0,18,1,9,7,10);p.cubicTo(8,1,21,3,20,12);p.cubicTo(25,14,22,19,18,18);p.close();canvas.drawPath(p,paint);
                if((weatherCode>=71&&weatherCode<=77)||weatherCode==85||weatherCode==86){
                    line(canvas,7,21,10,24);line(canvas,10,21,7,24);line(canvas,15,21,18,24);line(canvas,18,21,15,24);
                }else if(weatherCode>=95){
                    Path bolt=new Path();bolt.moveTo(14,12);bolt.lineTo(10,18);bolt.lineTo(15,18);bolt.lineTo(12,23);canvas.drawPath(bolt,paint);
                }else if(weatherCode>=51){line(canvas,8,21,7,23);line(canvas,15,21,14,23);}
            }
        }
        canvas.restore();
    }
}
