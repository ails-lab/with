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


package sources;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.RecordResource;
import model.resources.WithResource;
import play.libs.Json;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.formatreaders.DDBItemRecordFormatter;
import sources.formatreaders.DDBRecordFormatter;

public class DDBSpaceSource extends ISpaceSource {

	public DDBSpaceSource() {
		super();
		LABEL = Sources.DDB.toString();
		apiKey = "SECRET_KEY";
		vmap = FilterValuesMap.getDDBMap();
		formatreader = new DDBRecordFormatter();
	}

	@Override
	public QueryBuilder getBuilder(CommonQuery q) {
		QueryBuilder builder = super.getBuilder(q);
		builder.setBaseUrl("http://api.deutsche-digitale-bibliothek.de/search?");
		builder.addSearchParam("oauth_consumer_key", apiKey);
		builder.addQuery("query", q.searchTerm);
		builder.addSearchParam("rows", q.pageSize);
		builder.addSearchParam("offset", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)));
		builder.addSearchParam("facet", "place_fct");
		builder.addSearchParam("facet", "type_fct");
		builder.addSearchParam("facet", "provider_fct");
		builder.addSearchParam("facet", "time_fct");
		builder.addSearchParam("facet", "license_group");

		FashionSearch fq = new FashionSearch();
		fq.setTerm(q.searchTerm);
		fq.setCount(Integer.parseInt(q.pageSize));
		fq.setOffset(((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)));
		builder.setData(fq);
		addfilters(q, builder);
		return builder;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		// String httpQuery = getHttpQuery(q);
		QueryBuilder builder = getBuilder(q);
		res.query = builder.getHttp();
		JsonNode response;
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(res.query);
				System.out.println("-------------------------------------------------------------");
				System.out.println(response.toString());
				System.out.println("-------------------------------------------------------------");
				JsonNode docs = response.path("results").get(0).path("docs");
				res.totalCount = Utils.readIntAttr(response, "numberOfResults", true);
				res.count = docs.size();
				// res.startIndex = Utils.readIntAttr(response, "offset", true);
				ArrayList<WithResource<?, ?>> a = new ArrayList<>();

				for (JsonNode item : docs) {
					res.addItem(formatreader.readObjectFrom(item));
				}

				// CommonFilterLogic dataProvider =
				// CommonFilterLogic.dataproviderFilter();

				// JsonNode o = response.path("facets");
				// readList(o.path("dataProviders"), dataProvider);

				res.filtersLogic = new ArrayList<>();
				// res.filtersLogic.add(dataProvider);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return res;
	}
	
	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			
			
			QueryBuilder builder = new QueryBuilder();
			builder.setBaseUrl("http://api.deutsche-digitale-bibliothek.de/items/"+recordId+"/edm");
			builder.addSearchParam("oauth_consumer_key", apiKey);
			response = HttpConnector
					.getURLContent(builder.getHttp());
			// todo read the other format;
			JsonNode record = response;
			if (response != null) {
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record.toString()));
				DDBItemRecordFormatter f = new DDBItemRecordFormatter();
				String json = Json.toJson(f.readObjectFrom(record)).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}

			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
