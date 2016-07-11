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
import java.util.Set;

public class AnnotationRegexResult {
	
	public AnnotatedObject ano;
	public ArrayList<String> cats;
	
	public AnnotationRegexResult(AnnotatedObject ano, ArrayList<String> cats) {
		this.ano = ano;
		this.cats = cats;
	}
	
	public Object[] getResult(AnnotationIndex ai) {
		Span span = ano.getSpan();
		if (cats == null) {
			return new Object[] {ai.getText().substring(span.start, span.end)};
		} else {
			Set<AnnotationValue>[] ret = new Set[cats.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = ano.get(cats.get(i));
			}
			return ret;
		}
	}
	
	public String toString() {
		return (ano != null ? ano.toString() : "NULL" ) + " | " + cats.toString();
	}
}
