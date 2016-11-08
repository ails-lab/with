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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.Resource;
import model.basicDataTypes.WithDate;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import scala.collection.mutable.HashMap;
import sources.core.Utils;

public class JsonContextRecord {

	public static final ALogger log = Logger.of(JsonContextRecord.class);

	private JsonNode rootInformation;
	private List<String> context;
	private Language[] languages;

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
			log.error("", e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("", e);
		}
	}

	public JsonContextRecord(File jsonFile) {
		// TODO: what us context?
		context = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.rootInformation = mapper.readTree(jsonFile);
		} catch (JsonProcessingException e) {
			log.error("", e);
		} catch (IOException e) {
			log.error("", e);
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
			String[] singlepaths = splitPaths(string);
			spath.addAll(Arrays.asList(singlepaths));
		}
		for (String string : path) {
			String[] singlepaths = splitPaths(string);
			spath.addAll(Arrays.asList(singlepaths));
		}
		return spath;
	}

	private String[] splitPaths(String string) {
		List<String> res = new ArrayList<>();
		int d = 0;
		String current = "";
		for (int i = 0; i < string.length(); i++) {
			switch (string.charAt(i)) {
			case '[':
				d++;
				current += string.charAt(i);
				break;
			case ']':
				d--;
				current += string.charAt(i);
				break;
			case '.':
				if (d == 0) {
					res.add(current);
					current = "";
					break;
				}
			default:
				current += string.charAt(i);
				break;
			}

		}
		res.add(current);
		return res.toArray(new String[] {});
	}

	public JsonNode getValue(Collection<String> steps) {
		return getValue(steps, rootInformation);
	}

	public JsonNode getValue(Collection<String> steps, JsonNode node) {
		for (String string : steps) {
			node = getPath(node, string);
			if (node == null)
				return null;
		}
		return node;
	}

	public List<JsonNode> getValues(String... path) {
		return getValues(buildpaths(path));
	}
	public List<JsonNode> getValues(Collection<String> steps) {
		List<JsonNode> roots = new ArrayList<>();
		roots.add(rootInformation);
		for (String string : steps) {
			List<JsonNode> next = new ArrayList<>();
			for (JsonNode n : roots) {
				getPaths(n, string, next);
			}
			roots = next;
		}
		return roots;
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
				if (p.equals(""))
					return node.get(index);
				return node.path(p).get(index);
			} catch (NumberFormatException e) {
				// should be a condition:
				if (!p.equals(""))
					node = node.path(p);
				elements = elements[1].split(",");
				for (int i = 0; i < node.size(); i++) {
					JsonNode current = node.get(i);
					boolean ok = true;
					for (int h = 0; h < elements.length; h++) {
						String string = elements[h];
						ok &= checkCondition(h, current, string);
					}
					if (ok)
						return current;
				}
			}
		}
		return node.path(path);
	}

	private List<JsonNode> getPaths(JsonNode node, String path, List<JsonNode> res) {
		if (path.endsWith("]")) {
			String[] elements = path.split("\\[|\\]");
			String p = elements[0];
			// is a index
			if (Utils.isNumericInteger(elements[1])) {
				int index = Integer.parseInt(elements[1]);
				if (p.equals("")) {
					JsonNode e = node.get(index);
					if (e != null)
						res.add(e);
				} else {
					JsonNode e = node.path(p).get(index);
					if (e != null)
						res.add(e);
				}
			} else {
				// should be a condition:
				if (!p.equals(""))
					node = node.path(p);
				// get the conditions inside the []
				elements = elements[1].split(",");
				for (int i = 0; i < node.size(); i++) {
					JsonNode current = node.get(i);
					boolean ok = true;
					// check all the conditions
					for (int h = 0; h < elements.length; h++) {
						String string = elements[h];
						ok &= checkCondition(i, current, string);
					}
					if (ok && current != null)
						res.add(current);
				}
			}
		} else {
			if (node.path(path) != null)
				res.add(node.path(path));
		}
		return res;
	}

	private boolean checkCondition(int index, JsonNode current, String string) {
		if (string.matches(".*=.*")) {
			String[] splits = string.split("=");
			String name = splits[0];
			String vals = splits[1];
			String asText = getValue(buildpaths(name), current).asText();
			if (!asText.matches(vals)) {
				return false;
			}
		} else {
			if (!("" + index).matches(string)) {
				return false;
			}
		}
		return true;
	}

	public JsonNode getValue(String... path) {
		return getValue(buildpaths(path));
	}

	/**
	 * returns any of the paths that has information on it.
	 * 
	 * @param path
	 * @return
	 * @see Utils.hasInfo(...)
	 */
	public String getStringValue(String... path) {
		for (String spath : path) {
			JsonNode node = getValue(buildpaths(spath));
			if (node != null) {
				String res = JsonNodeUtils.asString(node);
				if (Utils.hasInfo(res))
					return res;
			}
		}
		return null;
	}

	public Language[] getLanguages() {
		return languages;
	}

	public void setLanguages(Language[] languages) {
		this.languages = languages;
	}

	public int getIntValue(String... path) {
		JsonNode node = getValue(buildpaths(path));
		try {
			return Integer.parseInt(JsonNodeUtils.asString(node));
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * gets the combined information of all the paths.
	 * 
	 * @param path
	 * @return
	 */
	public MultiLiteral getMultiLiteralValue(String... path) {
		return getMultiLiteralValue(true, path);
	}

	public MultiLiteral getMultiLiteralValue(boolean merge, String... path) {
		MultiLiteral res = null;
		for (String spath : path) {
			List<JsonNode> node = getValues(buildpaths(spath));
			if (Utils.hasInfo(node)) {
				for (JsonNode jsonNode : node) {
					if (Utils.hasInfo(jsonNode)) {
						res = merge(res, JsonNodeUtils.asMultiLiteral(jsonNode, languages));

					}
				}
			}
			if (!merge && Utils.hasInfo(res))
				return res;
		}
		return res;
	}

	/**
	 * gets any of the paths that has information.
	 * 
	 * @param path
	 * @return
	 */
	public Literal getLiteralValue(String... path) {
		for (String spath : path) {
			JsonNode node = getValue(buildpaths(spath));
			if (node != null) {
				Literal res = JsonNodeUtils.asLiteral(node, languages);
				if (Utils.hasInfo(res))
					return res;
			}
		}
		return null;
	}

	/**
	 * gets the combined information of all the paths.
	 * 
	 * @param path
	 * @return
	 */
	public MultiLiteralOrResource getMultiLiteralOrResourceValue(String... path) {
		return getMultiLiteralOrResourceValue(true, path);
	}

	public MultiLiteralOrResource getMultiLiteralOrResourceValue(boolean merge, String... path) {
		MultiLiteralOrResource res = null;
		for (String spath : path) {
			List<JsonNode> node = getValues(buildpaths(spath));
			if (Utils.hasInfo(node)) {
				for (JsonNode jsonNode : node) {
					if (Utils.hasInfo(jsonNode)) {
						res = merge(res, JsonNodeUtils.asMultiLiteralOrResource(jsonNode, languages));
					}
				}
			}
			if (!merge && Utils.hasInfo(res))
				return res;
		}
		return res;
	}

	private <T extends MultiLiteral> T merge(T res, T other) {
		if (!Utils.hasInfo(res))
			res = other;
		else
			res.merge(other);
		return res;
	}

	/**
	 * gets one of the paths with information on it.
	 * 
	 * @param path
	 * @return
	 */
	public LiteralOrResource getLiteralOrResourceValue(String... path) {
		for (String spath : path) {
			JsonNode node = getValue(buildpaths(spath));
			if (node != null) {
				LiteralOrResource res = JsonNodeUtils.asLiteralOrResource(node, languages);
				if (Utils.hasInfo(res))
					return res;
			}
		}
		return null;
	}

	public Resource getResource(String... path) {
		for (String spath : path) {
			String uri = getStringValue(path);
			if (org.apache.commons.lang3.StringUtils.isNotBlank(uri))
				return new Resource(uri);
		}
		return null;
	}

	public List<String> getStringArrayValue(String... path) {
		return getStringArrayValue(true, path);
	}

	public List<String> getStringArrayValue(boolean merge, String... path) {
		Set<String> res = new TreeSet<>();
		for (String spath : path) {
			List<JsonNode> node = getValues(buildpaths(spath));
			if (Utils.hasInfo(node)) {
				for (JsonNode jsonNode : node) {
					if (Utils.hasInfo(jsonNode)) {
						List<String> a = JsonNodeUtils.asStringArray(jsonNode);
						for (String string : a) {
							res.add(string);
						}
					}
				}
			}
			if (!merge && Utils.hasInfo(res))
				break;
		}
		return new ArrayList<String>(res);
	}

	public List<WithDate> getWithDateArrayValue(String... path) {
		return JsonNodeUtils.asWithDateArray(getStringArrayValue(path));
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
		ml.addLiteral(Language.DEFAULT, "http://www.google.com");

		System.out.println(Json.toJson(ml).toString());
		System.out.println(r.getRootInformation().toString());

		System.out.println(r.getValues(r.buildpaths("ages[.*]")));
	}

}
