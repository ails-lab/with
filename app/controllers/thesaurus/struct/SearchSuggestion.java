/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package controllers.thesaurus.struct;

public class SearchSuggestion implements Comparable<SearchSuggestion> {
	public String id;
	public String label;
	public String uri;
	public String vocabulary;
	
	public double distance;
	
	public SearchSuggestion (String reference, String id, String label, String uri, String vocabulary) {
		this.id = id;
		this.label = label;
		this.uri = uri;
		this.vocabulary = vocabulary;
		
		distance = jaccardDistance(2, reference, label);
	}

	@Override
	public int compareTo(SearchSuggestion arg0) {
		if (this.distance < arg0.distance) {
			return -1;
		} else if (this.distance > arg0.distance) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static void main(String[] args) {
		System.out.println(jaccardDistance(2, "dress", "dresses"));
		System.out.println(jaccardDistance(2, "dress", "dress"));
	}
	
	 public static double jaccardDistance(int n, String s, String t) {
		 if (s == null || t == null) {
			 return 1;
		 }
		 
		 int l1 = s.length() - n + 1;
		 int l2 = t.length() - n + 1;
		 
		 int found = 0;
		 for (int i = 0; i < l1 ; i++  ){
			 for (int j = 0; j < l2; j++) {
				 int k = 0;
				 for( ; ( k < n ) && ( s.charAt(i+k) == t.charAt(j+k) ); k++);
				 if (k == n) {
					 found++;
				 }
			 }
		 }

		 double dist = 1-(2*((double)found)/((double)(l1+l2)));
		 if (!s.equals(t)) {
			 dist += 0.1;
		 }
		 
		 return dist;
	}
}
