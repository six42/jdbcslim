// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.*;
import java.sql.Types;

public class SQLCommand extends SheetCommandBase {

  private final String defaultUpdateCountHeaderName = "Count";
  private final String disabledValue = "false";
	
	private Connection dbConnection;
  private boolean mustCloseConnection;
	static private Map<String, Connection> theConnections = new HashMap<String, Connection>();
	
	private Integer maxGetUpdateloops;
	
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
	public void execute() {

		if (Properties().isDebug()) System.out.println("begin execute:"+ success + ":" + command);
		
		if (success) {
			try{
				resultSheet = dbExecute(command);
				rawResult = null;
			} catch (SQLException e) {
			  System.err.println("Got Exception in SQLCommand dbExecute:" + e.getMessage());
				e.printStackTrace();
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
		ResultSet rs;
		Statement stmt;
		CallableStatement cstmt = null;
		boolean resultsAvailable;
		
    List<List<String>> paramterList = null;
		List<List<String>> resultTable = new ArrayList<List<String>>();

		
    String updateCountHeaderName = Properties().getPropertyOrDefault(ConfigurationParameters.dbUpdateCount, defaultUpdateCountHeaderName);

    
    String parameterName = Properties().getPropertyOrDefault(ConfigurationParameters.dbQueryParameters, "");
    if (!parameterName.isEmpty()){
      PropertiesLoader queryParameters = null;
      queryParameters = new PropertiesLoader();
        try {
          queryParameters.loadFromDefintionOrFile(parameterName );
        } catch (FileNotFoundException e) {
          throw new RuntimeException("The db query parameters (" + parameterName + ") could not be loaded: " + e.getMessage() );
        } catch (IOException e) {
          throw new RuntimeException("The db query parameters (" + parameterName + ") could not be loaded: " + e.getMessage() );
        }
        cstmt = dbConnection.prepareCall(sqlCommand);
        paramterList = queryParameters.toTable();
        //Skip the header start i at 1
        for( int i =1 ; i < paramterList.size(); i++){
          try{
            String [] paramValues = paramterList.get(i).get(1).split(":");           
            int parameterIndex = paramValues.length < 1 ? 0 : Integer.parseUnsignedInt( paramValues[0]);
            int sqlType = paramValues.length < 2 ? 0 : Integer.parseInt( paramValues[1]);
            int scale = paramValues.length < 3 ? 0 : Integer.parseUnsignedInt( paramValues[2]);

            if (sqlType == 0){ /*TODO */ cstmt.setObject(parameterIndex, 9.0); }
            else if(scale == 0)         cstmt.registerOutParameter(parameterIndex, sqlType);
            else         cstmt.registerOutParameter(parameterIndex, sqlType, scale);

          }catch( NumberFormatException e){
            System.out.println("Failed processing Query Parameter:"+ paramterList.get(i).get(0) + "=" + paramterList.get(i).get(1) + " ->" + e.getMessage() ) ;
          }

        }   

        resultsAvailable = cstmt.execute();
        stmt = cstmt;

    }else{
      // Statement without DB Parameters
      stmt = dbConnection.createStatement();
      resultsAvailable = stmt.execute(sqlCommand);

    }
    
    String updateCountStr;
    if (!updateCountHeaderName.equalsIgnoreCase(disabledValue)){
      updateCountStr = getUpdateCount(stmt);
    }else{ 
      updateCountStr ="";
    }
			
		
		if (Properties().isDebug()) System.out.println("result available:"+ resultsAvailable + ":" + sqlCommand) ;
    while (resultsAvailable){
      rs = stmt.getResultSet();
      convertRsIntoTable(resultTable, rs);
      resultsAvailable = stmt.getMoreResults();
      if (Properties().isDebug()) System.out.println("result available:"+ resultsAvailable );
    }

    // Get Identity columns
    if(!Properties().getPropertyOrDefault(ConfigurationParameters.dbGetgeneratedKeys, disabledValue).equalsIgnoreCase(disabledValue)){
      if (Properties().isDebug()) System.out.println("Processing Generated Keys:");
      try{
        ResultSet gkrs = stmt.getGeneratedKeys();
        convertRsIntoTable(resultTable, gkrs);
      }catch (Exception e){
        if (Properties().isDebug()) System.out.println("Failed to get Generated Keys:" + e.getMessage());
      }
    }
    
		if (resultTable.size() == 0){
		  // No result sets add two empty lines for Header and Data
		  resultTable.add(new ArrayList<String>());
      resultTable.add(new ArrayList<String>());
		}
    if(!updateCountStr.isEmpty()){
      resultTable.get(0).add(updateCountHeaderName);
      resultTable.get(1).add(updateCountStr);
    }		

    if(paramterList != null){
      //Skip the header start i at 1
      for( int i =1 ; i < paramterList.size(); i++){
        try{
          String [] paramValues = paramterList.get(i).get(1).split(":");           
          int parameterIndex = paramValues.length < 1 ? 0 : Integer.parseUnsignedInt( paramValues[0]);
          int sqlType = paramValues.length < 2 ? 0 : Integer.parseInt( paramValues[1]);

          if (sqlType == 0){ /* do nothing input parameter */}
          else{
            String value = "";
            try{
              value =  cstmt.getObject(parameterIndex).toString();
            }catch (SQLException e){
              value = e.getMessage();
            }
            try{
              value =  Double.toString(cstmt.getDouble(parameterIndex));
            }catch (SQLException e){
              value = e.getMessage();
            }
            resultTable.get(0).add(paramterList.get(i).get(0));
            resultTable.get(1).add(value);
          }
        }catch( NumberFormatException e){
          System.out.println("Failed processing Query Parameter:"+ paramterList.get(i).get(0) + "=" + paramterList.get(i).get(1) + " ->" + e.getMessage() ) ;
        }
      }
    }   


    
    // No Header columns - return nothing instead of two empty rows
    if(resultTable.get(0).size()==0){
      resultTable = new ArrayList<List<String>>();
    }
    return resultTable;
	}


  protected void convertRsIntoTable(List<List<String>> resultTable,  ResultSet rs) throws SQLException {
    ResultSetMetaData rsmd;
    int columnCount;
    List<String> oneRow;
    rsmd = rs.getMetaData();
    columnCount = rsmd.getColumnCount();
    if (Properties().isDebug()) System.out.println("HeaderCount:"+ columnCount) ;
    
    oneRow = new ArrayList<String>();
    for (int i = 1; i <= columnCount; i++){
    	oneRow.add(rsmd.getColumnName(i));
    	if (Properties().isDebug()) System.out.println("Header:"+ i + ":" + rsmd.getColumnName(i));
    }
    resultTable.add(oneRow);
    while(rs.next()){
    	oneRow = new ArrayList<String>();
    	for (int i = 1; i <= columnCount; i++){
    		oneRow.add(rs.getString(i));
    		if (Properties().isDebug()) System.out.println("Row:"+ i + ":" + rs.getString(i));
    	}
    	resultTable.add(oneRow);
    }
    rs.close();
  }

  protected String getUpdateCount(Statement stmt) throws SQLException {
    List<Integer> updateCountList= new ArrayList<Integer>();
    int i=0;
    int maxLoops = getMaxGetUpdateCountLoops();
		while(i++ < maxLoops){
		  int updateCount = stmt.getUpdateCount();
	    if(updateCount != -1){
	      updateCountList.add(updateCount);
	    }else{
	      break;
	    }
		}
    String updateCountStr = updateCountList.toString().replace("[","").replace("]", "").trim(); //remove the list brackets
    return updateCountStr;
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
    Boolean success;
    
    jdbcDriver = Properties().getProperty(ConfigurationParameters.jdbcDriver);
    dbUrl = Properties().getProperty(ConfigurationParameters.dbUrl);
    dbUser =Properties().getProperty(ConfigurationParameters.dbUser);
    dbPassword = Properties().getSecretProperty(ConfigurationParameters.dbPassword);
    if (jdbcDriver == null || dbUrl == null || dbUser == null || dbPassword == null){
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
    try {
      dbConnection= DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    } catch (SQLException e) {
      e.printStackTrace();
      success = false;
      rawResult = "Connect failed to db (" + dbUrl + ") as (" + dbUser + ") :" + e.getMessage() ;
      dbConnection =null;
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

  public int getMaxGetUpdateCountLoops() {
    if (maxGetUpdateloops == null){
      String strValue = Properties().getPropertyOrDefault(ConfigurationParameters.jdbcMaxGetUpdateCountloops, "1");
      try{
        maxGetUpdateloops=Integer.parseInt(strValue);
      }catch (NumberFormatException e){
        maxGetUpdateloops = 1;
        throw new RuntimeException("The value set for " + ConfigurationParameters.jdbcMaxGetUpdateCountloops.toString() + " must be a number. Got '" + strValue + "'", e); 
      }
    }
    return maxGetUpdateloops;
  }

}
