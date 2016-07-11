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
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.Elastic;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.mvc.QueryStringBindable;
import search.FiltersFields;
import search.Query;
import utils.ListUtils;
import utils.Tuple;

public class CommonQuery implements Cloneable , QueryStringBindable<CommonQuery>{

	public static final ALogger log = Logger.of( CommonQuery.class );
	
	@JsonIgnoreProperties(ignoreUnknown=true)
	public String page = "1";
	public String facetsMode = FacetsModes.DEFAULT;
	public boolean hasMedia=false;
	public String pageSize = "20";
	public String searchTerm;
	public List<String> source;
	public String tail;
	private List<Tuple<ObjectId, Access>> directlyAccessedByUserName = new ArrayList<Tuple<ObjectId, Access>>();
	private List<Tuple<ObjectId, Access>> directlyAccessedByGroupName = new ArrayList<Tuple<ObjectId, Access>>();
	//private List<Tuple<String, String>> recursivelyAccessedByGroupName;
	private List<String> effectiveUserIds;//not set in JSON

	public List<CommonFilter> filters;
	private List<String> types =Elastic.allTypes; //if not specified, default is search in all resource types

	public CommonQuery(String generalQueryBody) {
		this.searchTerm = generalQueryBody;
	}

	public CommonQuery() {
	}

	public CommonQuery(Query query) {
		this.page = ""+query.page;
		this.pageSize = ""+query.pageSize;
		this.searchTerm = query.findFilter(FiltersFields.ANYWHERE.getFilterId()).value;
		this.source = ListUtils.transform(query.sources, (x)->x.toString());  
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
			clone = clone();
			if (clone != null) {
				clone.filters = (List<CommonFilter>) arrayList.clone();
				result.add(clone);
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
	
	@Override
	public CommonQuery clone() {
		try {
			Object clone = super.clone();
			return (CommonQuery) clone;
		} catch (Exception e) {
			log.error("",e);
		}
		return null;
	}

	public void addFilter(CommonFilter commonFilter) {
		if (filters==null)
			filters = new ArrayList<>();
		filters.add(commonFilter);
	}

	@Override
	public Option<CommonQuery> bind(String arg0, Map<String, String[]> arg1) {
		// TODO Auto-generated method stub
		CommonQuery q = new CommonQuery();
		q.searchTerm = arg1.get("searchTerm")[0];
		q.page = arg1.get("page")[0];
		q.pageSize = arg1.get("pageSize")[0];
		String[] srcs = arg1.get("source");
		if(Utils.hasInfo(srcs)){
			q.source = Utils.parseArray(srcs);
		}
		
		q.filters = new ArrayList<>();
		for (String key : arg1.keySet()) {
			if (key.startsWith("filter.")){
				String filterID = key.substring(key.indexOf(".")+1);
				CommonFilter f = new CommonFilter();
				f.filterID = filterID;
				f.values = Utils.parseArray(arg1.get(key));
				q.filters.add(f);
			}
		}
		
		return Option.Some(q);
	}

	@Override
	public String javascriptUnbind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String unbind(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
