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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithDate;

public class JsonNodeUtils {

	public static String asString(JsonNode node) {
		if (node != null && !node.isMissingNode()) {
			if (node.isArray()) {
				JsonNode jsonNode = node.get(0);
				if (jsonNode != null)
					return jsonNode.asText();
			} else
				return node.asText();
		}
		return null;
	}
	public static MultiLiteral readMultiLiteral(MultiLiteral res, JsonNode node) {
		if (node != null && !node.isMissingNode()) {
			if (node.isArray()) {
				node = node.get(0);
			}
			if (node.isTextual()) {
				res.addLiteral(Language.DEF, node.asText());
				return res;
			} 
			if (node.isArray()){
				for (int i = 0; i < node.size(); i++) {
					res.addLiteral(Language.DEF, node.get(i).asText());
				}
				return res;
			}
			for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> next = iterator.next();
				Language language = Language.getLanguage(next.getKey());
				JsonNode value = next.getValue();
				for (int i = 0; i < value.size(); i++) {
					res.addLiteral(language, value.get(i).asText());
				}
			}
			res.fillDEF();
			return res;
		}
		return null;
	}

	public static MultiLiteral asMultiLiteral(JsonNode node) {
		return readMultiLiteral(new MultiLiteral(), node);
	}
	public static Literal readLiteral(Literal res, JsonNode node) {
		if (node != null && !node.isMissingNode()) {
			if (node.isArray()) {
				node = node.get(0);
			}
			if (node.isTextual()) {
				res.addLiteral(Language.DEF, node.asText());
				return res;
			}
			if (node.isArray()){
				res.addLiteral(Language.DEF, node.get(0).asText());
				return res;
			}
			for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> next = iterator.next();
				Language language = Language.getLanguage(next.getKey());
				JsonNode value = next.getValue();
				res.addLiteral(language, value.get(0).asText());
			}
			res.fillDEF();
			return res;
		}
		return null;
	}
	
	public static Literal asLiteral(JsonNode node) {
		return readLiteral(new Literal(), node);
	}
	public static LiteralOrResource asLiteralOrResource(JsonNode node) {
		return (LiteralOrResource) readLiteral(new LiteralOrResource(), node);
	}

	public static MultiLiteralOrResource asMultiLiteralOrResource(JsonNode node) {
		return (MultiLiteralOrResource) readMultiLiteral(new MultiLiteralOrResource(), node);
	}
	
	public static List<WithDate> asWithDateArray(JsonNode node) {
		if (node != null && !node.isMissingNode()) {
			ArrayList<WithDate> res = new ArrayList<>();
			if (node.isArray()) {
				for (int i = 0; i < node.size(); i++) {
					res.add(new WithDate(node.get(i).asText()));
				}
			} else if (node.isTextual())  {
				res.add(new WithDate(node.asText()));
				return res;
			} 
			for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> next = iterator.next();
				JsonNode value = next.getValue();
				for (int i = 0; i < value.size(); i++) {
					res.add(new WithDate(value.get(i).asText()));
				}
			}
			return res;
		}
		return new ArrayList<>();
	}

	public static List<String> asStringArray(JsonNode node) {
		if (node != null && !node.isMissingNode()) {
			ArrayList<String> res = new ArrayList<>();
			if (node.isArray()) {
				for (int i = 0; i < node.size(); i++) {
					res.add(node.get(i).asText());
				}
			} else {
				res.add(node.asText());
			}
			return res;
		}
		return null;
	}

}
