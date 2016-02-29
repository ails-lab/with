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


package controllers.thesaurus;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

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
				idMap.put(to.getSemantic().getUri(), to.getDbid());
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
			res = to.getSemantic();
			map.put(res.getUri(), res);
			idMap.put(res.getUri(), to.getDbid());
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
	
	private Set<DAGNode<String>> tops;
	
	public String toJSON(Language lang) {
		StringBuffer res = new StringBuffer("{ \"schemes\": [");
		
		int i = 0;
		for (DAGNode<String> node : tops) {
			if (i++ > 0) {
				res.append(", ");
			}
			res.append(node.toJSON(idMap, map, lang));
		}
		
		res.append("] }");
		
		return res.toString();
	}
	
	public void create(List<String[]> list, Set<String> selected) {
//		long start = System.currentTimeMillis();
		
		Map<String, DAGNode<String>> nodeMap = new HashMap<>();
		Map<String, Counter> counterMap = new HashMap<>();
		
		Map<String, Counter> levelMap = new HashMap<>();
		
		tops = new HashSet<>();
		
//		DAGNode<String> flattop = new DAGNode<String>();
		
		for (String[] uris : list) {
			Set<String> used = new HashSet<>();
			Set<String> fused = new HashSet<>();
			for (String uri : uris) {
				SKOSSemantic semantic = getSemantic(uri);
				
				count(counterMap, uri, used);
				count(levelMap, uri, fused);
				
				List<SKOSTerm> broader = semantic.getBroaderTransitive();
				if (broader != null) {
					for (SKOSTerm term : broader) {
						count(counterMap, term.getUri(), used);
					}
				}
			}
		}
		
		
//		System.out.println("A " + (System.currentTimeMillis() - start));

		Set<String> used = new HashSet<>();
		
		for (Map.Entry<String, Counter> entry : counterMap.entrySet()) {
			String term = entry.getKey();
			if (!used.add(term)) {
				continue;
			}
			
			DAGNode<String> node = nodeMap.get(term);
			if (node == null) {
				node = new DAGNode<String>(term, entry.getValue().getValue());
				nodeMap.put(term, node);
				tops.add(node);
			}
			
			List<SKOSTerm> broaderList = getSemantic(term).getBroader();
			
			if (broaderList != null) {
				for (SKOSTerm broader : broaderList) {
					String broaderURI = broader.getUri();
					
					DAGNode<String> parent = nodeMap.get(broaderURI);
					if (parent == null) {
						parent = new DAGNode<String>(broaderURI, counterMap.get(broaderURI).getValue());
						nodeMap.put(broaderURI, parent);
						tops.add(parent);
					}
					
					parent.addChild(node);
					tops.remove(node);
				}
			}
		}

		Set<DAGNode<String>> newTops = new HashSet<>();
		
		for (DAGNode<String> top : tops) {
//			top.print(map);
			top.normalize(selected);
//			if (norm.size() > 0) {
//				newTops.add(top);
//			}
//			top.print(map);
//			res.addChild(top);
		}
//		
//		tops = newTops;


	}

}
