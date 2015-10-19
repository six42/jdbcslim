package six42.fitnesse.jdbcslim;

public class RetryManager {

  private boolean allFieldsPassedNoRetry = true;
  private long maxTotalTimeMs;
  private long intervallTimeMs;
  private long startTimeMs;
  private long retryCount=0;
  private long maxRetryCount =0;
  private long loopStartTimeMs;
  
  public RetryManager(String cmd) {
   
    if(cmd.isEmpty()){
      init(1,3000);
      return;
    }
    
    String [] paramValues = cmd.split(":");
    long maxRetryCount = paramValues.length < 1 ? 1 : Long.parseLong( paramValues[0]);
    long maxTotalTimeMs = paramValues.length < 2 ? 3000 : Long.parseLong( paramValues[1]);
    
    init(maxRetryCount,maxTotalTimeMs);
  }
  public RetryManager(long maxRetryCount, long maxTotalTimeMs){
    init(maxRetryCount, maxTotalTimeMs);
  }
  
  private void init(long maxRetryCount, long maxTotalTimeMs){
    this.maxTotalTimeMs = maxTotalTimeMs;
    if (maxRetryCount <=0) maxRetryCount =1;
    this.maxRetryCount = maxRetryCount;

    this.intervallTimeMs = this.maxTotalTimeMs/ this.maxRetryCount;
    
    this.startTimeMs = System.currentTimeMillis();
    this.loopStartTimeMs = startTimeMs;
    this.retryCount=0;
    this.allFieldsPassedNoRetry = true;
  }

  public boolean shouldTryAgain() {
    long endTimeMs = System.currentTimeMillis();

    // Always one run
    if (retryCount==0) return prepareNextRetry();

    // Successful run no retry required
    if( allFieldsPassedNoRetry) return false;

    if (retryCount >= maxRetryCount) return false;

    // Time limit reached -> stop retry
    if((endTimeMs - startTimeMs) >= maxTotalTimeMs) return false;
    
    
    // another run is required -> slow down the process 
    long  sleepTimeMs = intervallTimeMs - (endTimeMs - loopStartTimeMs);
    if(sleepTimeMs > 0)
      try {
        Thread.sleep(intervallTimeMs);
      } catch (InterruptedException e) {
        // Should not happen - print and ignore
        e.printStackTrace();
      }
    
     return prepareNextRetry();
  }

  private boolean prepareNextRetry (){
    retryCount++;
    allFieldsPassedNoRetry=true;
    this.loopStartTimeMs = System.currentTimeMillis();
    return true;
  }

  
  public void runFailed(){
    this.allFieldsPassedNoRetry = false;
  }

  @Override
  public String toString() {
    return toString("");
  }

  public String toString(String context) {
    return "RetryManager " + context + " [allFieldsPassedNoRetry=" + allFieldsPassedNoRetry
        + ", maxTotalTimeMs=" + maxTotalTimeMs + ", intervallTimeMs="
        + intervallTimeMs + ", startTimeMs=" + startTimeMs + ", retryCount="
        + retryCount + ", maxRetryCount=" + maxRetryCount
        + ", loopStartTimeMs=" + loopStartTimeMs + "]";
  }
}
