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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationResult {
	
	public AnnotatedObject ano;
	public AnnotationResultDescriptor ard;
	
	public ArrayList<AnnotationResult> next;
	
	public AnnotationResult(AnnotatedObject ano, AnnotationResultDescriptor ard) {
		this.ano = ano;
		this.ard = ard;
	}
	
	public ArrayList<Map<String, Object>> print(AnnotationIndex ai) {
		ArrayList<Map<String, Object>> res = new ArrayList<>();
		
		Map<String, Object> map = new HashMap<>();
		print(ai, map, res);
		
		return res;
	}
	
	public void print(AnnotationIndex ai, Map<String, Object> map, ArrayList<Map<String, Object>> res) {
		map.putAll(getResult(ai));
		if (next != null) {
			for (AnnotationResult n : next) {
				Map<String, Object> newMap =(Map<String, Object>)((HashMap)map).clone();
				n.print(ai, newMap, res);
			}
		} else {
			res.add(map);
		}
				
//		return ret;
	}
	
	public void add(AnnotationResult n) {
		if (next == null) {
			next = new ArrayList<>();
		}
		next.add(n);
	}
	
	private Map<String, Object> getResult(AnnotationIndex ai) {
		Map<String, Object> ret = new HashMap<>();
		
		Span span = ano.getSpan();
		String name = ard.getName();
		ArrayList<String> classes = ard.getClasses();
		
		if (classes == null) {
			ret.put(name, ai.getText().substring(span.start, span.end));
		} else {
			for (String cl : classes) {
				Set<String> sann = new HashSet<>();
				for (AnnotationValue av : ano.get(cl)) {
					sann.add(av.toString());
				}
				ret.put(name + "." + cl, sann);
			}
		}

		if (ard.getID()) {
			ret.put(name + ".id", ano.getID() + "");
		}
		
		if (ard.getSpan()) {
			ret.put(name + ".start", span.start);
			ret.put(name + ".end", span.end);
		}
		
		return ret;

	}
	
	public String toString() {
		return (ano != null ? ano.toString() : "NULL" ) + " | " + ard.toString();
	}
}
