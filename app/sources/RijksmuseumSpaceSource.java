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
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import play.libs.Json;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.formatreaders.CulturalRecordFormatter;
import sources.formatreaders.RijksmuseumItemRecordFormatter;
import sources.formatreaders.RijksmuseumRecordFormatter;
import utils.Serializer;

public class RijksmuseumSpaceSource extends ISpaceSource {

	public RijksmuseumSpaceSource() {
		super(Sources.Rijksmuseum);
		apiKey = "SECRET_KEY";
		formatreader = new RijksmuseumRecordFormatter(FilterValuesMap.getRijksMap());
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("https://www.rijksmuseum.nl/api/en/collection");
		builder.addSearchParam("key", apiKey);
		builder.addSearchParam("format", "json");
		if (q.hasMedia)
			builder.addSearchParam("imgonly", "True");
		builder.addSearchParam("f", "1");
		
		builder.addQuery("q", q.searchTerm);
		builder.addSearchParam("p", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize) + 1));

		builder.addSearchParam("ps", "" + q.pageSize);

		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		// CommonFilterLogic type = CommonFilterLogic.typeFilter();
		// CommonFilterLogic provider = CommonFilterLogic.providerFilter();
		// CommonFilterLogic dataprovider =
		// CommonFilterLogic.dataproviderFilter();
		// CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
		// CommonFilterLogic rights = CommonFilterLogic.rightsFilter();
		// CommonFilterLogic country = CommonFilterLogic.countryFilter();
		// CommonFilterLogic year = CommonFilterLogic.yearFilter();
		// CommonFilterLogic contributor =
		// CommonFilterLogic.contributorFilter();
		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "count", true);
				// res.count = q.pageSize;
				for (JsonNode item : response.path("artObjects")) {
					res.addItem(formatreader.readObjectFrom(item));
				}
				//TODO: what is the count?
				res.count = res.items.getItemsCount();

				res.facets = response.path("facets");

				// for (JsonNode facet : response.path("facets")) {
				// for (JsonNode jsonNode : facet.path("fields")) {
				// String label = jsonNode.path("label").asText();
				// int count = jsonNode.path("count").asInt();
				// switch (facet.path("name").asText()) {
				// case "TYPE":
				// countValue(type, label, count);
				// break;
				//
				// case "DATA_PROVIDER":
				// countValue(dataprovider, label, false, count);
				// break;
				//
				// case "PROVIDER":
				// countValue(provider, label, false, count);
				// break;
				//
				// case "RIGHTS":
				// countValue(rights, label, count);
				// break;
				//
				// case "proxy_dc_creator":
				// countValue(creator, label, false, count);
				// break;
				//// case "proxy_dc_contributor":
				//// countValue(contributor, label, false, count);
				//// break;
				// case "COUNTRY":
				// countValue(country, label, false, count);
				// break;
				//
				// case "YEAR":
				// countValue(year, label, false, count);
				// break;
				//
				// default:
				// break;
				// }
				// }
				// }

				res.filtersLogic = new ArrayList<>();
				// res.filtersLogic.add(type);
				// res.filtersLogic.add(provider);
				// res.filtersLogic.add(dataprovider);
				// res.filtersLogic.add(creator);
				//// res.filtersLogic.add(contributor);
				// res.filtersLogic.add(rights);
				// res.filtersLogic.add(country);
				// res.filtersLogic.add(year);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// protected void countValue(CommonFilterResponse type, String t) {
		// type.addValue(vmap.translateToCommon(type.filterID, t));
		// }
		return res;
	}
	
	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector().getURLContent(
					"https://www.rijksmuseum.nl/api/en/collection/" + recordId + "?key=" + apiKey + "&format=json");
			JsonNode record = response;
			if (record != null) {
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_RIJ, record.toString()));
				CulturalRecordFormatter f = new RijksmuseumItemRecordFormatter();
				// TODO make another reader
				CulturalObject res = f .readObjectFrom(record);
				if (fullRecord!=null && Utils.hasInfo(fullRecord.getMedia())){
					EmbeddedMediaObject object = ((HashMap<MediaVersion, EmbeddedMediaObject>)fullRecord.getMedia().get(0)).get(MediaVersion.Thumbnail);
					res.addMedia(MediaVersion.Thumbnail, object);
				}
					
				String json = Json.toJson(res).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}
			Document xmlResponse = getHttpConnector().getURLContentAsXML(
					"https://www.rijksmuseum.nl/api/en/collection/" + recordId + "?key=" + apiKey + "&format=xml");
			jsonMetadata.add(new RecordJSONMetadata(Format.XML_NLA, Serializer.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
