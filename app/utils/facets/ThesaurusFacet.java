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

import org.bson.types.ObjectId;

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
	private static Map<String, ObjectId> idMap = new HashMap<>();
	
	public ThesaurusFacet() {
	}

	
	public SKOSSemantic getSemantic(String term) {
		SKOSSemantic res = map.get(term);
		if (res == null) {
			ThesaurusObject to = DB.getThesaurusDAO().getByUri(term);
			if (to != null) {
				res = to.getSemantic();
				map.put(res.getUri(), res);
				idMap.put(res.getUri(), to.getDbId());
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
	private Set<DAGNode<String>> schemeTopNodes;
	private Set<DAGNode<String>> selectedTerms;
	
	public static void count(Map<String, Counter> counterMap, String term, Set<String> used) {
		Counter c = counterMap.get(term);
		if (c == null) {
			c = new Counter(0);
			counterMap.put(term, c);
		}
		if (used.add(term)) {
			c.increase();
		}
	}
	
	private DAGNode<String> addNode(Map<String, DAGNode<String>> nodeMap, String term, int size,  Set<String> selected) {
		DAGNode<String> node = nodeMap.get(term);

		if (node == null) {
			node = new DAGNode<String>(term, size);
			nodeMap.put(term, node);
		
			tops.add(node);
	
			if (selected.contains(term)) {
				selectedTerms.add(node);
			}
			
			SKOSSemantic sem = getSemantic(term);
			if (sem != null && sem.isSchemeTopConcept()) {
				schemeTopNodes.add(node);
			}
		}

		return node;
	}
	
	public void create(List<String[]> list, Set<String> selected) {

		tops = new HashSet<>();
		schemeTopNodes = new HashSet<>();
		selectedTerms = new HashSet<>();
		
		Map<String, Counter> counterMap = new HashMap<>();
		
		for (String[] uris : list) {
			Set<String> used = new HashSet<>();

			if (uris != null) {
				for (String uri : uris) {
					SKOSSemantic semantic = getSemantic(uri);

					if (semantic != null) {
						count(counterMap, uri, used);

						Collection<SKOSTerm> broader = semantic.getBroaderTransitive();
						
						if (broader != null) {
							for (SKOSTerm term : broader) {
								count(counterMap, term.getUri(), used);
							}
						} 
					}
				}
			}
		}
		
		Map<String, DAGNode<String>> nodeMap = new HashMap<>();

		Set<DAGNode<String>> toRemove = new HashSet<>();
		
		for (Map.Entry<String, Counter> entry : counterMap.entrySet()) {
			String term = entry.getKey();
			DAGNode<String> node = addNode(nodeMap, term, entry.getValue().getValue(), selected);

			List<SKOSTerm> broaderList = getSemantic(term).getBroader();
			
			if (broaderList != null) {
				for (SKOSTerm broader : broaderList) {
					String broaderURI = broader.getUri();
					
					DAGNode<String> parent = addNode(nodeMap, broaderURI, counterMap.get(broaderURI).getValue(), selected);
					
					parent.addChild(node);
					toRemove.add(node);
				}
			}
		}
		
		tops.removeAll(toRemove);

//		System.out.println("TOP1");
//		for (DAGNode t : tops) { 
//			t.print(map);
//		}
		
		Set<DAGNode<String>> changedParents = new HashSet<>();

		for (DAGNode<String> p : selectedTerms) {
			for (DAGNode<String> parent : new HashSet<>(p.getParents())) {
				parent.removeChild(p);
				changedParents.add(parent);
			}

//			DAGNode<String> top = p;
			
//			while (top.getParents().size() > 0) {
//				top = top.getParents().iterator().next();
//			}
			
			for (DAGNode<String> child : p.getChildren()) {
				tops.add(child);
			}
		}

		for (DAGNode<String> p : schemeTopNodes) {
			for (DAGNode<String> parent : new HashSet<>(p.getParents())) {
				parent.removeChild(p);
				changedParents.add(parent);
			}
			
			tops.add(p);
		}
		
//		System.out.println("TOP2");
//		for (DAGNode t : tops) { 
//			t.print(map);
//		}

		Map<String, DAGNode<String>> schemes = new HashMap<>(); 
		Map<String, DAGNode<String>> vocabularies = new HashMap<>();
		
		for (DAGNode<String> top : tops) {
			String uri = top.getLabel().iterator().next();

			SKOSSemantic sem = getSemantic(uri);
			
			List<String> scs = sem.getInSchemes();
			if (scs != null) {
				for (String sc : scs) {
					SKOSSemantic schSem = getSemantic(sc);
					DAGNode<String> dg = schemes.get(sc);
					System.out.println(sc +  " " + schSem);
					
					if (dg == null) {
						dg = new DAGNode<String>(sc, top.size());
						schemes.put(sc,dg);
						
						String vocName = schSem.getVocabulary().getName();
						DAGNode<String> voc = vocabularies.get(vocName);
						if (voc == null) {
							voc = new DAGNode<String>(Vocabulary.getVocabulary(vocName).getLabel());
							vocabularies.put(vocName, voc);
						}
						voc.addChild(dg);
					} else {
						dg.setSize(Math.max(dg.size(), top.size()));
					}
					dg.addChild(top);
				}
			} else {
				String vocName = sem.getVocabulary().getName();
				DAGNode<String> voc = vocabularies.get(vocName);
				if (voc == null) {
					voc = new DAGNode<String>(Vocabulary.getVocabulary(vocName).getLabel());
					vocabularies.put(vocName, voc);
				}
				voc.addChild(top);
				
			}
		}
		
		while (changedParents.size() > 0) {
			Set<DAGNode<String>> newChangedParents = new HashSet<>();

			for (DAGNode<String> p : changedParents) {
				if (p.getChildren().size() == 0) {
					for (DAGNode<String> parent : new HashSet<>(p.getParents())) {
						parent.removeChild(p);
						newChangedParents.add(parent);
					}
				}
			}
			
			changedParents = newChangedParents;
		}

		tops = vocabularies.values();
		
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
