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


package search;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.F.Promise;
import search.Response.SingleResponse;
import sources.core.AdditionalQueryModifier;
import sources.core.FacetsModes;
import sources.core.QueryBuilder;
import sources.core.QueryModifier;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.EuropeanaItemRecordFormatter;
import sources.formatreaders.EuropeanaRecordFormatter;
import sources.utils.FunctionsUtils;
import utils.ListUtils;

public class EuropeanaExternalSource extends ExternalSource {

	
	public EuropeanaExternalSource() {
		super(Sources.Europeana);

		addDefaultWriter(FiltersFields.MIME_TYPE.getFilterId(), qfwriter("MIME_TYPE"));
		addDefaultWriter(FiltersFields.IMAGE_SIZE.getFilterId(), qfwriter("IMAGE_SIZE"));
		addDefaultWriter(FiltersFields.IMAGE_COLOUR.getFilterId(), qfwriter("IMAGE_COLOUR"));
		addDefaultWriter(FiltersFields.COLOURPALETE.getFilterId(), qfwriter("COLOURPALETE"));
		
		addDefaultQueryModifier(FiltersFields.PROVIDER.getFilterId(), qwriter("PROVIDER"));
		addDefaultQueryModifier(FiltersFields.DATA_PROVIDER.getFilterId(), qwriter("DATA_PROVIDER"));
		addDefaultQueryModifier(FiltersFields.COUNTRY.getFilterId(), qwriter("COUNTRY"));

		addDefaultQueryModifier(FiltersFields.YEAR.getFilterId(), qDatewriter());

		addDefaultQueryModifier(FiltersFields.CREATOR.getFilterId(), qwriter("CREATOR"));

		addDefaultQueryModifier(FiltersFields.RIGHTS.getFilterId(), qrightwriter());

		addDefaultQueryModifier(FiltersFields.TYPE.getFilterId(), qwriter("TYPE"));
		
	}
	
	
	
	private Function<List<String>, QueryModifier> qwriter(String parameter) {
		Function<String, String> function = (String s) -> {
			return parameter+":" + FunctionsUtils.smartquote().apply(s) + "";
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(" " + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
		
		
	}
	
	private Function<List<String>, QueryModifier> qrightwriter() {
		Function<String, String> function = (String s) -> {
			s = s.replace("(?!.*nc)", "* NOT *nc");
			s = s.replace("(?!.*nd)", "* NOT *nd");
			return "RIGHTS:(" + s.replace(".", "") + ")";
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(" " + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}

	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		return FunctionsUtils.toORList("qf", 
				(s)-> parameter + ":" + FunctionsUtils.smartquote().apply(s)
				);
	}
	
	private Function<List<String>, QueryModifier> qDatewriter() {
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				String val = dateRange(t);
				return new AdditionalQueryModifier(" YEAR:" + val);
			}
		};
	}
	
	private String dateRange(List<String> t) {
		String val = "\"" + t.get(0) + "\"";
		if (t.size() > 1) {
			val = "[" + val + " TO \"" + t.get(1) + "\"]";
		}
		return val;
	}
	
	@Override
	public boolean isError(JsonNode response) {
		return super.isError(response) || !response.path("success").asBoolean();
	}
	
	@Override
	public void parseResponse(JsonNode response, SingleResponse resultResponse) throws Exception {
		if (isError(response)){
			parseError(response,resultResponse);
		} else {
			resultResponse.source = thisSource();
			resultResponse.totalCount = Utils.readIntAttr(response, "totalResults", true);
			resultResponse.count = Utils.readIntAttr(response, "itemsCount", true);
			readRecords(response.path("items"), resultResponse, new EuropeanaRecordFormatter());
//			res.filtersLogic = createFilters(response);
//					if (usingCursor) {
//						nextCursor = Utils.readAttr(response, "nextCursor", true);
//						if (!Utils.hasInfo(nextCursor))
//							Logger.error("cursor error!!");
//					}
			
		}

	}

	
	@Override
	public QueryBuilder getBuilder(Query q) {
		QueryBuilder builder = new EuropeanaQueryBuilder("http://europeana.eu/api/v2/search.json");
		builder.addSearchParam("wskey", "ANnuDzRpW");

		builder.setQuery("query", q.findFilter(Fields.anywhere.fieldId()).value);

//		if (usingCursor){
//			if (q.page.equals("1"))
//				builder.addSearchParam("cursor", "*");
//			else
//				builder.addSearchParam("cursor", nextCursor);
//		} else
		builder.addSearchParam("start", "" + 
		(((q.getPage() - 1) * q.getPageSize()) + 1));

		builder.addSearchParam("rows", "" + q.getPageSize());
		builder.addSearchParam("profile", "rich facets");
		String facets = "DEFAULT";
//		if (q.facetsMode != null) {
//			switch (q.facetsMode) {
//			case FacetsModes.SOME:
//				facets = "proxy_dc_creator," + facets;
//				break;
//			case FacetsModes.ALL:
//				facets = "proxy_dc_creator,proxy_dc_contributor," + 
//			             "MIME_TYPE,IMAGE_SIZE,IMAGE_COLOUR,IMAGE_GREYSCALE,"+
//						facets;
//				break;
//			default:
//				break;
//			}
//		}
//		if (q)
			builder.addSearchParam("media", "true");
//		builder.addSearchParam("facet", facets);
//		builder.setTail(q.tail);
		return addfilters(q, builder);
	}
	

	@Override
	public Promise<Object> completeRecord(Object incompleteRecord) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<Object> getById(String id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	class EuropeanaQueryBuilder extends QueryBuilder {

		public EuropeanaQueryBuilder() {
			super();
		}

		public EuropeanaQueryBuilder(String baseUrl) {
			super(baseUrl);
		}

		public String getHttp() {
			String res = getBaseUrl();
			Iterator<Pair<String>> it = parameters.iterator();
			boolean skipqf = false;
			boolean added = false;
			if (query.second != null) {
				res += ("?" + query.getHttp());
				added = true;
			} else {
				skipqf = true;
			}
			for (; it.hasNext();) {

				Pair<String> next = it.next();
				String string = added ? "&" : "?";
				if (next.first.equals("qf") && skipqf) {
					skipqf = false;
					query.second = next.second;
					res += (string + query.getHttp());
					added = true;
				} else {
					added = true;
					res += string + next.getHttp();
				}
			}
			if (Utils.hasInfo(tail)){
				res+=tail;
			}
			return res;
		}

	}

}
