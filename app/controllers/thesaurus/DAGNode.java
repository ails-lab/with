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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.atlas.lib.SetUtils;
import org.bson.types.ObjectId;

import model.basicDataTypes.Language;
import model.resources.ThesaurusObject.SKOSSemantic;

public class DAGNode<T> implements Comparable<DAGNode<T>> {

	private Set<T> label;
	private Set<String> instances;
	private int size;
	
	public boolean explicit;
	
	private Collection<DAGNode<T>> children;
	private Collection<DAGNode<T>> parents;

	public DAGNode() {
		label = new HashSet<>();
		
//		children = new ArrayList<>();
		children = new TreeSet<>();
		parents = new HashSet<>();
	}
	
	public DAGNode(T l) {
		label = new HashSet<>();
		label.add(l);
		
//		children = new ArrayList<>();
		children = new TreeSet<>();
		parents = new HashSet<>();
	}

	public DAGNode(T l, int size) {
		label = new HashSet<>();
		label.add(l);
		
//		children = new ArrayList<>();
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
//		if (!children.contains(child)) {
			children.add(child);
			child.parents.add(this);
//		}
	}
	
	public void removeChild(DAGNode<T> child) {
//		if (!children.contains(child)) {
			children.remove(child);
			child.parents.remove(this);
//		}
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
	
//	
//	/// NEW CODE ------------------------------------------------------------------
//	public ArrayList<DAGNode> getDescendents(boolean include) {
//		Set<DAGNode> visited = new HashSet<>();
//		
//		ArrayList<DAGNode> res = new ArrayList<>();
//		res.add(this);
//		
//		int i = 0;
//		while (i < res.size()) {
//			DAGNode node = res.get(i++);
//			
//			for (DAGNode child : node.children) {
//				if (visited.add(child)) {
//					res.add(child);
//				}
//			}
//		}
//		
//		if (!include) {
//			res.remove(0);
//		}
//		
//		return res;
//		
//	}
//	
//	public ArrayList<DAGNode> getAncestors(boolean include) {
//		Set<DAGNode> visited = new HashSet<>();
//		
//		ArrayList<DAGNode> res = new ArrayList<>();
//		res.add(this);
//		
//		int i = 0;
//		while (i < res.size()) {
//			DAGNode node = res.get(i++);
//			
//			for (DAGNode parent : node.parents) {
//				if (visited.add(parent)) {
//					res.add(parent);
//				}
//			}
//		}
//		
//		if (!include) {
//			res.remove(0);
//		}
//		
//		return res;
//		
//	}
//	

	public String toString() {
		return label.toString() + "  :  " + size;
	}

	public String toJSON(Map<String, ObjectId> idMap, Map<String, SKOSSemantic> map, Language lang) {
		return itoJSON(idMap, map, lang).toString();
	}
	
	private StringBuffer itoJSON(Map<String, ObjectId> idMap, Map<String, SKOSSemantic> map, Language lang) {

		T s = label.iterator().next();

		ObjectId id = idMap.get(s);
		
		StringBuffer sb = new StringBuffer();

		if (id != null) {
			sb.append("{ \"id\":\"" + idMap.get(s).toString() + "\", \"uri\":\"" + s + "\", \"label\":\"" + map.get(s).getPrefLabel().getLiteral(lang) + "\", \"size\":\"" + size + "\", \"children\": [");
			
			int i = 0;
			for (DAGNode<T> node : children) {
				StringBuffer r1 = node.itoJSON(idMap, map, lang);
				if (r1.length() > 0) {
					if (i++ > 0) {
						sb.append(", ");
					}
					sb.append(r1);
				}
			}
			
			sb.append("] }");
			
		}
		
		return sb;

		
		

	}


	public void print(Map<String, SKOSSemantic> map) {
		print(map, 0);
	}
	
	public void print(Map<String, SKOSSemantic> map, int depth) {
		for (int i = 0; i < depth; i++) {
			System.out.print("   ");
		}
		
//		System.out.println(toString());
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
	
//	private static double THRESHOLD = 0.05;
	
	public boolean childRemoved = false;
	
	public void normalize(Set<T> selected) {

//		int totalch = 0;
		Collection<DAGNode<T>> toAdd = new HashSet<>();
		
//		System.out.println(this.getLabel());
//		for (Iterator<DAGNode<T>> iter =  children.iterator(); iter.hasNext();) {
//			System.out.println("\t" + iter.next().getLabel());
//		}
		
		for (Iterator<DAGNode<T>> iter =  children.iterator(); iter.hasNext();) {
			DAGNode<T> child = iter.next();
			child.normalize(selected);
		}
		
//		for (Iterator<DAGNode<T>> iter =  children.iterator(); iter.hasNext();) {
//			DAGNode<T> child = iter.next();
//			
//			if (child.childRemoved && child.children.size() == 0) {
////				iter.remove();
//				childRemoved = true;
//				continue;
//			}
//
//			if (SetUtils.intersection(child.label, selected).size() > 0) {
//				iter.remove();
//				childRemoved = true;
//				
//				toAdd.addAll(child.children);
//			}			

			
//			if (child.size() < size*THRESHOLD) {
//				iter.remove();
//			}
//			totalch += child.size();
//		}
		
//		for (DAGNode<T> cc : toAdd) {
//			children.add(cc);
//		}
		
		if (children.size() == 1) {
			DAGNode<T> child = children.iterator().next();
			if (size == child.size) {
				children.clear();
				children.addAll(child.children);
				label.clear();
				label.addAll(child.getLabel());
				
				childRemoved = child.childRemoved;
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
