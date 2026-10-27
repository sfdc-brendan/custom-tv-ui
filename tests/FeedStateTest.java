package local.commandcenter.tv;

public final class FeedStateTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        FeedState quote=new FeedState(30000),weather=new FeedState(60000);
        check(quote.due(0)&&weather.due(0),"Both feeds should fetch immediately");
        quote.begin(0);weather.begin(0);
        check(!quote.due(60000),"A slow request must not overlap itself");
        quote.success(500);
        check(!quote.due(29999)&&quote.due(30000),"Poll cadence is measured from request start");
        check(!weather.due(60000),"Weather may still be pending independently");
        quote.begin(30000);quote.failure(31000);
        check(quote.nextDue==91000&&!quote.due(90000),"Failed quote backs off to 60s");
        check(quote.status(31000,true).contains("saved data"),"Failed cached response must be labeled");
        check(quote.status(31000,false).contains("unavailable"),"No saved data must not imply cached values");
        weather.success(40000);
        check(weather.due(60000),"Quote failure must not delay weather");
        for(int i=0;i<9;i++){quote.begin(1000000);quote.failure(1000000);}
        check(quote.nextDue==1300000,"Retry delay must cap at five minutes");
        quote.begin(1300000);quote.success(1300200);
        check(!quote.failed&&quote.failures==0,"Success clears backoff and error");
        check(quote.status(1305200,true).equals("Checked 5s ago"),"Check age must track successful retrieval, not source timestamp");
        check(quote.status(1400200,true).contains("overdue"),"Successful checks cannot look current forever");
        FeedState restored=new FeedState(60000);
        check(restored.status(0,true).startsWith("Cached"),"Restoring saved content must not mark it freshly checked");
        System.out.println("PASS: independent polling, overlap prevention, backoff/recovery, cache and check-age labels");
    }
}
