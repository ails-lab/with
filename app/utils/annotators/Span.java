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


package utils.annotators;

import java.io.Serializable;
import java.util.Comparator;

public class Span implements Comparable<Span>, Cloneable, Serializable {
	public int start;
	public int end;
	
	public Span(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public Object clone() {
		try {
			Span s = (Span)super.clone();
			s.start = start;
			s.end = end;
			
			return s;
			
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int compareTo(Span arg0) {
		if (start < arg0.start) {
			return -1;
		} else if (start > arg0.start) {
			return 1;
		} else {
			if (end < arg0.end) {
				return -1;
			} else if (end > arg0.end) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	public int hashCode() {
		return start + end;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Span)) {
			return false;
		}
		
		return start == ((Span)obj).start && end == ((Span)obj).end;
	}
	
	public String toString() {
		return start + "-" + end;
	}
	
	public static class StartSpanComparator implements Comparator<Span> {
		
		public int compare(Span arg0, Span arg1) {
			if (arg0.start < arg1.start) {
				return -1;
			} else if (arg0.start > arg1.start) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	public static class EndSpanComparator implements Comparator<Span> {
		
		public int compare(Span arg0, Span arg1) {
			if (arg0.end < arg1.end) {
				return -1;
			} else if (arg0.end > arg1.end) {
				return 1;
			} else {
				return 0;
			}
		}
	}


	public static class LengthComparator implements Comparator<Span> {

		@Override
		public int compare(Span arg0, Span arg1) {
			int l0 = arg0.end - arg0.start;
			int l1 = arg1.end - arg1.start;
			
			if (l0 < l1) {
				return -1;
			} else if (l0 > l1) {
				return 1;
			} else {
				return 0;
			}
		}
	}

}