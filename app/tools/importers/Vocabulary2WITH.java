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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import tools.importers.vocabulary.VocabularyImportConfiguration;
import elastic.ElasticCoordinator;
import elastic.ElasticEraser;
import elastic.ElasticSearcher.SearchOptions;

public class Vocabulary2WITH {

	public static void main(String[] args) throws FileNotFoundException, IOException {
//		importJSONVocabularyToWITH("fashion");
//		importJSONVocabularyToWITH("photo");
//		importJSONVocabularyToWITH("euscreenxl");
//		importJSONVocabularyToWITH("partageplus");
//		importJSONVocabularyToWITH("mimo");
//		importJSONVocabularyToWITH("gemet");
//		importJSONVocabularyToWITH("hornbostelsachs");
//		importJSONVocabularyToWITH("nerd");
//		importJSONVocabularyToWITH("dbo");
//		importJSONVocabularyToWITH("dbr-place");
//		importJSONVocabularyToWITH("dbr-person");
	}
	
	public static int step = 2000;
	
	public static void importAll() {
		File dir = new File(VocabularyImportConfiguration.outPath);
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

		File f = new File(VocabularyImportConfiguration.outPath + File.separator + name + ".zip");
		uncompress(f);

		int count = 0;

		File uf = new File(VocabularyImportConfiguration.outPath + File.separator + name + ".txt");
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
				
//				if (count == start) {
//					System.out.println("Start at " + starttime);
//				}
				
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
//				System.out.println(line);
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
	
	private final static int BUFFER = 2048;
	
	private static void uncompress(File f) throws FileNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(f);
		     ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {
	    	  
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " + entry);
		        int count;
		        byte data[] = new byte[BUFFER];
		        
		        try (FileOutputStream fos = new FileOutputStream(VocabularyImportConfiguration.outPath + File.separator + entry.getName());
 		             BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER)) {
		        
			        while ((count = zis.read(data, 0, BUFFER)) != -1) {
			        	dest.write(data, 0, count);
			        }
		        
			        dest.flush();
		        }
			}
		}
	}

}
