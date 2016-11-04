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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.resources.ThesaurusObject.SKOSSemantic;

import org.bson.types.ObjectId;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DAGNode<T> implements Comparable<DAGNode<T>> {

	private Set<T> label;
	private Set<String> instances;
	private int size;
	
	private List<DAGNode<T>> children;
	private Set<DAGNode<T>> childrenSet;
	private Collection<DAGNode<T>> parents;
	
	public boolean isScheme;

	public DAGNode() {
		label = new HashSet<>();
		
		children = new ArrayList<>();
		childrenSet = new HashSet<>();
		parents = new HashSet<>();
	}
	
	public DAGNode(T l) {
		label = new HashSet<>();
		label.add(l);
		
		children = new ArrayList<>();
		childrenSet = new HashSet<>();
		parents = new HashSet<>();
	}

	public DAGNode(T l, int size) {
		label = new HashSet<>();
		label.add(l);
		
		children = new ArrayList<>();
		childrenSet = new HashSet<>();
		parents = new HashSet<>();
		
		this.size = size; 
	}

	
	public void setSize(int size) {
		this.size = size;
	}
	
	public Set<T> getLabel() {
		return label;
	}
	
	public Collection<DAGNode<T>> getChildren() {
		return children;
	}
	
	public Collection<DAGNode<T>> getParents() {
		return parents;
	}

	public void addChild(DAGNode<T> child) {
		if (childrenSet.add(child)) {
			children.add(child);
		}
		child.parents.add(this);
	}
	
	public void removeChild(DAGNode<T> child) {
		children.remove(child);
		childrenSet.remove(child);
		child.parents.remove(this);
	}
	
	public void removeChild(int k) {
		DAGNode<T> child = children.remove(k);
		childrenSet.remove(child);
		child.parents.remove(this);
	}


	public Set<String> getInstances() {
		return instances;
	}
	
	public int size() {
		return size;
	}


	private DAGNode<T> find(Set<String> expr, Set<DAGNode<T>> visited) {
		if (visited.add(this)) {
			if (label.containsAll(expr)) {
				return this;
			}
			
			for (DAGNode<T> node : children) {
				DAGNode<T> dg = node.find(expr, visited);
				if (dg != null) {
					return dg;
				}
			} 
		} 
		
		return null;
		
	}
	
	public String toString() {
		return (label != null? label.toString() : "NULL") + "  :  " + size;
	}

	public ObjectNode toJSON(Map<String, SKOSSemantic> map, Language lang) {
		return itoJSON(map, lang, new HashSet<>());
	}
	
	private ObjectNode itoJSON(Map<String, SKOSSemantic> map, Language lang, Set<DAGNode<T>> used) {
		T s = label.iterator().next();
		
//		if (!used.add(this)) {
//			ObjectNode json = Json.newObject();
//			json.put("ref", idMap.get(s).toString());
//			return json;
//		}

//		ObjectId id = idMap.get(s);
		
		ObjectNode element = Json.newObject();
		
		SKOSSemantic sem = map.get(s);
		String ss = "";
		if (sem != null) {
			Literal plabel = sem.getPrefLabel();
			if (plabel != null) {
				ss = plabel.getLiteral(lang);
				if (ss == null && plabel.values().size() > 0) {
					ss = plabel.values().iterator().next();
				}
			} else {
				String sv = s.toString();
				ss = s.toString().substring(Math.max(sv.lastIndexOf("/"), sv.lastIndexOf("#")) + 1);
			}
		} else {
			String sv = s.toString();
			ss = sv.substring(Math.max(sv.lastIndexOf("/"), sv.lastIndexOf("#")) + 1);
		}
		
		element.put("label", ss);
		
		if (sem != null && !sem.isScheme()) {
			element.put("uri", s.toString());
			element.put("size", size);
		}
		
		ArrayNode jchildren = Json.newObject().arrayNode();
		
		element.put("children", jchildren);
		
		for (DAGNode<T> node : children) {
			jchildren.add(node.itoJSON(map, lang, used));
		}
		
		return element;
	}


	public void print(Map<String, SKOSSemantic> map) {
		print(map, 0);
	}
	
	public void print(Map<String, SKOSSemantic> map, int depth) {
		for (int i = 0; i < depth; i++) {
			System.out.print("    ");
		}
		
		String ssx = "";
		for (T s : label) {
			String ss = "";
			if (map.get(s) != null) {
				Literal plabel = map.get(s).getPrefLabel();
				
				if (plabel != null) {
					ss = plabel.getLiteral(Language.EN);
					if (ss == null && plabel.values().size() > 0) {
						ss = plabel.values().iterator().next();
					}
				} else {
					String sv = s.toString();
					
					ss = sv.substring(Math.max(sv.lastIndexOf("/"), sv.lastIndexOf("#")) + 1);
				}
			} else {
				String sv = s.toString();
				
				ss = sv.substring(Math.max(sv.lastIndexOf("/"), sv.lastIndexOf("#")) + 1);
			}
			ssx += ss + (map.get(s) != null ? " <" + map.get(s).getUri() + "> ":" ") + size;// + " " + map.get(s).getUri();
		}
		System.out.println(ssx + " " + size);
		
		for (DAGNode<T> node : children) {
			node.print(map, depth + 1);
		}
	}
	
	public int hashCode() {
		int ret = 0;
	
		for (T s : label) {
			ret += s.hashCode();
		}
		
		return ret;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof DAGNode) {
			return label.equals(((DAGNode)obj).label);
		}
		
		return false;
	}
	
	public void normalize() {
		normalize(new HashSet<>());
	}
	
	public void normalize(Set<DAGNode<T>> used) {
		if (!used.add(this) || children.size() == 0) {
			return;
		}
		
		for (DAGNode<T> child : children) {
			child.normalize(used);
		}

		for (int i = 0; i < children.size(); i++) {
			DAGNode<T> child = children.get(i);
			for (int j = i + 1; j < children.size();) {
				if (child == children.get(j)) {
					removeChild(j);
				} else {
					j++;
				}
			}
		}
		
		if (children.size() == 1) {
			DAGNode<T> child = children.get(0);
			if (child.size() == size) {
				for (DAGNode<T> p : parents) {
					p.childrenSet.remove(this);
					p.children.set(p.children.indexOf(this), child);
					p.childrenSet.add(child);
				}
			}
		}
	}

	@Override
	public int compareTo(DAGNode<T> o) {
		if (size < o.size) {
			return 1;
		} else if (size > o.size) {
			return -1;
		} else {
			return label.toString().compareTo(o.label.toString());
		}
	}
}
