// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class StringListComparator implements Comparator<List<String>> {

	private  List<Integer> _SortKeys1;
	private  List<Integer> _SortKeys2;
	
	public StringListComparator(){
		_SortKeys1 =  new ArrayList<Integer>();
		_SortKeys2 =  new ArrayList<Integer>();
	}
	public StringListComparator(List<Integer> SortKeys){
		_SortKeys1 = SortKeys;
		_SortKeys2 = SortKeys;
	}
	public StringListComparator(List<Integer> SortKeys1, List<Integer> SortKeys2){
		_SortKeys1 = SortKeys1;
		_SortKeys2 = SortKeys2;
	}
	 @Override
	    public int compare(List<String> a1, List<String> a2) {
		 	int r =0;
		 	for (int i=0; i < _SortKeys1.size(); i++){
		 		r = a1.get(Math.abs(_SortKeys1.get(i))-1).compareTo(a2.get(Math.abs(_SortKeys2.get(i))-1));

		 		// sort in descending order if the sortkey is negative
		 		if (Math.signum(_SortKeys1.get(i)) < 0){
		 			r = r*-1;
		 		}

		 		if (r != 0){ 
		 			break;
		 		}
		 	}
	        return r;
	    }
}
