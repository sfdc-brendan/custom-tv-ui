package local.commandcenter.tv;

/** Immutable timestamped samples. Missing values remain gaps, never interpolated data. */
final class TrendSeries {
    final long[] times; final double[] values; final long start,end,gap;
    final int count; final double min,max;
    TrendSeries(long[] times,double[] values,long start,long end,long gap){
        if(times.length!=values.length||end<=start)throw new IllegalArgumentException("Invalid chart domain");
        this.times=times.clone();this.values=values.clone();this.start=start;this.end=end;this.gap=gap;
        int n=0;double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
        for(int i=0;i<times.length;i++){
            if(i>0&&times[i]<=times[i-1])throw new IllegalArgumentException("Unordered chart times");
            if(valid(i)){n++;lo=Math.min(lo,values[i]);hi=Math.max(hi,values[i]);}
        }
        count=n;min=lo;max=hi;
    }
    boolean valid(int i){return times[i]>=start&&times[i]<=end&&Double.isFinite(values[i]);}
    boolean joins(int i){return i>0&&valid(i-1)&&valid(i)&&times[i]-times[i-1]<=gap;}
}
