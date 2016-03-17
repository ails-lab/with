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


package utils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ContextData.ContextDataType;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;

import play.libs.Json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hp.hpl.jena.tdb.store.IntegerNode;

import db.DB;

public class Deserializer {

	public static class WithAccessDeserializer extends
			JsonDeserializer<WithAccess> {

		@Override
		public WithAccess deserialize(JsonParser accessString,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			WithAccess rights = new WithAccess();
			TreeNode treeNode = accessString.readValueAsTree();
			TreeNode isPublicNode = treeNode.get("isPublic");
			if ((isPublicNode != null) && isPublicNode.isValueNode()) {
				BooleanNode isPublic = (BooleanNode) treeNode.get("isPublic");
				rights.setIsPublic(isPublic.asBoolean());
			}
			TreeNode jsonAcl = treeNode.get("acl");
			if ((jsonAcl != null) && jsonAcl.isArray()) {
				for (int i = 0; i < jsonAcl.size(); i++) {
					TreeNode entry = jsonAcl.get(i);
					if (entry.get("user").isValueNode()
							&& entry.get("level").isValueNode()) {
						String username = ((TextNode) entry.get("user"))
								.asText();
						List<User> usersRetrieved =  DB
								.getUserDAO()
								.getByFieldAndValue(
										"username",
										username,
										new ArrayList<String>(Arrays.asList("_id")));
						User user = null;
						if(usersRetrieved.size() > 0 )
							user = usersRetrieved.get(0);

						if (user != null) {
							ObjectId userId = user.getDbId();
							String acc = ((TextNode) entry.get("level"))
									.asText();
							Access access = Access.valueOf(acc);
							if (access != null)
								rights.addToAcl(userId, access);
							else
								return rights;
						} else
							return rights;
					} else
						return rights;
				}

				// rights.setAcl(acl);
				// if (rightsMap != null)
				// for(Entry<String, Integer> e : rightsMap.entrySet())
				// rights.put(new ObjectId(e.getKey()),
				// Access.values()[e.getValue()]);
			}
			return rights;
		}
	}
	
	public static class ContextDataDeserializer extends
	JsonDeserializer<List<ContextData>> {

	@Override
	public List<ContextData> deserialize(JsonParser contextString,
			DeserializationContext arg1) throws IOException,
			JsonProcessingException {
		TreeNode contextDataJson = contextString.readValueAsTree();
		List<ContextData> contextDataList = new ArrayList<ContextData>();
		if (contextDataJson != null && contextDataJson.isArray()) {
			for (int i = 0; i < contextDataJson.size(); i++) {
				TreeNode c = contextDataJson.get(i);
				TreeNode contextDataTypeNode = c.get("contextDataType");
				ContextData contextData = new ContextData();
				if (contextDataTypeNode != null && contextDataTypeNode.isValueNode()) {
					String contextDataTypeString = ((TextNode) contextDataTypeNode).asText();
					ContextDataType contextDataType = null;
					if ((contextDataType = ContextDataType.valueOf(contextDataTypeString)) != null) {
						Class<?> clazz;
						try {
							clazz = Class.forName("model.annotations."
									+ contextDataTypeString);
							contextData = (ContextData) Json.fromJson((JsonNode) c, clazz);
							contextDataList.add(contextData);
						} catch (ClassNotFoundException e) {
						}
					}
				}
			}
		}
		return contextDataList;
	}
			
	}

	public static class MultiLiteralDesiarilizer extends
			JsonDeserializer<MultiLiteral> {

		@Override
		public MultiLiteral deserialize(JsonParser string,
				DeserializationContext arg1) {
			MultiLiteral out = new MultiLiteral();
			Map<String, String[]> map;
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, String[]>>() {
						});
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String[]> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addMultiLiteral(Language.getLanguage(e.getKey()),
							Arrays.asList(e.getValue()));
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class MultiLiteralOrResourceDesiarilizer extends
			JsonDeserializer<MultiLiteralOrResource> {

		@Override
		public MultiLiteralOrResource deserialize(JsonParser string,
				DeserializationContext arg1) {
			// Map<String, String[]> map = new HashMap<String, String[]>>();
			MultiLiteralOrResource out = new MultiLiteralOrResource();
			Map<String, String[]> map;
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, String[]>>() {
						});
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String[]> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addMultiLiteral(Language.getLanguage(e.getKey()),
							Arrays.asList(e.getValue()));
				} else if (e.getKey().equals(LiteralOrResource.URI)) {
					out.addURI(Arrays.asList(e.getValue()));
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class LiteralDesiarilizer extends JsonDeserializer<Literal> {

		@Override
		public Literal deserialize(JsonParser string,
				DeserializationContext arg1) {
			Map<String, String> map;
			Literal out = new Literal();
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, String>>() {
						});
			} catch (JsonProcessingException e1) {
				try {
					out.addSmartLiteral(string.getText());
					out.fillDEF();
					return out;
				} catch (Exception e2) {
					return null;
				}
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addLiteral(Language.getLanguage(e.getKey()),
							e.getValue());
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class LiteralOrResourceDesiarilizer extends
			JsonDeserializer<LiteralOrResource> {

		@Override
		public LiteralOrResource deserialize(JsonParser string,
				DeserializationContext arg1) {
			Map<String, String> map;
			LiteralOrResource out = new LiteralOrResource();
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, String>>() {
						});
			} catch (JsonProcessingException e1) {
				try {
					out = new LiteralOrResource(string.getText());
					out.fillDEF();
					return out;
				} catch (Exception e2) {
					return null;
				}
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addLiteral(Language.getLanguage(e.getKey()),
							e.getValue());
				} else if (e.getKey().equals(LiteralOrResource.URI)) {
					out.addURI(e.getValue());
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class AccessMapDeserializer extends
			JsonDeserializer<Map<ObjectId, Access>> {

		@Override
		public Map<ObjectId, Access> deserialize(JsonParser rights_string,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			Map<String, Integer> rights_map = rights_string
					.readValueAs(new TypeReference<Map<String, Integer>>() {
					});
			Map<ObjectId, Access> r = new HashMap<ObjectId, Access>();
			for (Entry<String, Integer> e : rights_map.entrySet()) {
				r.put(new ObjectId(e.getKey()), Access.values()[e.getValue()]);
			}
			return r;
		}
	}

	public static class DateDeserializer extends JsonDeserializer<Object> {

		@Override
		public Object deserialize(JsonParser date, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			try {
				return sdf.parse(date.getValueAsString());
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public static class PointDeserializer extends JsonDeserializer<Point> {

		@Override
		public Point deserialize(JsonParser string, DeserializationContext arg1) {
			Map<String, Double> map;
			Point point;
			try {
				map = string
						.readValueAs(new TypeReference<Map<String, Double>>() {
						});
				point = GeoJson
						.point(map.get("latitude"), map.get("longitude"));
			} catch (Exception e) {
				return null;
			}
			return point;
		}
	}
}
