package local.commandcenter.tv;

/** Monotonic polling state, confined to the UI thread. Source time is separate. */
final class FeedState {
    final long interval;
    long nextDue=0,lastSuccess=-1;
    boolean busy=false,failed=false;
    int failures=0;
    FeedState(long interval){this.interval=interval;}
    boolean due(long now){return !busy&&now>=nextDue;}
    void begin(long now){busy=true;nextDue=now+interval;}
    void success(long now){busy=false;failed=false;failures=0;lastSuccess=now;}
    void failure(long now){busy=false;failed=true;failures=Math.min(failures+1,4);nextDue=now+Math.min(300000,interval*(1L<<failures));}
    String status(long now,boolean cached){
        if(failed)return (cached?"Check failed · saved data":"Data unavailable")+" · retry "+Math.max(1,(nextDue-now+999)/1000)+"s";
        if(lastSuccess<0)return cached?"Cached · checking…":"Checking…";
        long age=Math.max(0,(now-lastSuccess)/1000);
        return (age>90?"Check overdue · ":"Checked ")+(age<60?age+"s":age/60+"m")+" ago";
    }
}
