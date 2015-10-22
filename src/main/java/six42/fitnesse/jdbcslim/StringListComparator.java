// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;


public class StringListComparator implements Comparator<List<String>> {

	private  List<HeaderCell> _SortKeys1;
	private  List<HeaderCell> _SortKeys2;
	
	public StringListComparator(){
		_SortKeys1 =  new ArrayList<HeaderCell>();
		_SortKeys2 =  new ArrayList<HeaderCell>();
	}
	public StringListComparator(List<HeaderCell> SortKeys){
		_SortKeys1 = SortKeys;
		_SortKeys2 = SortKeys;
	}
	public StringListComparator(List<HeaderCell> SortKeys1, List<HeaderCell> SortKeys2){
		_SortKeys1 = SortKeys1;
		_SortKeys2 = SortKeys2;
	}

	@Override
	    public int compare(List<String> a1, List<String> a2) {
		 	int r =0;
		 	for (int i=0; i < _SortKeys1.size(); i++){
		 	  String aStr1 = a1.get(_SortKeys1.get(i).getSortIndex()-1);
        String aStr2 = a2.get(_SortKeys2.get(i).getSortIndex()-1);
        int sortDirection = _SortKeys1.get(i).getSortDirection();
        // treat two null as equal
        if (aStr1 == null && aStr2 == null){
          r=0;
          continue;
        }
        // One side null 
        if(aStr1 == null){
          r=-1*sortDirection;
          break;
        }
        if(aStr2 == null){
          r=1*sortDirection;
          break;
        }
        if ("d".equalsIgnoreCase(_SortKeys1.get(i).getSortType())){
          try{
            Long aL1 = Long.parseLong(aStr1);
            Long aL2 = Long.parseLong(aStr2);
            r= aL1.compareTo(aL2);
          }catch (NumberFormatException e){
            System.out.println("Failed to convert to Long: '" + aStr1 + "' '" + aStr2 + "'");
            e.printStackTrace();
            r= aStr1.compareTo(aStr2);
          }
        }
        else if ("f".equalsIgnoreCase(_SortKeys1.get(i).getSortType())){
          try{
            Double aD1 = Double.parseDouble(aStr1);
            Double aD2 = Double.parseDouble(aStr2);
            r= aD1.compareTo(aD2);
          }catch (NumberFormatException e){
            System.out.println("Failed to convert to Double: '" + aStr1 + "' '" + aStr2 + "'");
            e.printStackTrace();
            r= aStr1.compareTo(aStr2);
          }
        }
        else{
          r= aStr1.compareTo(aStr2);
        }

        r = r*sortDirection;

		 		if (r != 0){ 
		 			break;
		 		}
		 	}
	        return r;
	    }
}
