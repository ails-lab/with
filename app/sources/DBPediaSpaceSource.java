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
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.AgentObject;
import model.resources.CulturalObject;
import model.resources.PlaceObject;
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

	protected JsonContextRecordFormatReader<AgentObject> agentformatreader;
	protected JsonContextRecordFormatReader<PlaceObject> placeformatreader;
	
	public DBPediaSpaceSource() {
		super(Sources.DBPedia);
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
		res.source = getSourceName().toString();

		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		
		JsonNode response;
		if (checkFilters(q)) {
			try {
				
				response = getHttpConnector().getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "totalcount", true);
				
				int count = 0;
				for (JsonNode item : response.path("results")) {
					for (JsonNode type : item.path("type")) {
						count++;
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
				res.count = count;

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
		//WHAT IS: recordID? IT SHOULD BY DBPEDIA URI name AFTER http://dbpedia.org/resource/...
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector()
					.getURLContent("http://zenon.image.ece.ntua.gr:8890/data/" + recordId + ".rdf");
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_RDF, response.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}
}
