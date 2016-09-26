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

import model.basicDataTypes.MultiLiteral;

public class SortClass implements Comparable<SortClass> {
	public String uri;
	public MultiLiteral label;
	public int count;
	
	public SortClass(String uri, MultiLiteral label, int count) {
		this.uri = uri;
		this.label = label;
		this.count = count;
	}

	@Override
	public int compareTo(SortClass so) {
		if (count < so.count) {
			return 1;
		} else if (count > so.count) {
			return -1;
		} else {
			return 0;
		}
	}
}
