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


package annotators.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import annotators.ComplexAnnotationValue;
import annotators.SimpleAnnotationValue;

public class AnnotationIndex implements Cloneable, Serializable {

	private String text; 
	
	private Map<String, Map<AnnotationValue, ArrayList<Span>>> tags;
	private ArrayList<AnnotatedObject> spanList;
	
	public AnnotationIndex(String text) {
		this.text = text;
		
		tags = new HashMap<>();
		spanList = new ArrayList<>();
	}
	
	public int size() {
		return spanList.size();
	}
	
	public Object clone() {
		try {
			AnnotationIndex ai = (AnnotationIndex)super.clone();
			
			ai.text = text;
			
			ai.spanList = new ArrayList<>();
			for (AnnotatedObject entry : spanList) {
				ai.spanList.add(new AnnotatedObject(entry.getSpan(), ai));
			}
			
			ai.tags = new HashMap<>();
			
			for (int i = 0; i < spanList.size(); i++) {
				AnnotatedObject copy = ai.spanList.get(i);
				Span l = copy.getSpan();
				
				for (Map.Entry<String, Set<AnnotationValue>> entry2 : spanList.get(i).entrySet()) {
					String key = entry2.getKey();
					
					Map<AnnotationValue, ArrayList<Span>> map = ai.tags.get(key);
					if (map == null) {
						map = new HashMap<>();
						ai.tags.put(key, map);
					}

					for (AnnotationValue val : entry2.getValue()) {
						Object v = val.getValue();
						
						AnnotationValue newav = null;
						
						if (val instanceof SimpleAnnotationValue) {
							if (v instanceof AnnotatedObject) {
								int p = binarySearch(ai.spanList, ((AnnotatedObject)v).getSpan());
								newav = new SimpleAnnotationValue(ai.spanList.get(p));
							} else {
								newav = new SimpleAnnotationValue(v);
							}
						} else if (val instanceof ComplexAnnotationValue) {
							Object[] cv = (Object[])v;
							Object[] newv = new Object[cv.length];
							for (int j = 0; j < cv.length; j++) {
								Object vv = cv[j];
								
								if (vv instanceof AnnotatedObject) {
									int p = binarySearch(ai.spanList, ((AnnotatedObject)vv).getSpan());
									newv[j] = ai.spanList.get(p);
								} else {
									newv[j] = vv;
								}
							}
							newav = new ComplexAnnotationValue(newv);
						}
						
						copy.add(key, newav);
						
						ArrayList<Span> locs = map.get(newav);
						if (locs == null) {
							locs = new ArrayList<>();
							map.put(newav, locs);
						}
						
						int lpos = Collections.binarySearch(locs, l);
						if (lpos < 0) {
							locs.add(-lpos - 1, l);
						}
					}
					
					ArrayList<Span> locs = map.get(null);
					if (locs == null) {
						locs = new ArrayList<>();
						map.put(null, locs);
					}
					
					int lpos = Collections.binarySearch(locs, l);
					if (lpos < 0) {
						locs.add(-lpos - 1, l);
					}

					copy.add(key, null);
				}
			}
			
			return ai;
			
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public void add(String cat, AnnotationValue value, int s, int e) {
		add(cat, value, new Span(s, e));
	}
	
	public void add(String cat, AnnotationValue value, Span l) {
		Map<AnnotationValue, ArrayList<Span>> map = tags.get(cat);
		if (map == null) {
			map = new HashMap<>();
			tags.put(cat, map);
		} 

		AnnotatedObject ano;
		int pos = binarySearch(spanList, l);
		
		if (pos < 0) {
			ano = new AnnotatedObject(l, this);
			spanList.add(-pos - 1, ano);
		} else {
			ano = spanList.get(pos);
		}

		if (value != null) {
			ArrayList<Span> locs = map.get(value);
			if (locs == null) {
				locs = new ArrayList<>();
				map.put(value, locs);
			}
			
			int lpos = Collections.binarySearch(locs, l);
			if (lpos < 0) {
				locs.add(-lpos - 1, l);
			}
	
			ano.add(cat, value);
		}
		
		ArrayList<Span> locs = map.get(null);
		if (locs == null) {
			locs = new ArrayList<>();
			map.put(null, locs);
		}
		
		int lpos = Collections.binarySearch(locs, l);
		if (lpos < 0) {
			locs.add(-lpos - 1, l);
		}

		ano.add(cat, null);
	}
	
	public Map<String, Set<AnnotationValue>> getClassValueMap() {
		Map<String, Set<AnnotationValue>> res = new TreeMap<>();
		
		for (Map.Entry<String, Map<AnnotationValue, ArrayList<Span>>> entry : tags.entrySet()) {
			res.put(entry.getKey(), entry.getValue().keySet());
		}
		
		return res;
	}
	
	public Set<AnnotationValue> getValuesForClass(String cat) {
		Map<AnnotationValue, ArrayList<Span>> r = tags.get(cat);
		if (r != null) {
			return r.keySet();
		} else {
			return null;
		}
	}
	
	public AnnotatedObject getAnnotations(Span loc) {
		int pos = binarySearch(spanList, loc);
		if (pos < 0) {
			return null;
		} else {
			return spanList.get(pos);
		}
	}

	public ArrayList<AnnotatedObject> getAnnotationsInSpan(Span loc) {
		ArrayList<AnnotatedObject> res = new ArrayList<>();
		
		int pos = binarySearchStart(spanList, loc.start);
		if (pos >= 0) {
			for (int i = pos; i < spanList.size(); i++) {
				int start = spanList.get(i).getSpan().start;
				if (start > loc.end) {
					break;
				}
				
				int end = spanList.get(i).getSpan().end;
				if (end <= loc.end) {
					res.add(spanList.get(i));
				}
			}
		}
		
		return res;
	}

	public ArrayList<AnnotatedObject> getAnnotatedObjects() {
		return spanList;
	}

	public ArrayList<Span> getLocations(String cat, AnnotationValue value) {
		Map<AnnotationValue, ArrayList<Span>> map = tags.get(cat);
		if (map != null) {
			return map.get(value);			
		} else {
			return null;
		}
	}

	public String toString() {
		return text;
	}
	
	public String getText() {
		return text;
	}
	
	private ArrayList<ArrayList<AnnotationRegexResult>> mm;
	
	private boolean find(ArrayList<AnnotatedObject> spans, int prevSpanIndex, int spanIndex, ArrayList<AnnotationRegexExpression> select, int regexIndex, ArrayList<AnnotationRegexResult> matched) {
//		System.out.println("FIND " + spanIndex + " " + regexIndex + " " + select.size());
		if (regexIndex == select.size()) {
			mm.add(matched);
			return true;
		} else if (regexIndex == select.size() - 1 && select.get(regexIndex).isEnd()) {
//			System.out.println("-- " + prevSpanIndex + " / " + spans.get(prevSpanIndex).getSpan().end + " " + spans.get(spans.size() - 1).getSpan().end); 
			if (prevSpanIndex > -1 && spans.get(prevSpanIndex).getSpan().end == spans.get(spans.size() - 1).getSpan().end) {
				mm.add(matched);
				return true;
			}
			return false;
		}
		
		while (spanIndex < spans.size() && regexIndex < select.size()) {
//			System.out.println("LOOP " + spanIndex + " " + regexIndex);
			Span cSpan = spans.get(spanIndex).getSpan();
			AnnotationRegexExpression cAnnSel = select.get(regexIndex);

//			System.out.println("\t " + cAnnSel + " " + cSpan);
//			System.out.println("\t " + cAnnSel.getClass().getName());
//			System.out.println("\t " + getAnnotations(cSpan));
//			System.out.println("\t " + cAnnSel.satisfies(getAnnotations(cSpan)));
			if (cAnnSel.satisfies(getAnnotations(cSpan))) {
				ArrayList<AnnotationRegexResult> newMatched = (ArrayList<AnnotationRegexResult>)matched.clone();
				newMatched.add(new AnnotationRegexResult(getAnnotations(cSpan), cAnnSel.getReturnElements()));
				
				int lastStart = cSpan.start;
				int lastEnd = cSpan.end;
				int newSpanIndex = spanIndex + 1;
				
				while (newSpanIndex < spans.size()) {
					Span wSpan = spans.get(newSpanIndex).getSpan();
					if (lastStart == wSpan.start) {
						newSpanIndex++;
					} else {
						break;
					}
				}				

				while (newSpanIndex < spans.size()) {
					Span wSpan = spans.get(newSpanIndex).getSpan();
					if (lastEnd > wSpan.start) {
						newSpanIndex++;
					} else {
						break;
					}
				}				

				boolean f = find(spans, spanIndex, newSpanIndex, select, regexIndex + 1, newMatched);
				spanIndex++;
				
				if (!f) {
					if (spans.get(spanIndex).getSpan().start > spans.get(spanIndex - 1).getSpan().start) {
						return false;
					}
				}
			} else if (select.get(0).isStart()) {
				spanIndex++;
				
				if (spanIndex < spans.size() && spans.get(spanIndex).getSpan().start == 0) {
					
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		
		return true;
	}
	
	public ArrayList<ArrayList<AnnotationRegexResult>> find(ArrayList<AnnotationRegexExpression> select) {
		mm = new ArrayList<ArrayList<AnnotationRegexResult>>();
		
		if (select == null || select.size() == 0) {
			return mm;
		}

		int regexIndex = 0;
		
		if (select.get(regexIndex).isStart()) {
			regexIndex++;
		} 

		find(new ArrayList<>(getAnnotatedObjects()), -1, 0, select, regexIndex, new ArrayList<AnnotationRegexResult>());
		
		return mm;
	}

	
    private static int binarySearch(ArrayList<AnnotatedObject> list, Span key) {
        int low = 0;
        int high = list.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            AnnotatedObject midVal = list.get(mid);
            int cmp = midVal.getSpan().compareTo(key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        
        return -(low + 1);  
    }
    
    private static int binarySearchStart(ArrayList<AnnotatedObject> list, int start) {
        int low = 0;
        int high = list.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            AnnotatedObject midVal = list.get(mid);
            int val = midVal.getSpan().start;

            if (val < start) {
                low = mid + 1;
            } else if (val > start) {
                high = mid - 1;
            } else {
            	
            	while (mid - 1 >= 0) {
            		if (list.get(mid - 1).getSpan().start == start) {
            			mid--;
            		} else {
            			break;
            		}
            	}
            	
                return mid;
            }
        }
        
        return -(low + 1);  
    }
}
