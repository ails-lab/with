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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotatedObject implements Comparable<AnnotatedObject>, Serializable {

	private AnnotationIndex ai;
	
	private Span span;
	private Map<String, Set<AnnotationValue>> annotations;
	
	private int id;
	
	public AnnotatedObject(Span span, AnnotationIndex ai) {
		this.span = span;
		this.ai = ai;
		this.id = ai.size();
		
		annotations = new HashMap<>();
	}
	
	public int getID() {
		return id;
	}
	
	public void add(String cat, AnnotationValue value) {
		Set<AnnotationValue> vals = annotations.get(cat);
		if (vals == null) {
			vals = new HashSet<>();
			annotations.put(cat, vals);
		}			
		
		if (value != null) {
			vals.add(value);
		}
	}
	
	public Set<AnnotationValue> get(String cat) {
		return annotations.get(cat);
	}
	
	public Set<Map.Entry<String, Set<AnnotationValue>>> entrySet() {
		return annotations.entrySet();
	}
	
	public Span getSpan() {
		return span;
	}

	@Override
	public int compareTo(AnnotatedObject o) {
		return span.compareTo(o.span);
	}
	
	public String getText() {
		return ai.getText().substring(span.start, span.end);
	}
	
	public String toString() {
		return span + " : " + ai.getText().substring(span.start, span.end) + " " + annotations;
	}
}
