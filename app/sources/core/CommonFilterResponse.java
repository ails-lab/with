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


package sources.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import model.basicDataTypes.ProvenanceInfo.Sources;

public class CommonFilterResponse {
	public String filterName;
	public String filterID;
	public List<Sources> sources;
	public List<ValueCount> suggestedValues;

	public CommonFilterResponse() {
		sources = new ArrayList<>();
	}
	
	public CommonFilterResponse(String filterID, String filterName) {
		super();
		this.filterID = filterID;
		this.filterName = filterName;
	}

	public CommonFilterResponse(String filterID) {
		super();
		this.filterID = filterID;
		this.filterName = filterID;
	}

	@Override
	public String toString() {
		return "CommonFilterResponse [filterName=" + filterName.toString() + ", suggestedValues=" + suggestedValues.size() + "]";
	}
	
	public void addSource(Sources s){
		if (!sources.contains(s)){
			sources.add(s);
		}
	}

}