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
import sources.core.JsonContextRecordFormatReader;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.formatreaders.CulturalRecordFormatter;
import sources.formatreaders.DBPediaAgentRecordFormatter;
import sources.formatreaders.DBPediaPlaceRecordFormatter;
import sources.formatreaders.RijksmuseumItemRecordFormatter;
import sources.formatreaders.RijksmuseumRecordFormatter;
import utils.Serializer;

public class DBPediaSpaceSource extends ISpaceSource {

	protected JsonContextRecordFormatReader agentformatreader;
	protected JsonContextRecordFormatReader placeformatreader;
	
	public DBPediaSpaceSource() {
		LABEL = Sources.DBPedia.toString();
		formatreader = new DBPediaAgentRecordFormatter(FilterValuesMap.getDBPediaMap());
		
		agentformatreader = formatreader; 
		placeformatreader = new DBPediaPlaceRecordFormatter(FilterValuesMap.getDBPediaMap());
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("http://zenon.image.ece.ntua.gr/fres/service/dbpedia-with");
		builder.addSearchParam("type", "Person,Place");
		builder.addSearchParam("start", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)));
		builder.addSearchParam("rows", "" + (Integer.parseInt(q.pageSize) - 1));
		builder.addQuery("query", q.searchTerm);

		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();

		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		
		JsonNode response;
		if (checkFilters(q)) {
			try {
				
				response = getHttpConnector().getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "totalCount", true);

				for (JsonNode item : response.path("results")) {
					for (JsonNode type : item.path("type")) {
						if (type.asText().equals("http://dbpedia.org/ontology/Place")) {
							res.addItem(placeformatreader.readObjectFrom(item));
							break;
						} else if (type.asText().equals("http://dbpedia.org/ontology/Person")) {
							res.addItem(agentformatreader.readObjectFrom(item));
							break;
						}
					}
					
				}
				//TODO: what is the count?
				res.count = res.items.getItemsCount();

//				res.facets = response.path("facets");

				res.filtersLogic = new ArrayList<>();
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
//		try {
//			response = HttpConnector.getURLContent(
//					"https://www.rijksmuseum.nl/api/en/collection/" + recordId + "?key=" + apiKey + "&format=json");
//			JsonNode record = response;
//			if (record != null) {
//				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_RIJ, record.toString()));
//				CulturalRecordFormatter f = new RijksmuseumItemRecordFormatter();
//				// TODO make another reader
//				CulturalObject res = f .readObjectFrom(record);
//				if (fullRecord!=null && Utils.hasInfo(fullRecord.getMedia())){
//					EmbeddedMediaObject object = ((HashMap<MediaVersion, EmbeddedMediaObject>)fullRecord.getMedia().get(0)).get(MediaVersion.Thumbnail);
//					res.addMedia(MediaVersion.Thumbnail, object);
//				}
//					
//				String json = Json.toJson(res).toString();
//				System.out.println(json);
//				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
//			}
//			Document xmlResponse = HttpConnector.getURLContentAsXML(
//					"https://www.rijksmuseum.nl/api/en/collection/" + recordId + "?key=" + apiKey + "&format=xml");
//			jsonMetadata.add(new RecordJSONMetadata(Format.XML_NLA, Serializer.serializeXML(xmlResponse)));
			return jsonMetadata;
//		} catch (Exception e) {
//			return jsonMetadata;
//		}
	}

}
