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


package tools.importers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import controllers.ThesaurusController;
import sources.core.ParallelAPICall;
import tools.importers.vocabulary.AAT2Vocabulary;
import tools.importers.vocabulary.DBPedia2Vocabulary;
import tools.importers.vocabulary.OWL2Vocabulary;
import tools.importers.vocabulary.SKOS2Vocabulary;
import tools.importers.vocabulary.VocabularyImportConfiguration;
import elastic.ElasticCoordinator;
import elastic.ElasticEraser;
import elastic.ElasticSearcher.SearchOptions;

public class Vocabulary2WITH {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		// specify configure folders
		VocabularyImportConfiguration.tmpdir = "C:/Users/achort.IPLAB/git/with-vocabularies/tmp";
		
		VocabularyImportConfiguration.srcdir = "C:/Users/achort.IPLAB/git/with-vocabularies/resources/vocabulary/src";
		VocabularyImportConfiguration.outdir = "C:/Users/achort.IPLAB/git/with-vocabularies/resources/vocabulary/json";
		
		// converts sources in srcdir to zipped jsons in outfir
		//convertSourcesToJSONs();
		
		// import all jsons to WITH
		//importAll();
		
		// or import one-by-one
		//importJSONVocabularyToWITH("fashion");
		//importJSONVocabularyToWITH("photo");
		//importJSONVocabularyToWITH("euscreenxl");
		//importJSONVocabularyToWITH("partageplus");
		//importJSONVocabularyToWITH("mimo");
		//importJSONVocabularyToWITH("gemet");
		//importJSONVocabularyToWITH("hornbostelsachs");
		//importJSONVocabularyToWITH("nerd");
		//importJSONVocabularyToWITH("aat");
		//importJSONVocabularyToWITH("dbo");
		//importJSONVocabularyToWITH("dbr-place");
		//importJSONVocabularyToWITH("dbr-person");
	}
	
	private static void convertSourcesToJSONs() {
		// import skos vocabularies
		SKOS2Vocabulary.doImport(SKOS2Vocabulary.confs);
		
		// import owl vocabularies
		OWL2Vocabulary.doImport(OWL2Vocabulary.confs);
		
		// import aat thesaurus
		AAT2Vocabulary.doImport(AAT2Vocabulary.confs);
		
		// import dbpedia
		List<String[]> filters = new ArrayList<>();
		filters.add(new String[] {"Person"});
		filters.add(new String[] {"Place"});
		
		DBPedia2Vocabulary.doImport(filters);
	}
	
	
	public static int step = 2000;
	
	public static void importAll() {
		File dir = new File(VocabularyImportConfiguration.outdir);
		for (File f : dir.listFiles()) {
			String name = f.getName();
			
			if (name.endsWith(".zip")) {
				try {
					importJSONVocabularyToWITH(name.substring(0, name.lastIndexOf(".")));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void importJSONVocabularyToWITH(String name) throws FileNotFoundException, IOException {
		long starttime = System.currentTimeMillis();
		System.out.println("Adding vocabulary " + name);

		File tmpFolder = VocabularyImportConfiguration.getTempFolder();
		
		File f = new File(VocabularyImportConfiguration.outdir + File.separator + name + ".zip");
		VocabularyImportConfiguration.uncompress(tmpFolder, f);

		int count = 0;

		File uf = new File(tmpFolder + File.separator + name + ".txt");
		try (BufferedReader br = new BufferedReader(new FileReader(uf))) {
			String line;

			long start = 0;
			long end = Long.MAX_VALUE;
			List<JsonNode> jsons = new ArrayList<>(step);
			while ((line = br.readLine()) != null)   {
				if (count < start) {
					count++;
					starttime = System.currentTimeMillis();
					continue;
				}
				
				if (count >= end) {
					break;
				}
				
				if (jsons.size() < step) {
					jsons.add(Json.parse(line));
				} else {
					ThesaurusController.addThesaurusTerms(jsons);
					jsons = new ArrayList<>(step);
					jsons.add(Json.parse(line));
				}
				count++;
				if (count % step == 0) {
					System.out.println("Added " + count);
				}
			}
			
			if (jsons.size() > 0) {
				ThesaurusController.addThesaurusTerms(jsons);
			}
			
			System.out.println("Added in " + (System.currentTimeMillis() - starttime) + " msecs");
			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
			
		uf.delete();
		
		System.out.println("Completed. Added " + count + " for " + name);
	}
	
	public static void deleteVocabularyFromIndex(String voc) {
		TermQueryBuilder query = QueryBuilders.termQuery("vocabulary.name", voc);
	
		SearchOptions so = new SearchOptions(0, Integer.MAX_VALUE);
		so.isPublic = false;
		ElasticCoordinator es = new ElasticCoordinator();
		SearchResponse res = es.queryExcecution(query, so);
		SearchHits sh = res.getHits();
		
		System.out.println(sh.getTotalHits());
		
		List<ObjectId> tids = new ArrayList<>();
		for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
			SearchHit hit = iter.next();
			tids.add(new ObjectId(hit.getId()));
		}
		
		Function<List<ObjectId>, Boolean> deleteResources = (List<ObjectId> ids) -> (ElasticEraser.deleteManyTermsFromThesaurus(ids));
		ParallelAPICall.createPromise(deleteResources, tids);

	}
	


}
