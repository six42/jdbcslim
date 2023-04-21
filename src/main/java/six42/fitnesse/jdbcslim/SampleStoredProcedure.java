package six42.fitnesse.jdbcslim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SampleStoredProcedure {

  public static void showTestData(ResultSet[] rs)
    throws SQLException {

    Connection con = DriverManager.getConnection("jdbc:default:connection");
    Statement stmt = null;

    String query =
      "select * from TestData";

    stmt = con.createStatement();
    rs[0] = stmt.executeQuery(query);
  }

  public static ResultSet myquery(Connection conn, String sql) throws SQLException {
    return conn.createStatement().executeQuery(sql);
  }
}
