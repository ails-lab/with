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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import utils.Tuple;

public class CommonQuery {

	@JsonIgnoreProperties(ignoreUnknown=true)
	public String page = "1";
	public String facetsMode = FacetsModes.DEFAULT;
	public String pageSize = "20";
	public String searchTerm;
	public List<String> source;
	private List<Tuple<ObjectId, Access>> directlyAccessedByUserName = new ArrayList<Tuple<ObjectId, Access>>();
	private List<Tuple<ObjectId, Access>> directlyAccessedByGroupName = new ArrayList<Tuple<ObjectId, Access>>();
	//private List<Tuple<String, String>> recursivelyAccessedByGroupName;
	private List<String> effectiveUserIds;//not set in JSON

	public List<CommonFilter> filters;
	private List<String> types;

	public CommonQuery(String generalQueryBody) {
		this.searchTerm = generalQueryBody;
	}

	public CommonQuery() {
	}

	private List<Tuple<ObjectId, Access>> transformList(ObjectNode[] inputList, boolean group) {
		List<Tuple<ObjectId, Access>> list = new ArrayList<Tuple<ObjectId, Access>>();
		for (int i=0; i<inputList.length; i++) {
			ObjectNode node = inputList[i];
			if (node.size() == 2) {
				Iterator<String> iterator = node.fieldNames();
				String x = node.get(iterator.next()).asText();
				Access access = Access.valueOf(node.get(iterator.next()).asText().toUpperCase());
				ObjectId userOrGroupId;
				if (!group) {
					User user = DB.getUserDAO().getByUsername(x);
					if (user != null) {
						userOrGroupId = user.getDbId();
						list.add(new Tuple<ObjectId, Access>(userOrGroupId, access));
					}
				}
				else {
					UserGroup user = DB.getUserGroupDAO().getByName(x);
					if (user != null) {
						userOrGroupId = user.getDbId();
						list.add(new Tuple<ObjectId, Access>(userOrGroupId, access));
					}
				}
	        }
		}
		return list;
	}

	public List<Tuple<ObjectId, Access>> getDirectlyAccessedByUserName() {
		return directlyAccessedByUserName;
	}

	public void setDirectlyAccessedByUserName(ObjectNode[]  list) {
		directlyAccessedByUserName = transformList(list, false);
	}

	public List<Tuple<ObjectId, Access>> getDirectlyAccessedByGroupName() {
		return directlyAccessedByGroupName;
	}

	public void setDirectlyAccessedByGroupName(ObjectNode[]  list) {
		directlyAccessedByGroupName = transformList(list, true);
	}

	public String getQuery() {
		return searchTerm;
	}

	public void setQuery(String query) {
		this.searchTerm = query;
	}

	public List<CommonQuery> splitFilters(ISpaceSource src) {
		if ((filters == null) || (filters.size() == 0))
			return Arrays.asList(this);
		else
			return splitFilters(0, new ArrayList<>(), new ArrayList<CommonQuery>(), src);
	}

	private List<CommonQuery> splitFilters(int i, ArrayList<CommonFilter> arrayList, ArrayList<CommonQuery> result,
			ISpaceSource src) {
		if (i == filters.size()) {
			CommonQuery clone;
			try {

				clone = (CommonQuery) clone();
				clone.filters = (List<CommonFilter>) arrayList.clone();
				result.add(clone);

			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (i < filters.size()) {
			for (CommonFilter f : filters.get(i).splitValues(src)) {
				arrayList.add(f);
				splitFilters(i + 1, arrayList, result, src);
				arrayList.remove(f);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "CommonQuery [searchTerm=" + searchTerm + ", page=" + page + ", pageSize=" + pageSize + ", source="
				+ source + ", filters=" + filters + "]";
	}

	public void validate() {
		if (!Utils.hasAny(page)) {
			page = "1";
		}
		if (!Utils.hasAny(pageSize)) {
			pageSize = "20";
		}
	}

	public void setEffectiveUserIds(List<String> effectiveUserIds) {
		this.effectiveUserIds = effectiveUserIds;
	}

	public List<String> getEffectiveUserIds() {
		return effectiveUserIds;
	}

	public List<String> getTypes() {
		return types;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public void addType(String type) {
		if(types != null)
			types.add(type);
		else {
			types = new ArrayList<String>();
			types.add(type);
		}

	}
}
