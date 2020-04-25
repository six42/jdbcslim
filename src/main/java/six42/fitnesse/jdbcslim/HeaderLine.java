// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderLine {
	static private final Pattern reSort = Pattern.compile("(<|>)(\\d*)([sdf]?)"); 


	static public boolean isOutputColumn(String columnHeader){
		return columnHeader.contains("?");
	}
	static public boolean isCommentColumn(String columnHeader){
		return columnHeader.startsWith("#") || columnHeader.isEmpty();
	}

  public static boolean isInputColumn(String rawColumnName) {
    return (!isOutputColumn(rawColumnName) && !isCommentColumn(rawColumnName));
  }
	
	static public String plainColumnName(String columnHeader){
		// Remove the question mark from parameter columns and
		// the sort symbol 
		return reSort.matcher(columnHeader.replace("?", "").replace("!", "")).replaceFirst("");
	}

  public static boolean isHeaderNameEqual(String plainHeaderName1, String plainHeaderName2) {
    //By default header names are not case sensitive
    return (plainHeaderName1.equalsIgnoreCase(plainHeaderName2));
  }

	static public int sortIndex(String columnHeader){
		Matcher m = reSort.matcher(columnHeader);
		if(m.find()){
			String ascDesc = m.group(1);
			Integer no;
			
			if (m.group(2) != null && !m.group(2).equals("")){
				//System.out.println("SortIndex: m.group(2)");
					no = Integer.parseInt(m.group(2));
			}else {no = 1;}
			
			//Sort in Descending Order?
			if(ascDesc.equals(">")){ no=no*-1;}

			return no;
		}
		return 0;
	}

	 static public String sortType(String columnHeader){
	    Matcher m = reSort.matcher(columnHeader);
	    if(m.find()){
	      return  m.group(3);
	    }
	    return "s";
	  }

	static public List<HeaderCell> generateSortKeyList(List<String> Header){
		List<HeaderCell> result = new ArrayList<HeaderCell>();
		int no;
		
		for (int p=0; p < Header.size(); p++){
			no = sortIndex(Header.get(p));
			if (no != 0){
				// TODO use the value of no to define the sort order
				// SortKeyList Index values start from 1 not 0 so add 1
			  String sortType = sortType(Header.get(p));
				result.add(new HeaderCell(sortType, p+1, no >0 ? 1 : -1));
			}
		}
		return result;
		
	}

	
	static public List<HeaderCell> generateActualSortKeyList(List<HeaderCell> expectedSortKeyList, int[] headerMap, int sizeExpected){
		List<HeaderCell> result = new ArrayList<HeaderCell>();
			for(int i=0; i< expectedSortKeyList.size(); i++){
				HeaderCell expectedIndex = expectedSortKeyList.get(i);
				Integer actualIndex = headerMap[expectedIndex.getSortIndex()-1];
				if (actualIndex != -1){
					result.add(new HeaderCell(expectedIndex.getSortType(),actualIndex+1,  expectedIndex.getSortDirection()));
				}else{
					// The actual result has no matching column
					// this makes it impossible to compare results
					// throw an exception
					throw new RuntimeException("The actual result has no column with the same name as expected column: " + (expectedIndex.getSortIndex()-1));
				}
			}
		return result;
	}
	
	static public boolean hasHeaderParameters(List<String> Header){
		for (int p=0; p < Header.size(); p++){
			if (isInputColumn(Header.get(p))) return true;
		}
		return false;
	}

	static public List<String> formatHeader(List<String> originalExpectedHeader, List<String> resultHeader) {
    return formatHeader(originalExpectedHeader, resultHeader,
        new HashSet<String>(), "", null);

	}

	    /***
   * Checks that each parameter given in the header exists in the command, flag
   * them as fail if not Marks all Header columns which are new in the actual
   * and don't exist in the expected Mark Input Columns green if they have been
   * used
   * 
   * TODO check also that the order of the Header columns is matching the output
   * 
   * @param originalExpectedHeader
   * @param resultHeader
   * @param usedHeaderColumns
   * @param rawCommand
   * @param properties
   * @return
   */
  public static List<String> formatHeader(List<String> originalExpectedHeader,
      List<String> resultHeader, Set<String> usedHeaderColumns,
      String rawCommand, PropertiesInterface properties) {

	  
		List<String> result = new ArrayList<String>();
		boolean flagUnused = true;
		boolean flagExtra = true;
		
		if(properties != null){
		  flagUnused = properties.getBooleanPropertyOrDefault(ConfigurationParameters.outputFlagUnusedInputColumns, flagUnused);
		  flagExtra = properties.getBooleanPropertyOrDefault(ConfigurationParameters.outputFlagExtraOutputColumns, flagExtra);
		}
		for (int p=0; p < resultHeader.size(); p++){
			String Cell;
			if (originalExpectedHeader.size() <= p){
        // New Header Columns added during the execution mark them as red
			  Cell = flagExtra ? "fail:" :  "report:";
				Cell = Cell + resultHeader.get(p);
			}
			else if (isOutputColumn(resultHeader.get(p)) || isCommentColumn(resultHeader.get(p))|| rawCommand.isEmpty()){
				Cell = "report:" + resultHeader.get(p);
      } else if (usedHeaderColumns.contains(HeaderLine.plainColumnName(
              resultHeader.get(p)).toLowerCase())) {
        Cell = "pass:" + resultHeader.get(p);
			}else{
        Cell = flagUnused ? "fail:" : "ignore:";
        Cell = Cell + resultHeader.get(p);
			}
			result.add( Cell);
		}
		return result;
	}

	/**
	 * 
	 * @param expected  List with expected Header Names
	 * @param actual	List with actual Header Names
	 * @return returns an Array of size expected + actual
	 * 			first for each expected column the index to the actual column is given
	 * 			second from index expected.size() onwards for each actual column the index to 
	 * 			the expected column is given
	 * 			-1 is given as index if no matching column exists in the other table 
	 */
	static public int[] mapActualHeaderToExpectedHeader(List<String> expected, List<String> actual){
		int[] result = new int[actual.size()+expected.size()];
		// Mark all elements as missing with -1
		for (int i=0; i< result.length; i++){ result[i]=-1; }
		
	    int expectedSize = expected.size();
    	String expectedColumnName;
    	String actualColumnName;
    	
    	for (int e=0; e < expected.size(); e++){
    		expectedColumnName = plainColumnName(expected.get(e));
		    for (int a=0; a < actual.size(); a++){
		    	actualColumnName = actual.get(a);
	    		if (isHeaderNameEqual(actualColumnName, expectedColumnName) ){
	    			result[e]= a;
	    			result[expectedSize+a]=e;
	    			break;
	    		}
	    	}
	    	// No expected Column is matching the actual
	    	// Keep the default value -1 to flag this 
	    }
		return result;
	}
}
