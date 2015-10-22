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


package espace.core.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonContextRecord {

	private JsonNode rootInformation;
	private List<String> context;
	
	public JsonContextRecord(JsonNode rootInformation) {
		this();
		this.rootInformation = rootInformation;
	}
	
	public JsonContextRecord(String jsonString) {
		this();
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.rootInformation = mapper.readTree(jsonString);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public JsonContextRecord() {
		super();
		context = new ArrayList<>();
	}
	
	public void enterContext(String path){
		
	}
	
	public void exitContext(){
		context.remove(context.size()-1);
	}
	
	protected List<String> buildpaths(String... path){
		List<String> spath = new ArrayList<>();
		for (String string : context) {
			String[] singlepaths = string.split("\\.");
			spath.addAll(Arrays.asList(singlepaths));
		}
		for (String string : path) {
			String[] singlepaths = string.split("\\.");
			spath.addAll(Arrays.asList(singlepaths));
		}
		return spath;
	}
	
	public JsonNode getValue(Collection<String> steps){
		JsonNode node = rootInformation;
		for (String string : steps) {
			node = getPath(node, string);
			if (node==null)
				return null;
		}
		return node;
	}
	
	public void setValue(String steps, String val){
		List<String> path = buildpaths(steps);
		setValue(path,val);
	}
	
	public void setValue(String steps, JsonNode val){
		List<String> path = buildpaths(steps);
		setValue(path,val);
	}

	
	public void setValue(Collection<String> steps, String val){
		ObjectMapper mapper = new ObjectMapper();
		setValue(steps,mapper .convertValue(val, JsonNode.class));
	}

	
	public void setValue(Collection<String> steps, JsonNode val){
		JsonNode node = rootInformation;
		for (Iterator<String> iterator = steps.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			JsonNode path = getPath(node, string);
			if (!node.has(string)){
				ObjectNode n = (ObjectNode) node;
				n.put(string, buildNewObject(iterator,val, new ObjectMapper()));
			} else {
				node = path;
			}
		}
	}

	private JsonNode buildNewObject(Iterator<String> iterator, JsonNode val, ObjectMapper map) {
		if (!iterator.hasNext()){
			return val;
		} else{
			ObjectNode n = map.createObjectNode();
			n.put(iterator.next(), buildNewObject(iterator, val, map));
			return n;
		}
	}

	private JsonNode getPath(JsonNode node, String path) {
		if (path.endsWith("]")){
			String[] elements = path.split("\\[|\\]");
			String p = elements[0];
				int index = Integer.parseInt(elements[1]);
				return node.path(p).get(index);
		}
		return node.path(path);
	}
	
	public JsonNode getValue(String... path){
		return getValue(buildpaths(path));
	}
	
	public String getStringValue(String... path){
		return JsonNodeUtils.asString(getValue(buildpaths(path)));
	}
	public List<String> getStringArrayValue(String... path){
		return JsonNodeUtils.asStringArray(getValue(buildpaths(path)));
	}
	
	
	public JsonNode getRootInformation() {
		return rootInformation;
	}

	public void setRootInformation(JsonNode rootInformation) {
		this.rootInformation = rootInformation;
	}

	public static void main(String[] args) {
		JsonContextRecord r = new JsonContextRecord("{\"a\":\"b\",\"ages\":[1,2], \"f\":{\"size\":\"4\"}}");
		JsonNode value = r.getValue("f.size3.jk");
		System.out.println(value);
		System.out.println(r.getValue("ages[1]"));
		r.setValue("f.color.name.enrique", r.getValue("a"));
		r.setValue("f.color.name.mijalis", r.getValue("a"));
		System.out.println(r.getRootInformation().toString());
	}
	
}
