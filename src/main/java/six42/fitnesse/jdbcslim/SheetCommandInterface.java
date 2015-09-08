// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.List;


public interface SheetCommandInterface  {

	  public void setCommand(String Command);
 	  public void execute(); 
 	  public void execute(String Command); 
	  
	  public void reset();
	  

	  public void table(List<List<String>> table);

	  public void beginTable();
	

	  public void endTable(); 

 	  public boolean success();
 	  public String rawResult();
 	  public String command(); 	  
 	  public List<List<String>> resultSheet();

 	  public PropertiesInterface Properties();

}
