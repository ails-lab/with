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


package utils.facets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import utils.Counter;
import vocabularies.Vocabulary;
import model.basicDataTypes.Language;
import model.resources.ThesaurusObject;
import model.resources.ThesaurusObject.SKOSSemantic;
import model.resources.ThesaurusObject.SKOSTerm;
import db.DB;

public class ThesaurusFacet {
	private static Map<String, SKOSSemantic> map = new HashMap<>();
	
	public ThesaurusFacet() {
	}
	
	public SKOSSemantic getSemantic(String term) {
		SKOSSemantic res = map.get(term);
		if (res == null) {
			ThesaurusObject to = DB.getThesaurusDAO().getByUri(term);
			if (to != null) {
				res = to.getSemantic();
				map.put(res.getUri(), res);
			}
		}
		
		return res;
	}
	

	public ObjectNode toJSON(Language lang) {
		ObjectNode json = Json.newObject();
		
		ArrayNode schemes = Json.newObject().arrayNode();
		json.put("schemes", schemes);
		
		for (DAGNode<String> node : tops) {
			schemes.add(node.toJSON(map, lang));
		}
		
		return json;
	}

	private Collection<DAGNode<String>> tops;
//	private Set<DAGNode<String>> selectedTerms;
	private Map<String, Counter> counterMap;
	
	public Set<String> getTopConceptInSchemes(SKOSSemantic sem) {
		Set<String> res = new HashSet<>();
		
		List<String> schemes = sem.getInSchemes();
		if (schemes != null) {
			for (String sc : schemes) {
				SKOSSemantic cc = getSemantic(sc);
				if (cc != null && cc.getTopConcepts().contains(sem)) {
					res.add(sc);
				}
			}
		}
		
		return res;
	}
	
	private DAGNode<String> addNode(Map<String, DAGNode<String>> nodeMap, String term, int size) {
		DAGNode<String> node = nodeMap.get(term);

		if (node == null) {
			node = new DAGNode<String>(term, size);
			nodeMap.put(term, node);
		}

		return node;
	}
	
	public void count(String term, Set<String> used) {
		Counter c = counterMap.get(term);
		if (c == null) {
			c = new Counter(0);
			counterMap.put(term, c);
		}
		c.increase();
	}
	
	private void add(SKOSSemantic sem, Set<String> selected, Set<String> used) {
		String term = sem.getUri();
	
		if (used.add(term)) {
			if (!selected.contains(term)) {
				count(term, used);
	
				Set<String> schemes = getTopConceptInSchemes(sem);
				if (schemes.size() > 0) {
					for (String scheme : schemes) {
						if (used.add(scheme)) {
							count(scheme, used);
						}
					}
				} else {
					List<SKOSTerm> broader = sem.getBroader();
					if (broader != null) {
						for (SKOSTerm br : broader) {
							add(getSemantic(br.getUri()), selected, used);
						}
					}
				}
			} else {
				List<String> schemes = sem.getInSchemes();
				if (schemes != null) {
					for (String scheme : schemes) {
						if (used.add(scheme)) {
							count(scheme, used);
						}
					}
				}
			}
		}
	}
	
	public void create(List<String[]> list, Set<String> selected) {

		tops = new HashSet<>();
		counterMap = new HashMap<>();
		
		for (String[] uris : list) {
			Set<String> used = new HashSet<>();

			for (String uri : uris) {
				SKOSSemantic sem = getSemantic(uri);
				if (sem != null) {
					add(sem, selected, used);
				}
			}
		}
		
		Map<String, DAGNode<String>> nodeMap = new HashMap<>();

		for (Map.Entry<String, Counter> entry : counterMap.entrySet()) {
			String term = entry.getKey();
			SKOSSemantic sem = getSemantic(term);
			
			DAGNode<String> node = addNode(nodeMap, term, entry.getValue().getValue());

			boolean hasParent = false;			

			Set<String> schemes = getTopConceptInSchemes(sem);
			if (schemes.size() > 0) {
				for (String scheme : schemes) {
					if (counterMap.get(scheme) != null) {
						DAGNode<String> parent = addNode(nodeMap, scheme, counterMap.get(scheme).getValue());
						parent.addChild(node);
						tops.add(parent);
						parent.isScheme = true;
						hasParent = true;
					}
				}
			} else {
				List<SKOSTerm> broaderList = sem.getBroader();

				if (broaderList != null) {
					for (SKOSTerm broader : broaderList) {
						String broaderURI = broader.getUri();
						
						if (counterMap.get(broaderURI) != null) {
							DAGNode<String> parent = addNode(nodeMap, broaderURI, counterMap.get(broaderURI).getValue());
							parent.addChild(node);
							hasParent = true;
						} 
					}
				}
			}
			
			if (!hasParent) {
				List<String> sschemes = sem.getInSchemes();
				if (sschemes != null) {
					for (String scheme : sschemes) {
						DAGNode<String> parent = addNode(nodeMap, scheme, counterMap.get(scheme).getValue());
						parent.addChild(node);
						tops.add(parent);
						parent.isScheme = true;
					}
				} else {
					tops.add(node);
				}
			}

		}
		
//		System.out.println("TOP1");
//		for (DAGNode t : tops) { 
//			t.print(map);
//		}
		
		Map<String, DAGNode<String>> vocabularies = new HashMap<>();
		
		for (Iterator<DAGNode<String>> iter = tops.iterator();iter.hasNext();) {
			DAGNode<String> top = iter.next();
			
			String uri = top.getLabel().iterator().next();
			SKOSSemantic sem = getSemantic(uri);
			
			if (top.isScheme) {
				List<String> scs = sem.getInSchemes();
				if (scs != null) {
					for (String sc : scs) {
						nodeMap.get(sc).addChild(top);
						iter.remove();
					}
				}
			} 
		}
		
		for (Iterator<DAGNode<String>> iter = tops.iterator();iter.hasNext();) {
			DAGNode<String> top = iter.next();
			
			String uri = top.getLabel().iterator().next();
			SKOSSemantic sem = getSemantic(uri);

			String vocName = sem.getVocabulary().getName();
			DAGNode<String> voc = vocabularies.get(vocName);
			if (voc == null) {
				voc = new DAGNode<String>(Vocabulary.getVocabulary(vocName).getLabel());
				vocabularies.put(vocName, voc);
			}
			voc.addChild(top);
		}
		
		tops = vocabularies.values();
		
//		System.out.println("TOP2");
//		for (DAGNode t : tops) { 
//			t.print(map);
//		}
		
		for (Iterator<DAGNode<String>> iter =  tops.iterator(); iter.hasNext();) {
			DAGNode<String> child = iter.next();
			child.normalize();
		}
		
//		System.out.println("TOP3");
//		for (DAGNode t : tops) { 
//			t.print(map);
//		}
		
		for (DAGNode<String> top : tops) {
			Collection<DAGNode<String>> ch = top.getChildren();
			if (ch.size() == 1) {
				DAGNode<String> child = ch.iterator().next();
				Collection<DAGNode<String>> ch2 = child.getChildren();
				
				if (ch2.size() > 0) {
					int count = 0;
					for (DAGNode<String> ch2c : ch2) {
						if (ch2c.size() == child.size()) {
							count++;
						}
					}
					
					if (count == ch2.size()) {
						top.removeChild(child);
						for (DAGNode<String> cc : ch2) {
							top.addChild(cc);
						}
					}
				}
			}
		}
		
//		System.out.println("TOP5");
//		for (DAGNode t : tops) { 
//			t.print(map);
//		}
	}
}
