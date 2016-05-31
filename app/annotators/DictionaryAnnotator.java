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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import play.libs.Json;
import model.basicDataTypes.Language;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class DictionaryAnnotator extends Annotator {
	
	protected static Map<Language, DictionaryAnnotator> annotators = new HashMap<>();

	private ExactDictionaryChunker tt;
	
    public static DictionaryAnnotator getAnnotator(Language lang, boolean cs) {
    	DictionaryAnnotator ta = annotators.get(lang);
    	if (ta == null) {
   			ta = new DictionaryAnnotator(lang, cs);
   			annotators.put(lang, ta);
   		} 
    	
    	return ta;
    } 
    
	private DictionaryAnnotator(Language lang, boolean caseSensitive) {
		String langField = "prefLabel." + lang.getDefaultCode();
		String enField = "prefLabel.en";
		String uriField = "uri";
		String thesaurusField = "thesaurus";
		
		ElasticSearcher es = new ElasticSearcher();
		
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("_type", Elastic.thesaurusResource));

		SearchResponse res = es.execute(query, new SearchOptions(0, Integer.MAX_VALUE), new String[] {langField, uriField, thesaurusField, enField} );
		SearchHits sh = res.getHits();

		MapDictionary<String> dict = new MapDictionary<>();

		for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
			SearchHit hit = iter.next();
			
			SearchHitField label = hit.field(langField);
			SearchHitField uri = hit.field(uriField);
			SearchHitField thesaurus = hit.field(thesaurusField);
			SearchHitField enLabel = hit.field(enField);
			
			if (label != null && uri != null) {
				ObjectNode cc = Json.newObject();
				cc.put("uri", uri.getValue().toString());
				cc.put("label", label.getValue().toString());
				cc.put("vocabulary", thesaurus.getValue().toString());

				if (enLabel != null) {
					cc.put("label-en", enLabel.getValue().toString());
				} else {
					cc.put("label-en", label.getValue().toString());
				}
				
				dict.addEntry(new DictionaryEntry<String>(label.getValue().toString(),  Json.stringify(cc)));
			}
		}
			
		tt = new ExactDictionaryChunker(dict, IndoEuropeanTokenizerFactory.INSTANCE, false, caseSensitive);

	}
	
	public static void main(String[] args) throws Exception {
		String text = "This is a red silk dress"; 
		
		DictionaryAnnotator ta = DictionaryAnnotator.getAnnotator(Language.EN, true);
		ta.annotate("This is a red silk dress", new HashMap<String, Object>());
	}
	
	
	@Override
	public String getName() {
		return "Dictionary Annotator";
	}

	@Override
	public String getService() {
		return "";
	}
	
	@Override
	public List<Annotation> annotate(String text, Map<String, Object> properties) throws Exception {
		List<Annotation> res = new ArrayList<>();
		
		Chunking chunking = tt.chunk(text);
	    for (Chunk chunk : chunking.chunkSet()) {
	    	JsonNode node = Json.parse(chunk.type());
	    	
	        res.add(new Annotation(this.getClass(), chunk.start(), chunk.end(), 1.0f, node.get("uri").asText(), node.get("label-en").asText(), node.get("vocabulary").asText()));
	    }

		return res;
	}

}
