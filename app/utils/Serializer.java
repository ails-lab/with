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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import model.Collection;
import model.RecordLink;
import model.User;
import play.Logger;
import play.Logger.ALogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import db.DB;

public class Serializer {
	public static final ALogger log = Logger.of( Serializer.class);

	private final JsonNode json;
	private final Class clazz;

	public Serializer(JsonNode json, Class clazz) {
		this.json = json;
		this.clazz = clazz;
	}

	public Object jsonToObject() {
		return this.jsonToObject(clazz);
	}

	private Object jsonToObject(Class clazz)  {
		try {
			String methodName = "jsonTo" + clazz.getSimpleName() + "Object";
			Method serializer = getClass().getDeclaredMethod(methodName);
			return serializer.invoke(this, new Object());
		} catch(IllegalAccessException | InvocationTargetException e) {
			log.error("Illegal access to method!");
		} catch(IllegalArgumentException e) {
			log.error("Wrong arguments specified to method invocation!");
		} catch(Exception e) {
			log.error("Cannot invoke serialization method!");
		}
		return null;
	}

	private Collection jsonToCollectionObject() {
		Collection collection = new Collection();

		collection.setDescription(json.get("description").toString());
		collection.setTitle(json.get("title").toString());
		String isPublic = json.get("public").toString();
		if(isPublic.equals("true"))
			collection.setPublic(true);
		else
			collection.setPublic(false);

		String ownerMail = json.get("ownerMail").toString();
		User owner = DB.getUserDAO().getByEmail(ownerMail);
		collection.setOwner(owner);

		ArrayNode firstEntriesIds = (ArrayNode)json.get("firstEntries");
		ArrayList<RecordLink> firstEntries = new ArrayList<RecordLink>();
		for(JsonNode idNode: firstEntriesIds) {
			String id = idNode.get("id").toString();
			RecordLink rlink = DB.getRecordLinkDAO().getByDbId(id);
			firstEntries.add(rlink);
		}
		collection.setFirstEntries(firstEntries);

		if(collection == null)
			log.debug("Null collection! No database storage!");
		return collection;
	}
}
