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

import model.resources.WithResource;

public class ItemsGrouping {
	
	//TODO: When Within search is separated from external resources, 
	//support all types of resources. Generic, for all entries of WithResourceType enum.
	private List<WithResource<?, ?>> culturalCHO;
	private List<WithResource<?, ?>> recordResource;

	public ItemsGrouping() {
		super();
		culturalCHO = new ArrayList<>();
		recordResource = new ArrayList<>();
	}

	public List<WithResource<?, ?>> getCulturalCHO() {
		return culturalCHO;
	}

	public void setCulturalCHO(List<WithResource<?, ?>> culturalHO) {
		this.culturalCHO = culturalHO;
	}
	
	public List<WithResource<?, ?>> getRecordResource() {
		return recordResource;
	}

	public void setRecordResource(List<WithResource<?, ?>> recordResource) {
		this.recordResource = recordResource;
	}

	public void addAll(ItemsGrouping items) {
		culturalCHO.addAll(items.getCulturalCHO());
		recordResource.addAll(items.getRecordResource());
	}

	public int getItemsCount() {
		return culturalCHO.size()+recordResource.size();
	}

}
