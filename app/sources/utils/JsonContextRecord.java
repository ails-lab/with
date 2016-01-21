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


package sources.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithDate;
import play.libs.Json;
import scala.collection.mutable.HashMap;

public class JsonContextRecord {

	private JsonNode rootInformation;
	private List<String> context;

	public JsonContextRecord(JsonNode rootInformation) {
		this.context = new ArrayList<>();
		this.rootInformation = rootInformation;
	}

	public JsonContextRecord(String jsonString) {
		// TODO: what us context?
		context = new ArrayList<>();
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

	public void enterContext(String path) {
		context.add(path);
	}

	public void exitContext() {
		context.remove(context.size() - 1);
	}

	protected List<String> buildpaths(String... path) {
		// System.out.println(Arrays.toString(path));
		List<String> spath = new ArrayList<>();
		// TODO: what is context?
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

	public JsonNode getValue(Collection<String> steps) {
		JsonNode node = rootInformation;
		// TODO: if one null, return null?
		for (String string : steps) {
			node = getPath(node, string);
			if (node == null)
				return null;
		}
		return node;
	}

	public void setValue(String steps, String val) {
		List<String> path = buildpaths(steps);
		setValue(path, val);
	}

	public void setValue(String steps, JsonNode val) {
		List<String> path = buildpaths(steps);
		setValue(path, val);
	}

	public void setValue(Collection<String> steps, String val) {
		ObjectMapper mapper = new ObjectMapper();
		setValue(steps, mapper.convertValue(val, JsonNode.class));
	}

	public void setValue(Collection<String> steps, JsonNode val) {
		JsonNode node = rootInformation;
		for (Iterator<String> iterator = steps.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			JsonNode path = getPath(node, string);
			if (!node.has(string)) {
				ObjectNode n = (ObjectNode) node;
				n.put(string, buildNewObject(iterator, val, new ObjectMapper()));
			} else {
				node = path;
			}
		}
	}

	private JsonNode buildNewObject(Iterator<String> iterator, JsonNode val, ObjectMapper map) {
		if (!iterator.hasNext()) {
			return val;
		} else {
			ObjectNode n = map.createObjectNode();
			n.put(iterator.next(), buildNewObject(iterator, val, map));
			return n;
		}
	}

	private JsonNode getPath(JsonNode node, String path) {
		if (path.endsWith("]")) {
			String[] elements = path.split("\\[|\\]");
			String p = elements[0];
			// is a index
			try {
				int index = Integer.parseInt(elements[1]);
				return node.path(p).get(index);
			} catch (NumberFormatException e) {
				// should be a condition:
				node = node.path(p);
				for (int i = 0; i < node.size(); i++) {
					JsonNode current = node.get(i);
					elements = elements[1].split(",");
					for (int h = 0; h < elements.length; h++) {
						String string = elements[h];
						String[] splits = string.split("=");
						String name = splits[0];
						String vals = splits[1];
						if (current.path(name).asText().matches(vals)) {
							return current;
						}
					}

				}
			}
		}
		return node.path(path);
	}

	public JsonNode getValue(String... path) {
		return getValue(buildpaths(path));
	}

	public String getStringValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		if (node != null)
			return JsonNodeUtils.asString(node);
		else
			return null;
	}

	public int getIntValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		try {
			return Integer.parseInt(JsonNodeUtils.asString(node));
		} catch (Exception e) {
			return 0;
		}
	}

	public MultiLiteral getMultiLiteralValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		if (node != null)
			return JsonNodeUtils.asMultiLiteral(node);
		else
			return null;
	}
	
	public Literal getLiteralValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		if (node != null)
			return JsonNodeUtils.asLiteral(node);
		else
			return null;
	}

	public MultiLiteralOrResource getMultiLiteralOrResourceValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		if (node != null)
			return JsonNodeUtils.asMultiLiteralOrResource(node);
		else
			return null;
	}
	
	public LiteralOrResource getLiteralOrResourceValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		if (node != null)
			return JsonNodeUtils.asLiteralOrResource(node);
		else
			return null;
	}

	public List<String> getStringArrayValue(String... path) {
		return JsonNodeUtils.asStringArray(getValue(buildpaths(path)));
	}
	
	public List<WithDate> getWithDateArrayValue(String... path) {
		return JsonNodeUtils.asWithDateArray(getValue(buildpaths(path)));
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
		HashMap<String, String> f = new HashMap<>();
		f.put("en", "Hello World");
		System.out.println(Json.toJson(f).toString());
		Literal l = new Literal(Language.EN, "hello");
		System.out.println(Json.toJson(l).toString());
		MultiLiteralOrResource ml = new MultiLiteralOrResource();
		ml.addLiteral(Language.EN, "bye");
		ml.addLiteral(Language.DEF, "http://www.google.com");

		System.out.println(Json.toJson(ml).toString());
		System.out.println(r.getRootInformation().toString());
	}

}
