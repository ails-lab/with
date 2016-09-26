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


package controllers.thesaurus.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import model.basicDataTypes.Language;
import model.resources.ThesaurusObject;
import model.resources.ThesaurusObject.SKOSSemantic;
import model.resources.ThesaurusObject.SKOSTerm;
import db.DB;
import db.ThesaurusObjectDAO;

public class ThesaurusFacet {
	private static boolean precompute = false;
	private static Map<String, SKOSSemantic> map;
	private static Map<String, ObjectId> idMap;
	
	private static ThesaurusObjectDAO dao;
	
	static {
		dao = DB.getThesaurusDAO();
		map = new HashMap<>();
		idMap = new HashMap<>();
		
		if (precompute) {
			System.out.println("READING THESAURUSES");
			
			long start = System.currentTimeMillis();
	
			for (ThesaurusObject to : dao.getAll()) {
				idMap.put(to.getSemantic().getUri(), to.getDbId());
				map.put(to.getSemantic().getUri(), to.getSemantic()); 
			}
	
			System.out.println("INIT TIME: " + (System.currentTimeMillis() - start));
		}
	}
	
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
	
	private Collection<DAGNode<String>> tops;
	
	public ObjectNode toJSON(Language lang) {
		ObjectNode json = Json.newObject();
		
		ArrayNode schemes = Json.newObject().arrayNode();
		json.put("schemes", schemes);
		
		for (DAGNode<String> node : tops) {
			schemes.add(node.toJSON(idMap, map, lang));
		}

		return json;
	}
	
	private Set<DAGNode<String>> schemeNodes;
	private Set<DAGNode<String>> points;
	
	private DAGNode<String> addNode(Map<String, DAGNode<String>> nodeMap, String term, int size,  Set<String> selected) {
		DAGNode<String> node = nodeMap.get(term);
		if (node == null) {
			node = new DAGNode<String>(term, size);
			nodeMap.put(term, node);
			tops.add(node);
	
			if (selected.contains(term)) {
				points.add(node);
			}
			
			SKOSSemantic sem = getSemantic(term);
			if (sem != null) {
				List<String> schemes = sem.getInSchemes();
				if (schemes != null) {
					loop:
					for (String sc : schemes) {
						List<SKOSTerm> tops = getSemantic(sc).getTopConcepts();
						for (SKOSTerm t : tops) {
							if (t.getUri().equals(term)) {
								schemeNodes.add(node);
								break loop;
							}
						}
					}
				}
			}

		}

		return node;
	}
	
	public void create(List<String[]> list, Set<String> selected) {

		schemeNodes = new HashSet<>();
		
		Map<String, DAGNode<String>> nodeMap = new HashMap<>();
		Map<String, Counter> counterMap = new HashMap<>();
		
//		Map<String, Counter> levelMap = new HashMap<>();
		
		tops = new HashSet<>();
		
//		DAGNode<String> flattop = new DAGNode<String>();
		
//		boolean computeBroader = false;
		for (String[] uris : list) {
			Set<String> used = new HashSet<>();

			if (uris != null) {
				for (String uri : uris) {
					SKOSSemantic semantic = getSemantic(uri);

					count(counterMap, uri, used);
	//				count(levelMap, uri, fused);
					if (semantic != null) {
						Collection<SKOSTerm> broader = semantic.getBroaderTransitive();
						
//						if (broader == null && semantic.getBroader() != null) {
//							broader = constructBroaderTransitive(semantic);
//						}

						if (broader != null) {
//							computeBroader = true;
							for (SKOSTerm term : broader) {
								count(counterMap, term.getUri(), used);
							}
						}
					}
				}
			}
		}
		
		
		points = new HashSet<>();
		
		Set<String> used = new HashSet<>();
		
		for (Map.Entry<String, Counter> entry : counterMap.entrySet()) {
			String term = entry.getKey();
			
//			System.out.println(term);
			if (!used.add(term)) {
				continue;
			}
			
			SKOSSemantic semantic = getSemantic(term);

//			if (semantic != null && computeBroader) {
			if (semantic != null) {
				DAGNode<String> node = addNode(nodeMap, term, entry.getValue().getValue(), selected);
				
				List<SKOSTerm> broaderList = semantic.getBroader();
				
//				System.out.println(">>> " + broaderList);
				
				if (broaderList != null) {
					for (SKOSTerm broader : broaderList) {
						String broaderURI = broader.getUri();
						
						DAGNode<String> parent = addNode(nodeMap, broaderURI, counterMap.get(broaderURI).getValue(), selected);
						
						parent.addChild(node);
						tops.remove(node);
					}
				}
			}
		}

		Set<DAGNode<String>> changedParents = new HashSet<>();

		for (DAGNode<String> p : points) {
			
			Set<DAGNode<String>> pcopy = new HashSet<>(p.getParents());
			
			for (DAGNode<String> parent : pcopy) {
				parent.removeChild(p);
				changedParents.add(parent);
			}

			DAGNode<String> top = p;
			
			while (top.getParents().size() > 0) {
				top = top.getParents().iterator().next();
			}
			
			for (DAGNode<String> child : p.getChildren()) {
				top.addChild(child);
			}
		}

		for (DAGNode<String> p : schemeNodes) {
			Set<DAGNode<String>> pcopy = new HashSet<>(p.getParents());
			
			for (DAGNode<String> parent : pcopy) {
				parent.removeChild(p);
				changedParents.add(parent);
			}

			tops.add(p);
		}
		
		while (changedParents.size() > 0) {
			Set<DAGNode<String>> newChangedParents = new HashSet<>();

			for (DAGNode<String> p : changedParents) {
				if (p.getChildren().size() == 0) {
					Set<DAGNode<String>> pcopy = new HashSet<>(p.getParents());
					
					for (DAGNode<String> parent : pcopy) {
						parent.removeChild(p);
						newChangedParents.add(parent);
					}
				}
			}
			
			changedParents = newChangedParents;
		}
		
	
//		System.out.println(toJSON(Language.EN));
		
		for (DAGNode<String> top : tops) {
			top.normalize(selected);
		}
		
		Map<String, DAGNode<String>> schemes = new HashMap<>(); 
				
		for (DAGNode<String> top : tops) {
			String uri = top.getLabel().iterator().next();

			SKOSSemantic sem = getSemantic(uri);
			
			if (sem != null ) {
				List<String> scs = sem.getInSchemes();
				if (scs != null) {
					for (String sc : scs) {
						getSemantic(sc);
//						System.out.println("SCHEME" + sc);
						DAGNode<String> dg = schemes.get(sc);
						
						if (dg == null) {
							dg = new DAGNode<String>(sc);
							schemes.put(sc,dg);
						}
						
						dg.addChild(top);
					}
				}
			}
		}
		
		tops = schemes.values();
		
		for (DAGNode<String> top : tops) {
			Collection<DAGNode<String>> ch = top.getChildren();
			if (ch.size() == 1) {
				DAGNode<String> child = ch.iterator().next();
				Collection<DAGNode<String>> ch2 = child.getChildren();
				
				top.removeChild(child);
				for (DAGNode<String> cc : ch2) {
					top.addChild(cc);
				}
			}
		}
	}
	
	public Collection<SKOSTerm> constructBroaderTransitive(SKOSSemantic skos) {
		Collection<SKOSTerm> used = new HashSet<>();
		
		List<SKOSSemantic> queue = new ArrayList<>();
		for (SKOSTerm c : skos.getBroader()) {
			if (used.add(c)) {
				queue.add(getSemantic(c.getUri()));
			}
		}
		
		int i = 0;
		while (queue.size() > i) {
			SKOSSemantic current = queue.get(i++);
//			System.out.println("S " + i + " " + current + " " + current.getUri());
			
			List<SKOSTerm> list = current.getBroader();
			if (list != null) {
				for (SKOSTerm c : list) {
					if (used.add(c)) {
						SKOSSemantic br = getSemantic(c.getUri());
						if (br != null) {
							queue.add(br);
						}
					}
				}
			}
		}
		
		return used;
	}

}
