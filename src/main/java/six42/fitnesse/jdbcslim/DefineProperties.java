// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42

package six42.fitnesse.jdbcslim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
 * 
 * @author six42
 * Copyright six42 2015 - contact me at github.com/six42
 * A Collection which stores Properties (Key, Values) to be used in fixture code
 * 
 */
public class DefineProperties {

	final static private Map<String, List<String[]>> theDefinitions = new HashMap<String, List<String[]>>();
	
	private String key;
	private String value;
	private String definitionName;
	private List<String[]> oneDefinition;
	
	public DefineProperties(String definitionName) {
		this.definitionName=definitionName;
	}


	public void setKey(String key) {
		this.key = key;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public String echo(){
		return key + "=" + value;
	}
	public void execute() {
		oneDefinition.add(new String[]{key, value});
	  }

	  public void reset() {
	  }
	  
	  public void beginTable() {
		  oneDefinition = new ArrayList<String[]>();
	  }

	  public void endTable() {
			  theDefinitions.put(definitionName, oneDefinition);
	  }

	  /**
	   *  This is only for testing/debugging
	   * @param definitionName 
	   *   Sets the current definition
	   */
		public void setDefinition(String definitionName) {
			this.definitionName = definitionName;
		}

		  /**
		   *  This is only intended for testing/debugging
		   * @return the current definition as a string
		   */
		public String show(String definitionName){
			  if (theDefinitions.containsKey(definitionName)){
				  StringBuilder sb = new StringBuilder();
				  sb.append('{');
				  for(String[] key_value : theDefinitions.get(definitionName)){
					  sb.append(key_value[0]).append('=').append(key_value[1]).append(", ");
				  }
				  sb.append('}');
				  
				  return sb.toString();
			  }else{
				  return "ERROR:No Definitions exists under this name.";
			  }
			  
		}
		
		
		public  List<String[]> getDefinition(String definitionName){
			  if (theDefinitions.containsKey(definitionName)){
				  return theDefinitions.get(definitionName);
			  }else{
				  return null;
			  }
		}
		public  boolean removeDefinition(String definitionName){
			  if (theDefinitions.containsKey(definitionName)){
				  theDefinitions.remove(definitionName);
				  return true;
			  }else{
				  return false;
			  }
		}
}
