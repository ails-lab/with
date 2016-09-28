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


package annotators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.libs.Json;
import sources.core.ParallelAPICall;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.ThesaurusController;
import controllers.thesaurus.struct.SearchSuggestion;
import db.DB;
import elastic.ElasticCoordinator;
import elastic.ElasticEraser;
import elastic.ElasticSearcher.SearchOptions;

public class Vocabulary {

	public static enum VocabularyType {
		THESAURUS,
		RESOURCE
	}
	
	static {
		loadVocabularies();
	}

	private String name;
	private String label;
	private VocabularyType type;
	private Class<? extends Annotator> annotator;
	private String version;
	
	private static Set<Vocabulary> vocabularies;
	
	public static Set<Vocabulary> getVocabularies() {
		return vocabularies;
	}
	
	public static void loadVocabularies() {
		vocabularies = new HashSet<>();
		
		for (String p : DB.getConf().getString("vocabulary.names").split(",")) {
			String title = DB.getConf().getString("vocabulary." + p + ".title");
			String t = DB.getConf().getString("vocabulary." + p + ".type");
			VocabularyType type = null;
			if (t.equals("thesaurus")) {
				type = VocabularyType.THESAURUS;
			} else {
				type = VocabularyType.RESOURCE;
			}
			String ann = DB.getConf().getString("vocabulary." + p + ".annotator");
			Class<? extends Annotator> annotator = null;
			if (annotator != null) {
				try {
					annotator = (Class<? extends Annotator>)Class.forName("annotators" + ann);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			String version = DB.getConf().getString("vocabulary." + p + ".version");
			if (version == null) {
				version = "";
			}
			
			vocabularies.add(new Vocabulary(p, title, type, annotator, version));
		}
	}
	

	
	public Vocabulary(String name, String label, VocabularyType type, Class<? extends Annotator> annotator, String version) {
		this.name = name;
		this.setType(type);
		this.setLabel(label);
		this.setAnnotator(annotator);
		this.setVersion(version);
	}
	
	public String getName() {
		return name;
	}
	
	public static Vocabulary getVocabulary(String name){
		for (Vocabulary voc : vocabularies) {
			if (voc.name.equals(name)) {
				return voc;
			} 
		}
		
		return null;
	}

	public Class<? extends Annotator> getAnnotator() {
		return annotator;
	}

	public void setAnnotator(Class<? extends Annotator> annotator) {
		this.annotator = annotator;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public VocabularyType getType() {
		return type;
	}

	public void setType(VocabularyType type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	
	public static void main(String[] args) {
		importJSONVocabularyToWITH("fashion");
	}
	
//	public static void main(String[] args) {
//		TermQueryBuilder query = QueryBuilders.termQuery("vocabulary.name", "fashion");
//	
//		SearchOptions so = new SearchOptions(0, 10000);
//		so.isPublic = false;
//		ElasticCoordinator es = new ElasticCoordinator();
//		SearchResponse res = es.queryExcecution(query, so);
//		SearchHits sh = res.getHits();
////		
//		System.out.println(sh.getTotalHits());
////		
//		List<ObjectId> tids = new ArrayList<>();
//		for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
//			SearchHit hit = iter.next();
//			tids.add(new ObjectId(hit.getId()));
//		}
//		
//		Function<List<ObjectId>, Boolean> deleteResources = (List<ObjectId> ids) -> (ElasticEraser.deleteManyTermsFromThesaurus(ids));
//		ParallelAPICall.createPromise(deleteResources, tids);
//
//	}
	
	
	public static int step = 10000;
	
	public static void importJSONVocabularyToWITH(String name) {
		long start = System.currentTimeMillis();
		System.out.println("Adding vocabulary " + name);

		File f = new File(System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path") + File.separator + name + ".txt");

		int count = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String line;

//			List<JsonNode> jsons = new ArrayList<>();
			while ((line = br.readLine()) != null)   {
				ThesaurusController.addThesaurusTerm(Json.parse(line));
//				if (jsons.size() < step) {
//					jsons.add(Json.parse(line));
//				} else {
//					ThesaurusController.addThesaurusTerms(jsons);
//					jsons = new ArrayList<>();
//				}
//				count++;
//				if (count % 1000 == 0) {
//					System.out.println(count);
//				}
			}
//			
//			if (jsons.size() > 0) {
//				ThesaurusController.addThesaurusTerms(jsons);
//			}
			
			System.out.println(System.currentTimeMillis() - start);
			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
			
		System.out.println("Added " + count + " for " + name);
	}
}
