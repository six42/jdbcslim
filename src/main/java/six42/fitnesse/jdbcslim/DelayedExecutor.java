package six42.fitnesse.jdbcslim;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


public class DelayedExecutor {

  private java.lang.Thread serviceThread;
  
  public DelayedExecutor(final Connection cnn, final String cmd, final long delay) throws IOException {
    serviceThread = new Thread(
            new Runnable() {
              @Override
              public void run() {
                
                System.out.println("Delayed Executor:Starting:" + String.valueOf(delay) + ":" + cmd);
                try {
                  Thread.sleep(delay);
                } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                System.out.println("Delayed Executor:Executing:" + String.valueOf(delay) + ":" + cmd);
                int result = 0;
                try {
                  result = cnn.prepareStatement(cmd).executeUpdate();
                } catch (SQLException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                System.out.println("Delayed Executor:Finished:" + String.valueOf(delay) + ":" + cmd + ":result:" + result);
              }
            }
    );
    serviceThread.setDaemon(true);
    serviceThread.start();
  }
}
