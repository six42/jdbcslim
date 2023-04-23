package six42.fitnesse.jdbcslim;

import fitnesse.util.Clock;

public class RetryManager {

  private boolean allFieldsPassedNoRetry = true;
  private long maxTotalTimeMs;
  private long intervalTimeMs;
  private long startTimeMs;
  private long retryCount = 0;
  private long maxRetryCount = 0;
  private long loopStartTimeMs;

  public RetryManager(String cmd) {

    if (cmd.isEmpty()) {
      init(1, 3000);
      return;
    }

    String[] paramValues = cmd.split(":");
    long maxRetryCount = paramValues.length < 1 ? 1 : Long.parseLong(paramValues[0]);
    long maxTotalTimeMs = paramValues.length < 2 ? 3000 : Long.parseLong(paramValues[1]);

    init(maxRetryCount, maxTotalTimeMs);
  }

  public RetryManager(long maxRetryCount, long maxTotalTimeMs) {
    init(maxRetryCount, maxTotalTimeMs);
  }

  private void init(long maxRetryCount, long maxTotalTimeMs) {
    this.maxTotalTimeMs = maxTotalTimeMs;
    if (maxRetryCount <= 0) maxRetryCount = 1;
    this.maxRetryCount = maxRetryCount;

    this.intervalTimeMs = this.maxTotalTimeMs / this.maxRetryCount;

    this.startTimeMs = Clock.currentTimeInMillis();
    this.loopStartTimeMs = startTimeMs;
    this.retryCount = 0;
    this.allFieldsPassedNoRetry = true;
  }

  public boolean shouldTryAgain() {
    long endTimeMs = Clock.currentTimeInMillis();

    // Always one run
    if (retryCount == 0) return prepareNextRetry();

    // Successful run no retry required
    if (allFieldsPassedNoRetry) return false;

    if (retryCount >= maxRetryCount) return false;

    // Time limit reached -> stop retry
    if ((endTimeMs - startTimeMs) >= maxTotalTimeMs) return false;


    // another run is required -> slow down the process 
    long sleepTimeMs = intervalTimeMs - (endTimeMs - loopStartTimeMs);
    if (sleepTimeMs > 0)
      try {
        Thread.sleep(intervalTimeMs);
      } catch (InterruptedException e) {
        // Should not happen - print and ignore
        e.printStackTrace();
      }

    return prepareNextRetry();
  }

  private boolean prepareNextRetry() {
    retryCount++;
    allFieldsPassedNoRetry = true;
    this.loopStartTimeMs = Clock.currentTimeInMillis();
    return true;
  }


  public void runFailed() {
    this.allFieldsPassedNoRetry = false;
  }

  @Override
  public String toString() {
    return toString("");
  }

  public String toString(String context) {
    return "RetryManager " + context + " [allFieldsPassedNoRetry=" + allFieldsPassedNoRetry
      + ", maxTotalTimeMs=" + maxTotalTimeMs + ", intervalTimeMs="
      + intervalTimeMs + ", startTimeMs=" + startTimeMs + ", retryCount="
      + retryCount + ", maxRetryCount=" + maxRetryCount
      + ", loopStartTimeMs=" + loopStartTimeMs + "]";
  }
}
