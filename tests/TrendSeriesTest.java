package local.commandcenter.tv;
public final class TrendSeriesTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        TrendSeries s=new TrendSeries(new long[]{0,60000,120000,180000,360000,420000},new double[]{100,101,Double.NaN,102,103,104},0,360000,90000);
        check(s.count==4&&s.min==100&&s.max==103,"Only finite in-domain samples affect bounds");
        check(s.joins(1),"Adjacent minute samples connect");
        check(!s.joins(2)&&!s.joins(3),"Null price leaves a gap on both sides");
        check(!s.joins(4),"Missing timestamps must not be bridged");
        check(!s.valid(5),"After-session sample must not stretch regular-session chart");
        TrendSeries flat=new TrendSeries(new long[]{0,60000},new double[]{100,100},0,60000,90000);
        check(flat.count==2&&flat.max==flat.min,"Flat markets remain valid");
        TrendSeries missing=new TrendSeries(new long[]{0},new double[]{Double.NaN},0,60000,90000);
        check(missing.count==0,"Empty feed is an unavailable chart, not a zero price");
        boolean rejected=false;try{new TrendSeries(new long[]{60000,0},new double[]{1,2},0,60000,90000);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Unordered times must not draw backward across the chart");
        System.out.println("PASS: time domains, missing-value/time gaps, valid range, flat/empty data, timestamp order");
    }
}
