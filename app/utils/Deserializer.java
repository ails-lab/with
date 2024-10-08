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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.time.DateUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.net.MediaType;

import db.DB;
import model.Campaign.BadgePrizes;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataType;
import model.annotations.selectors.ImageSVGSelector;
import model.annotations.selectors.PropertySelector;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.selectors.SelectorType;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

public class Deserializer {
	public static final ALogger log = Logger.of(Deserializer.class);

	public static class WithAccessDeserializer extends JsonDeserializer<WithAccess> {

		@Override
		public WithAccess deserialize(JsonParser accessString, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
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
					if (entry.get("user").isValueNode() && entry.get("level").isValueNode()) {
						String username = ((TextNode) entry.get("user")).asText();
						List<User> usersRetrieved = DB.getUserDAO().getByFieldAndValue("username", username,
								new ArrayList<String>(Arrays.asList("_id")));
						User user = null;
						if (usersRetrieved.size() > 0)
							user = usersRetrieved.get(0);

						if (user != null) {
							ObjectId userId = user.getDbId();
							String acc = ((TextNode) entry.get("level")).asText();
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

	public static class ContextDataDeserializer extends JsonDeserializer<List<ContextData>> {

		@Override
		public List<ContextData> deserialize(JsonParser contextString, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			TreeNode contextDataJson = contextString.readValueAsTree();
			List<ContextData> contextDataList = new ArrayList<ContextData>();
			if ((contextDataJson != null) && contextDataJson.isArray()) {
				for (int i = 0; i < contextDataJson.size(); i++) {
					TreeNode c = contextDataJson.get(i);
					TreeNode contextDataTypeNode = c.get("contextDataType");
					ContextData contextData = new ContextData();
					if ((contextDataTypeNode != null) && contextDataTypeNode.isValueNode()) {
						String contextDataTypeString = ((TextNode) contextDataTypeNode).asText();
						ContextDataType contextDataType = null;
						if ((contextDataType = ContextDataType.valueOf(contextDataTypeString)) != null) {
							Class<?> clazz;
							try {
								clazz = Class.forName("model.annotations." + contextDataTypeString);
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

	public static class MultiLiteralDesiarilizer extends JsonDeserializer<MultiLiteral> {

		@Override
		public MultiLiteral deserialize(JsonParser string, DeserializationContext arg1) {
			MultiLiteral out = new MultiLiteral();
			Map<String, String[]> map;
			try {
				map = string.readValueAs(new TypeReference<Map<String, String[]>>() {
				});
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String[]> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addMultiLiteral(Language.getLanguageByCode(e.getKey()), Arrays.asList(e.getValue()));
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
		@Override
		public ObjectId deserialize(JsonParser string, DeserializationContext arg1) {
			try {
				return new ObjectId(string.getValueAsString());
			} catch (IOException e) {
				return null;
			}
		}
	}

	public static class MultiLiteralOrResourceDesiarilizer extends JsonDeserializer<MultiLiteralOrResource> {

		@Override
		public MultiLiteralOrResource deserialize(JsonParser string, DeserializationContext arg1) {
			// Map<String, String[]> map = new HashMap<String, String[]>>();
			MultiLiteralOrResource out = new MultiLiteralOrResource();
			Map<String, String[]> map;
			try {
				map = string.readValueAs(new TypeReference<Map<String, String[]>>() {
				});
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String[]> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addMultiLiteral(Language.getLanguageByCode(e.getKey()), Arrays.asList(e.getValue()));
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
		public Literal deserialize(JsonParser string, DeserializationContext arg1) {
			Map<String, String> map;
			Literal out = new Literal();
			try {
				map = string.readValueAs(new TypeReference<Map<String, String>>() {
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
					out.addLiteral(Language.getLanguageByCode(e.getKey()), e.getValue());
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class LiteralEnglishDefaultDesiarilizer extends JsonDeserializer<Literal> {

		@Override
		public Literal deserialize(JsonParser string, DeserializationContext arg1) {
			Map<String, String> map;
			Literal out = new Literal();
			try {
				map = string.readValueAs(new TypeReference<Map<String, String>>() {
				});
			} catch (JsonProcessingException e1) {
				try {
					out.addLiteral(Language.EN, string.getText());
					return out;
				} catch (Exception e2) {
					return null;
				}
			} catch (IOException e1) {
				return null;
			}
			for (Entry<String, String> e : map.entrySet()) {
				if (Language.isLanguage(e.getKey())) {
					out.addLiteral(Language.getLanguageByCode(e.getKey()), e.getValue());
				}
			}
			return out;
		}
	}
	
	public static class BadgePrizesDeserializer extends JsonDeserializer<BadgePrizes> {

		@Override
		public BadgePrizes deserialize(JsonParser jp, DeserializationContext ctxt) {
			BadgePrizes badgePrizes = new BadgePrizes();
			try {
				badgePrizes = jp.readValueAs(BadgePrizes.class);
			} catch (Exception e) {
				return null;
			}
			return badgePrizes;
		}
		
	}

	public static class LiteralOrResourceDesiarilizer extends JsonDeserializer<LiteralOrResource> {

		@Override
		public LiteralOrResource deserialize(JsonParser string, DeserializationContext arg1) {
			Map<String, String> map;
			LiteralOrResource out = new LiteralOrResource();
			try {
				map = string.readValueAs(new TypeReference<Map<String, String>>() {
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
					out.addLiteral(Language.getLanguageByCode(e.getKey()), e.getValue());
				} else if (e.getKey().equals(LiteralOrResource.URI)) {
					out.addURI(e.getValue());
				}
			}
			out.fillDEF();
			return out;
		}
	}

	public static class AccessMapDeserializer extends JsonDeserializer<Map<ObjectId, Access>> {

		@Override
		public Map<ObjectId, Access> deserialize(JsonParser rights_string, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			Map<String, Integer> rights_map = rights_string.readValueAs(new TypeReference<Map<String, Integer>>() {
			});
			Map<ObjectId, Access> r = new HashMap<ObjectId, Access>();
			for (Entry<String, Integer> e : rights_map.entrySet()) {
				r.put(new ObjectId(e.getKey()), Access.values()[e.getValue()]);
			}
			return r;
		}
	}

	public static class MimeTypeDeserializer extends JsonDeserializer<MediaType> {

		@Override
		public MediaType deserialize(JsonParser mimetype_string, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			return MediaType.parse(mimetype_string.readValueAs(String.class));
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
				log.error("", e);
				return null;
			}
		}
	}

	public static class DateExtendedDeserializer extends JsonDeserializer<Date> {

		@Override
		public Date deserialize(JsonParser date, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			try {
				Date d = DateUtils.parseDateStrictly(date.getValueAsString(), "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'",
						"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd", "yyyy/MM/dd");
				return d;
			} catch (ParseException e) {
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
				map = string.readValueAs(new TypeReference<Map<String, Double>>() {
				});
				point = GeoJson.point(map.get("latitude"), map.get("longitude"));
			} catch (Exception e) {
				return null;
			}
			return point;
		}
	}

	public static class SelectorTypeDeserializer extends JsonDeserializer<SelectorType> {

		@Override
		public SelectorType deserialize(JsonParser jp, DeserializationContext arg1) {
			SelectorType st = null;
			try {
				Map<String, Object> map = jp.readValueAs(new TypeReference<Map<String, Object>>() {
				});

				Object property = map.get("property");
				Object imageURL = map.get("imageWithURL");

				if (property != null) {

					Object start = map.get("start");
					Object end = map.get("end");
					Object origValue = map.get("origValue");
					Object origLang = map.get("origLang");
					Object prefix = map.get("prefix");
					Object suffix = map.get("suffix");
					Object annotatedValue = map.get("annotatedValue");

					if (start != null || end != null || origValue != null || origLang != null || prefix != null || suffix != null || annotatedValue != null) {
						st = new PropertyTextFragmentSelector();
						((PropertyTextFragmentSelector) st).setProperty((String) property);
						((PropertyTextFragmentSelector) st).setStart((Integer) start);
						((PropertyTextFragmentSelector) st).setEnd((Integer) end);
						((PropertyTextFragmentSelector) st).setOrigValue((String) origValue);
						((PropertyTextFragmentSelector) st).setOrigLang(Language.getLanguageByCode((String) origLang));
						((PropertyTextFragmentSelector) st).setPrefix((String) prefix);
						((PropertyTextFragmentSelector) st).setSuffix((String) suffix);
						((PropertyTextFragmentSelector) st).setAnnotatedValue((String) annotatedValue);

					} else {
						st = new PropertySelector();
						((PropertySelector) st).setProperty((String) property);
					}
				} else if (imageURL != null) {
					st = new ImageSVGSelector();
					((ImageSVGSelector) st).setImageWithURL((String) imageURL);
					((ImageSVGSelector) st).setFormat((String) map.get("format"));
					((ImageSVGSelector) st).setText((String) map.get("text"));
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return st;
		}
	}

}
