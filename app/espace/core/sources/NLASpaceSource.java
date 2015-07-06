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


package espace.core.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;

import utils.ListUtils;
import utils.Serializer;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.AdditionalQueryModifier;
import espace.core.CommonFilterLogic;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.QueryModifier;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;
import espace.core.Utils.Pair;



public class NLASpaceSource extends ISpaceSource {


	
	private String Key = "SECRET_KEY";

	public NLASpaceSource() {
		super();
		addDefaultQueryModifier(CommonFilters.TYPE_ID, qfwriter("l-format"));
		addDefaultQueryModifier(CommonFilters.YEAR_ID,qfwriterYEAR());
		
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, 
				"Image","Photograph", "Poster, chart, other");
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "Video");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, 
				"Sound","Sheet music");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "Books","Article");
	}
	
	private Function<List<String>, QueryModifier> qfwriterYEAR() {
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				String a = t.get(0), b = a;
				
				if (t.size()>1){
					b = t.get(1);
				}
				return new AdditionalQueryModifier("%20date:%5B"+a+"%20TO%20"+b+"%5D");
			}
		};
	}
	
	private Function<List<String>, QueryModifier> qfwriter(String parameter) {
		Function<String, String> function = (String s)->{return Utils.spacesFormatQuery(s, "%20");};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(parameter+ ":%5B" 
						+Utils.getORList(ListUtils.transform(t, 
								function),false)+"%5D");
			}
		};
	}
	
	
private Function<List<String>, Pair<String>> fwriter(String parameter) {
		
		Function<String, String> function = (String s)->{return Utils.spacesFormatQuery(s, "%20");};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter,  
						Utils.getORList(ListUtils.transform(t, 
								function),false));
			}
		};
	}
	public String getHttpQuery(CommonQuery q) {
//		String spacesFormatQuery = Utils.spacesFormatQuery(q.searchTerm, "%20");
//		spacesFormatQuery = addfilters(q, spacesFormatQuery);
		QueryBuilder builder = new QueryBuilder("http://api.trove.nla.gov.au/result");
		builder.addSearchParam("key", Key);
		builder.addSearchParam("zone", "picture,book,music,article");
		builder.addQuery("q", q.searchTerm);
		// TODO term to exclude?
		builder.addSearchParam("n", q.pageSize);
		builder.addSearchParam("s", ""+((Integer.parseInt(q.page) - 1) * Integer
						.parseInt(q.pageSize)));
		builder.addSearchParam("encoding", "json");
		builder.addSearchParam("reclevel", "full");
		builder.addSearchParam("facet", "format,year");
		return addfilters(q, builder).getHttp();
	}

	public String getSourceName() {
		return "NLA";
	}

	public String getDPLAKey() {
		return Key;
	}

	public void setDPLAKey(String dPLAKey) {
		Key = dPLAKey;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = CommonFilterLogic.typeFilter();
		CommonFilterLogic year = CommonFilterLogic.yearFilter();

		if (checkFilters(q)){
		try {
			response = HttpConnector.getURLContent(httpQuery);
			// System.out.println(response.toString());
			JsonNode pa = response.path("response").path("zone");
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (int i = 0; i < pa.size(); i++) {
				JsonNode o = pa.get(i);
				if (!o.path("name").asText().equals("people")) {
					System.out.print(o.path("name").asText() + " ");
					res.totalCount += Utils.readIntAttr(o.path("records"),
							"totalCount", true);
					res.count += Utils
							.readIntAttr(o.path("records"), "n", true);
					res.startIndex = Utils.readIntAttr(o.path("records"), "s",
							true);

					JsonNode aa = o.path("records").path("work");

					// System.out.println(aa.size());

					for (JsonNode item : aa) {
						// System.out.println(item.toString());

						List<String> v = Utils.readArrayAttr(item, "type",
								false);
						// type.addValue(vmap.translateToCommon(type.filterID,
						// ));
						// System.out.println("add " + v);
//						for (String string : v) {
//							countValue(type, string);
//						}

						ItemsResponse it = new ItemsResponse();
						it.id = Utils.readAttr(item, "id", true);
						it.thumb = Utils.readArrayAttr(Utils.findNode(item
								.path("identifier"), new Pair<String>("type",
								"url"), new Pair<String>("linktype",
								"thumbnail")), "value", false);
						// TODO not present
						it.fullresolution = null;
						it.title = Utils.readLangAttr(item, "title", false);
						it.description = Utils.readLangAttr(item, "abstract",
								false);
						it.year = Utils.readArrayAttr(item, "issued", true);

						// TODO are they the same?
						it.creator = Utils.readLangAttr(item, "contributor",
								false);
						it.dataProvider = Utils.readLangAttr(item,
								"contributor", false);

						it.url = new MyURL();
						it.url.original = Utils.readArrayAttr(Utils.findNode(
								item.path("identifier"), new Pair<String>(
										"type", "url"), new Pair<String>(
										"linktype",
										"fulltext, restricted, unknown")),
								"value", false);
						it.url.fromSourceAPI = Utils.readAttr(item, "troveUrl",
								false);

						if (it.url.original != null)
							it.externalId = it.url.original.get(0);
						else
							it.externalId=getSourceName() + "_" + it.id;
						it.externalId = DigestUtils.md5Hex(it.externalId);
						it.rights=null;

						a.add(it);
						
						 

						

						
						
						
					}
					JsonNode facet = o.path("facets").path("facet");
					{
//						System.out.println(">>>"+facet.toString());
						for (JsonNode jsonNode : facet.path("term")) {
							String label = jsonNode.path("search").asText();
							int count = jsonNode.path("count").asInt();
							JsonNode path = facet.path("name");
//							System.out.println(" path "+path);
							switch (path.asText()) {
							case "format":
								countValue(type, label, count);
								break;
							case "year":
								countValue(year, label, count);
								break;
							default:
								break;
							}
						}
					}
					
					
				}
			}
		
			res.items = a;
			res.filtersLogic = new ArrayList<>();
			res.filtersLogic.add(type);
			res.filtersLogic.add(year);
			
		
			
			// System.out.println(type);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}

		return res;
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://api.trove.nla.gov.au/work/"
							+ recordId + "?key=" + Key
							+ "&encoding=json&reclevel=full");
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_NLA, record
					.toString()));
			Document xmlResponse = HttpConnector
					.getURLContentAsXML("http://api.trove.nla.gov.au/work/"
							+ recordId + "?key=" + Key
							+ "&encoding=xml&reclevel=full");
			jsonMetadata.add(new RecordJSONMetadata(Format.XML_NLA, Serializer
					.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
