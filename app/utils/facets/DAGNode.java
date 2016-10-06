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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
	
	public boolean explicit;
	
	private Collection<DAGNode<T>> children;
	private Collection<DAGNode<T>> parents;

	public DAGNode() {
		label = new HashSet<>();
		
		children = new TreeSet<>();
		parents = new HashSet<>();
	}
	
	public DAGNode(T l) {
		label = new HashSet<>();
		label.add(l);
		
		children = new TreeSet<>();
		parents = new HashSet<>();
	}

	public DAGNode(T l, int size) {
		label = new HashSet<>();
		label.add(l);
		
		children = new TreeSet<>();
		parents = new HashSet<>();
		
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
		children.add(child);
		child.parents.add(this);
	}
	
	public void removeChild(DAGNode<T> child) {
		children.remove(child);
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
		return (label !=null?label.toString():"NULL") + "  :  " + size;
	}

	public ObjectNode toJSON(Map<String, ObjectId> idMap, Map<String, SKOSSemantic> map, Language lang) {
		return itoJSON(idMap, map, lang, new HashSet<>());
	}

	
	private ObjectNode itoJSON(Map<String, ObjectId> idMap, Map<String, SKOSSemantic> map, Language lang, Set<DAGNode<T>> used) {
		if (!used.add(this)) {
			return Json.newObject();
		}

		T s = label.iterator().next();

		ObjectId id = idMap.get(s);
		
		ObjectNode element = Json.newObject();
		
		if (id != null) {
			Literal plabel = map.get(s).getPrefLabel();
			String ss = "";
			if (plabel != null) {
				ss = plabel.getLiteral(lang);
				if (ss == null && plabel.values().size() > 0) {
					ss = plabel.values().iterator().next();
				}
			} else {
				String sv = s.toString();
				
				ss = sv.substring(Math.max(sv.lastIndexOf("/"), sv.lastIndexOf("#")));
			}
			
			element.put("id", idMap.get(s).toString());
			element.put("uri", s.toString());
			element.put("label", ss);
			element.put("size", size);
			
			ArrayNode jchildren = Json.newObject().arrayNode();
			
			element.put("children", jchildren);
			
			for (DAGNode<T> node : children) {
				jchildren.add(node.itoJSON(idMap, map, lang, used));
			}
		}
		
		return element;
	}


	public void print(Map<String, SKOSSemantic> map) {
		print(map, 0);
	}
	
	public void print(Map<String, SKOSSemantic> map, int depth) {
		for (int i = 0; i < depth; i++) {
			System.out.print("   ");
		}
		
		String ss = "";
		for (T s : label) {
			ss += (explicit? "*":" ") + map.get(s).getPrefLabel().getLiteral(Language.EN) + " " + size;// + " " + map.get(s).getUri();
		}
		System.out.println(ss);
		
		for (DAGNode<T> node : children) {
			node.print(map, depth + 1);
		}
	}
	public int hashCode() {
		return label.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof DAGNode) {
			return label.equals(((DAGNode)obj).label);
		}
		
		return false;
	}
	
	public void normalize(Set<T> selected) {
		normalize(selected, new HashSet<>());
	}
	
	public void normalize(Set<T> selected, Set<DAGNode<T>> used) {
		if (!used.add(this)) {
			return;
		}
		
		for (Iterator<DAGNode<T>> iter =  children.iterator(); iter.hasNext();) {
			DAGNode<T> child = iter.next();
			child.normalize(selected, used);
		}
		
	
		Set<DAGNode<T>> toAdd = new HashSet<>();

		for (Iterator<DAGNode<T>> iter = children.iterator(); iter.hasNext();) {
			DAGNode<T> child = iter.next();
			
			if (child.size() == size) {
				iter.remove();
				toAdd.addAll(child.children);
				label.clear();
				label.addAll(child.getLabel());
			}
		}
				
		for (DAGNode<T> ch : toAdd) {
			addChild(ch);
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
