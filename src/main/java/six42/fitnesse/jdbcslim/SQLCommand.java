// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.*;

public class SQLCommand extends SheetCommandBase {

  private final String defaultUpdateCountHeaderName = "Count";
  private final String disabledValue = "false";
	
	private Connection dbConnection;
  private boolean mustCloseConnection;
	static private Map<String, Connection> theConnections = new HashMap<String, Connection>();

	private Map<String, String> rowValues = new HashMap<String, String>(); 

 	private Integer cacheMaxLoops;
	
	public SQLCommand(String configurationOptions, String rawCommand,  String outputFormatOptions) throws FileNotFoundException, IOException{
		super(configurationOptions, rawCommand, outputFormatOptions);
	}

	public SQLCommand(String configurationOptions, String rawCommand) throws FileNotFoundException, IOException{
		super(configurationOptions, rawCommand);
	}

	public SQLCommand(String configurationOptions) throws FileNotFoundException, IOException{
		super(configurationOptions);
	}

  @Override
  public void reset() {
    rowValues = new HashMap<String, String>();
  }

  @Override
  public void set(String columnName, String value) {
    //keys from Properties are always lower case
    rowValues.put(columnName.toLowerCase(), value);
  }

  @Override
  public String get(String columnName) {
    // TODO Auto-generated method stub
    return null;
  }
  
 

	@Override
	public void execute() {

		if (Properties().isDebug()) System.out.println("begin execute:"+ success + ":" + command);
		
		if (success) {
			try{
				resultSheet = dbExecute(command);
				rawResult = null;
			} catch (SQLException e) {
			  System.err.println("Got Exception in SQLCommand dbExecute:" + e.getMessage());
				success = false;
				rawResult = "Database execution failed:" + e.getMessage();
				resultSheet = null;
				return;
			}

		}
		else{
			rawResult = "Skipped this command as a previous command failed.";
		}
	}

	private List<List<String>> dbExecute(String sqlCommand) throws SQLException{
		Statement stmt;
		CallableStatement cstmt = null;
		boolean resultsAvailable;
		
    List<List<String>> paramterList = null;
		List<List<String>> resultTable = new ArrayList<List<String>>();

		
    String updateCountHeaderName = Properties().getPropertyOrDefault(ConfigurationParameters.dbUpdateCount, defaultUpdateCountHeaderName);
    String nullInputValueStr = Properties().getPropertyOrDefault(ConfigurationParameters.inputNullString, Properties().getPropertyOrDefault(ConfigurationParameters.outputNullString, "#null#"));

    long startTime = System.currentTimeMillis();

    String parameterName = Properties().getPropertyOrDefault(ConfigurationParameters.dbQueryParameters, "");
    if (!parameterName.isEmpty()){
        PropertiesLoader queryParameters = Properties().getSubProperties(parameterName);
        cstmt = dbConnection.prepareCall(sqlCommand);
        paramterList = queryParameters.toTable();
        //Skip the header start i at 1
        for( int i =1 ; i < paramterList.size(); i++){
          try{
            String key = paramterList.get(i).get(0);
            String [] paramValues = paramterList.get(i).get(1).split(":");
            boolean inParameter = paramValues.length < 1 ? false : paramValues[0].toUpperCase().contains("I");
            boolean outParameter = paramValues.length < 1 ? false : paramValues[0].toUpperCase().contains("O");
            int parameterIndex = paramValues.length < 2 ? 0 : Integer.parseInt( paramValues[1]);
            int sqlType = paramValues.length < 3 ? 0 : Integer.parseInt( paramValues[2]);
            int scale = paramValues.length < 4 ? -1 : Integer.parseInt( paramValues[3]);

            if (inParameter && rowValues.containsKey(key)){ 
              Object obj = rowValues.get(key);
              String value = null;
              if(obj != null){
                value = obj.toString();
                if (nullInputValueStr.equalsIgnoreCase(value)) value = null;
              }
              
              if(scale == -1)cstmt.setObject(parameterIndex, value, sqlType); 
              else cstmt.setObject(parameterIndex, value, sqlType, scale);
            }
            if(outParameter){
              if(scale == -1)  cstmt.registerOutParameter(parameterIndex, sqlType);
              else         cstmt.registerOutParameter(parameterIndex, sqlType, scale);
            }
          }catch( NumberFormatException e){
            throw new RuntimeException("Failed processing Query Parameter:"+ paramterList.get(i).get(0) + "=" + paramterList.get(i).get(1), e);
          }catch( SQLException e){
            throw new RuntimeException("Failed setting Query Parameter:"+ paramterList.get(i).get(0) + "=" + paramterList.get(i).get(1), e);
          }

        }
        
        // Restart the time to just measure the execution time
        startTime = System.currentTimeMillis();
        resultsAvailable = cstmt.execute();
        stmt = cstmt;

    }else{
      // Statement without DB Parameters
      stmt = dbConnection.createStatement();
      resultsAvailable = stmt.execute(sqlCommand);

    }

    float perfExecutionTimeSeconds = new Float(System.currentTimeMillis() - startTime)/1000;

    
		
    startTime = System.currentTimeMillis();
    resultTable =getResultSetsAndUpdateCounts(stmt, resultsAvailable,  updateCountHeaderName,
        Properties().getBooleanPropertyOrDefault(ConfigurationParameters.outputMultipleRecordsetsAsExtraColumns, true));

    float perfRetrieveTimeSeconds = new Float(System.currentTimeMillis() - startTime)/1000;

    String perfHeader;
    perfHeader = Properties().getPropertyOrDefault(ConfigurationParameters.dbPerf, disabledValue);
    if(!disabledValue.equalsIgnoreCase(perfHeader)){
      resultTable.get(0).add(perfHeader);
      resultTable.get(1).add(String.valueOf(perfRetrieveTimeSeconds+perfExecutionTimeSeconds));
    }
    perfHeader = Properties().getPropertyOrDefault(ConfigurationParameters.dbPerfExecution, disabledValue);
    if(!disabledValue.equalsIgnoreCase(perfHeader)){
      resultTable.get(0).add(perfHeader);
      resultTable.get(1).add(String.valueOf(perfExecutionTimeSeconds));
    }
    perfHeader = Properties().getPropertyOrDefault(ConfigurationParameters.dbPerfRetrieval, disabledValue);
    if(!disabledValue.equalsIgnoreCase(perfHeader)){
      resultTable.get(0).add(perfHeader);
      resultTable.get(1).add(String.valueOf(perfRetrieveTimeSeconds));
    }
    
    
    // Get Identity columns if flag has been set
    if(Properties().getBooleanPropertyOrDefault(ConfigurationParameters.dbGetgeneratedKeys, false)){
      if (Properties().isDebug()) System.out.println("Processing Generated Keys:");
      try{
        ResultSet gkrs = stmt.getGeneratedKeys();
        convertRsIntoTable(resultTable, gkrs, true, disabledValue);
      }catch (Exception e){
        if (Properties().isDebug()) System.out.println("Failed to get Generated Keys:" + e.getMessage());
      }
    }
    

    if(paramterList != null){
      //Skip the header start i at 1
      for( int i =1 ; i < paramterList.size(); i++){
        try{
          String [] paramValues = paramterList.get(i).get(1).split(":");           
          boolean outParameter = paramValues.length < 1 ? false : paramValues[0].toUpperCase().contains("O");
          int parameterIndex = paramValues.length < 2 ? 0 : Integer.parseUnsignedInt( paramValues[1]);

          if (outParameter){ 
            String value = "";
            try{
              Object o = cstmt.getObject(parameterIndex);
              value = (o == null) ? null : o.toString();
            }catch (SQLException e){
              value = e.getMessage();
            }
            resultTable.get(0).add(paramterList.get(i).get(0));
            resultTable.get(1).add(value);
          }
        }catch( NumberFormatException e){
          throw new RuntimeException("Failed processing Output Parameter:"+ paramterList.get(i).get(0) + "=" + paramterList.get(i).get(1), e);
        }
      }
    }   


    
    // Empty Header columns - remove the top 2 empty rows
    if(resultTable.get(0).size()==0){
      resultTable.remove(0);
      resultTable.remove(0);     
    }
    return resultTable;
	}

  private  List<List<String>> getResultSetsAndUpdateCounts(Statement stmt, boolean resultSetAvailable,  String updateCountHeaderName, boolean extend) throws SQLException
  {
 
     List<List<String>> results = new ArrayList<List<String>>();
     results.add(new ArrayList<String>());// Empty Header
     results.add(new ArrayList<String>());// Empty First Row

     boolean supportsMultiResultSets = dbConnection.getMetaData().supportsMultipleResultSets();
     if (Properties().isDebug()) System.out.println("supportsMultiResultSets:" + supportsMultiResultSets + "; Additional Sets will be added as Columns:" + extend);

     String summaryHeader = Properties().getPropertyOrDefault(ConfigurationParameters.dbOnlyRowCount, disabledValue);

     int updateColumnCounter = 1;
     String updateColumnCounterStr="";
     boolean moreResultsReceived = false;
     int updateCount = 0; // set to something <> -1
     
     // To avoid an endless loop in case a JDBC driver is badly written we limit the number of loops
     int l;
     for(l=0; l < getMaxLoops(); l++){
        if (resultSetAvailable) {
          ResultSet res = null;
          res = stmt.getResultSet();
          if (null != res){
            moreResultsReceived = true;
            convertRsIntoTable(results, res, extend, summaryHeader);
          }
        }else{
          moreResultsReceived = false;
          updateCount = stmt.getUpdateCount();
          if (-1 != updateCount && !updateCountHeaderName.equalsIgnoreCase(disabledValue)){
            results.get(0).add(updateCountHeaderName+ updateColumnCounterStr);
            results.get(1).add(Integer.toString(updateCount));
            updateColumnCounterStr = Integer.toString(++updateColumnCounter);
          }
        }

        if (!supportsMultiResultSets) break;
        if (!moreResultsReceived && -1 == updateCount) break;
        // Get for the next loop
        resultSetAvailable = stmt.getMoreResults();
     }
     if(l>= getMaxLoops()){
       //The max loop limit was reached, this should never happen so we raise an exception
       throw new RuntimeException("The command returned more than '" + l +"' recordsets or update count results and execution was aborted by jdbcSlim. " + 
           "Set the parameter 'jdbcMaxloops' to a higher value to be able to finish your command.");
     }
     
     return results;
  }

  
  protected void convertRsIntoTable(List<List<String>> resultTable,  ResultSet rs, boolean extend, String summaryHeader) throws SQLException {
    ResultSetMetaData rsmd;
    int columnCount;
    int rowCount =0;
    List<String> oneRow;
    rsmd = rs.getMetaData();
    columnCount = rsmd.getColumnCount();
    if (Properties().isDebug()) System.out.println("HeaderCount:"+ columnCount) ;

    boolean summaryOnly = !disabledValue.equalsIgnoreCase(summaryHeader);

    
    oneRow = new ArrayList<String>();
    for (int i = 1; i <= columnCount; i++){
      String columnHeader = rsmd.getColumnLabel(i);
      if (columnHeader == null || Properties().getBooleanPropertyOrDefault(ConfigurationParameters.dbUseColumnName, false)){
        columnHeader = rsmd.getColumnName(i);
      }
    	oneRow.add(columnHeader);
    	if (Properties().isDebug()) System.out.println("Header:"+ i + ":" + columnHeader);
    }
    appendOrExtendRow(resultTable, oneRow, rowCount++, extend, summaryOnly);
    
    while(rs.next()){
    	oneRow = new ArrayList<String>();
    	for (int i = 1; i <= columnCount; i++){
    		oneRow.add(rs.getString(i));
    		if (Properties().isDebug()) System.out.println("Row:"+ i + ":" + rs.getString(i));
    	}
      appendOrExtendRow(resultTable, oneRow, rowCount++, extend, summaryOnly);
    }
    //rs.close();
    
    if(summaryOnly){
        resultTable.get(0).add(summaryHeader);
        resultTable.get(1).add(String.valueOf(rowCount-1));
    }
  }

  protected List<List<String>> appendOrExtendRow(List<List<String>> resultTable, List<String> oneRow, int rowCount, boolean extend, boolean summaryOnly){
    // If a summary is requested nothing must be done
    if (summaryOnly) return resultTable;
    
    if(extend && resultTable.size() > rowCount){
      resultTable.get(rowCount).addAll(oneRow);
    }else{
      resultTable.add(oneRow);
    }
    return resultTable;
  }
      
  @Override
	public void beginTable() {
	  success= openConnection();
	  if(!success){
      throw new RuntimeException("No Connection after beginTable: " + rawResult );
	  }
	}

	@Override
	public void endTable() {
	  if(mustCloseConnection){
	    if(!closeConnection()){
	      throw new RuntimeException("Connection close failed in endTable: " + rawResult );
	    }
	  }
	}

	/**
	 * Use in scripts or scenarios to open a connection
	 * @return true if the database connection could be opened
	 */
	public boolean openConnection(){
    String dbConnectionName = "";
    
    dbConnection = null;
    
    dbConnectionName = Properties().getProperty(ConfigurationParameters.dbConnection);
    if(dbConnectionName == null){
      mustCloseConnection = true;
      success= openNewConnection();
      return success;
    }
    else{
      mustCloseConnection = false;
      if(theConnections.containsKey(dbConnectionName)){
        dbConnection = theConnections.get(dbConnectionName);
        success=  (dbConnection != null);
        return success;
      }else{
        success = openNewConnection();
        if (success){
          theConnections.put(dbConnectionName, dbConnection);
        }else{
          theConnections.put(dbConnectionName, null);
        }
        return success;
      }
      
    }
	  
	}
	  private boolean openNewConnection(){
    String jdbcDriver = "";
    String dbUrl ="";
    String dbUser ="";
    String dbPassword = "";
    String dbPropertiesName = "";
    Boolean success;
    java.util.Properties dbProperties = null; 

    jdbcDriver = Properties().getProperty(ConfigurationParameters.jdbcDriver);
    dbPropertiesName =Properties().getPropertyOrDefault(ConfigurationParameters.dbProperties, "");
    dbUrl = Properties().getProperty(ConfigurationParameters.dbUrl);
    dbUser =Properties().getProperty(ConfigurationParameters.dbUser);
    dbPassword = Properties().getSecretProperty(ConfigurationParameters.dbPassword);
    if(!dbPropertiesName.isEmpty()){
      dbProperties =  Properties().getSubProperties(dbPropertiesName).toProperties();
      if(jdbcDriver == null || dbProperties == null){
        success = false;
        rawResult = "Db Configuration is not complete. Required are  'jdbcDriver' (" + jdbcDriver + ") 'dbProperties' (" + dbPropertiesName + ")";
        return success;
        
      }
    }else if (jdbcDriver == null || dbUrl == null || dbUser == null || dbPassword == null){
      success = false;
      rawResult = "Db Configuration is not complete. Required are  'jdbcDriver' (" + jdbcDriver + ") 'dbURL' (" + dbUrl + ") 'dbUser' (" + dbUser + ") 'dbPassword'";
      return success;
        
    }
    try {
      Class.forName(jdbcDriver);
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
      success = false;
      rawResult = "JDBC Driver (" + jdbcDriver + ") not found.";
      return success;
    }
    if(dbProperties!=null){
      try {
        dbConnection= DriverManager.getConnection(dbUrl, dbProperties);
      } catch (SQLException e) {
        e.printStackTrace();
        success = false;
        rawResult = "Connect failed to db (" + dbUrl + ") with properties from (" + dbPropertiesName + ") :" + e.getMessage() ;
        dbConnection =null;
        return success;
      }
      
    }else{
      try {
        dbConnection= DriverManager.getConnection(dbUrl, dbUser, dbPassword);
      } catch (SQLException e) {
        e.printStackTrace();
        success = false;
        rawResult = "Connect failed to db (" + dbUrl + ") as (" + dbUser + ") :" + e.getMessage() ;
        dbConnection =null;
        return success;
      }
    }
    Boolean autoCommit = Properties().getBooleanPropertyOrDefault(ConfigurationParameters.dbAutoCommit, true);
    if (Properties().isDebug()){
      Boolean ac2 = null;
      try {
        ac2 = dbConnection.getAutoCommit();
      } catch (SQLException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      System.out.println("Open Connection - autocommit:"+ autoCommit + ":" + ac2 + ":" + command);
    }
    try {
      dbConnection.setAutoCommit(autoCommit);
    } catch (SQLException e1) {
      success = false;
      rawResult = "Setting Auto Commit to '"+ autoCommit +"' failed:" + e1.getMessage();
      resultSheet = null;
      return success;
    }

    success = true;
    return success;
	}
	
	/**
	 * Use in scripts or scenarios to close a connection
	 * @return true if the database connection could be closed
	 */
	public boolean closeConnection(){
    Boolean success = true;
	   if (dbConnection !=null){
	      try {
	        dbConnection.close();
	      } catch (SQLException e) {
	        e.printStackTrace();
	        success = false;
	        this.success = false;
	        rawResult = "Close connection failed:" + e.getMessage() ;
	      }
	      String dbConnectionName = Properties().getProperty(ConfigurationParameters.dbConnection);
	      if(dbConnectionName != null){
	        theConnections.remove(dbConnectionName);
	      }

	    }

		return success;
	}
	
  /**
   * Use in scripts or scenarios to add a connection
   * to the list of named connections
   */
	public void addConnection(String key, Connection cnn){
	  theConnections.put(key, cnn);
	}

  /**
   * Use in scripts or scenarios to remove a connection
   * from the list of named connections
   * Close the connection before removing it.
   */
  public void removeConnection(String key){
    theConnections.remove(key);
  }

  /**
   * Only added for demonstration purpose in the user manual
   * Don't use in your applications!
   * 
   * @param dbUrl
   * @param dbUser
   * @param dbPassword
   * @return
   * @throws SQLException
   */
  public Connection testHomeMadeConnection(String dbUrl, String dbUser, String dbPassword) throws SQLException{
    return DriverManager.getConnection(dbUrl, dbUser, dbPassword);

  }

  private int getMaxLoops() {
    if (cacheMaxLoops == null){
      String strValue = Properties().getPropertyOrDefault(ConfigurationParameters.jdbcMaxloops, "100");
      try{
        cacheMaxLoops=Integer.parseInt(strValue);
      }catch (NumberFormatException e){
        cacheMaxLoops = 1;
        throw new RuntimeException("The value set for " + ConfigurationParameters.jdbcMaxloops.toString() + " must be a number. Got '" + strValue + "'", e); 
      }
    }
    return cacheMaxLoops;
  }

}
