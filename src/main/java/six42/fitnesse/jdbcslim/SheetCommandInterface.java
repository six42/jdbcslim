// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.List;
import java.util.Set;

public interface SheetCommandInterface  {

	  public void setCommand(String Command);
 	  public void execute(); 
 	  public void execute(String Command);
	  public boolean run(String Command);

	  public void reset();
	  

	  public void table(List<List<String>> table);

	  public void beginTable();
	

	  public void endTable(); 

 	  public boolean success();
 	  public String rawResult();
 	  public String command(); 	  
 	  public List<List<String>> resultSheet();

 	  public PropertiesInterface Properties();
    public void set(String columnName, String value);
    public String get(String columnName);
    
  boolean containsKey(String columnName);

  Set<String> getUsedColumnNames();

}
