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


package controllers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Session;

import model.DescriptiveData;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.resources.WithResource;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;

import db.DB;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.mvc.Controller;

public abstract class WithController extends Controller {
	
	public enum Profile {FULL, BASIC, MEDIUM};
	
	public static final ALogger log = Logger.of(WithController.class);

	public static enum Action {
		READ, EDIT, DELETE
	};

	public static boolean isSuperUser() {
		if (effectiveUserIds().isEmpty())
			return false;
		String userId = effectiveUserIds().get(0);
		return (DB.getUserDAO().isSuperUser(new ObjectId(userId)));
	}
	
	public static User effectiveUser() {
		ObjectId userId = effectiveUserDbId();
		if (userId != null)
			return DB.getUserDAO().get(userId);
		else
			return null;
	}

	public static String loggedInUser() {
		return session().get("user");
	}

	public static boolean hasAccessToRecordResource(
			Action action, ObjectId resourceId) {
		return DB.getRecordResourceDAO().hasAccess(effectiveUserDbIds(),
				action, resourceId);
	}

	public static boolean hasAccessToCollectionResource(
			Action action, ObjectId resourceId) {
		return DB.getCollectionObjectDAO().hasAccess(
				effectiveUserDbIds(), action, resourceId);
	}

	public static List<ObjectId> toObjectIds(List<String> userIds) {
		List<ObjectId> objectIds = new ArrayList<ObjectId>();
		for (String userId : userIds) {
			objectIds.add(new ObjectId(userId));
		}
		return objectIds;
	}

	public static boolean checkAccessRecursively(Map<ObjectId, Access> rights,
			ObjectId groupId) {
		return false;
	}

	public static Access getMaxAccess(WithAccess rights, List<String> userIds) {
		Access maxAccess = Access.NONE;
		for (String id : userIds) {
			User user = DB.getUserDAO().getById(new ObjectId(id),
					new ArrayList<String>(Arrays.asList("superUser")));
			if (user != null) {
			  if (user.isSuperUser())
				  return Access.OWN;
			  else if (rights.containsUser(new ObjectId(id))) {
					Access access = rights.getAcl(new ObjectId(id));
					if (access.ordinal() > maxAccess.ordinal())
						maxAccess = access;
			  }
			}
		}
		return maxAccess;
	}

	/*
	 * public static boolean increasedAccess(Access before, Access after) { if
	 * (before == null) { if (after == null) { return false; } else { return
	 * true; } } return (after.ordinal() > before.ordinal()); }
	 */

	/**
	 * This methods supposes we have all user ids and all userGroup ids
	 * (recursively obtained) for the user, in a comma-separated list. It then
	 * transforms the comma-separated in java.util.List
	 * 
	 * @param effectiveUserIds
	 * @return
	 */
	public static List<String> effectiveUserIds() {
		String effectiveUserIds = session().get("effectiveUserIds");
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		List<String> userIds = new ArrayList<String>();
		for (String ui : effectiveUserIds.split(",")) {
			if (ui.trim().length() > 0)
				userIds.add(ui);
		}
		return userIds;
	}

	public static List<ObjectId> effectiveUserDbIds() {
		String effectiveUserIds = session().get("effectiveUserIds");
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		List<ObjectId> userIds = new ArrayList<ObjectId>();
		for (String ui : effectiveUserIds.split(",")) {
			if (ui.trim().length() > 0)
				userIds.add(new ObjectId(ui.trim()));
		}
		return userIds;
	}

	public static String effectiveUserId() {
		String effectiveUserIds = session().get("effectiveUserIds");
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		String[] ids = effectiveUserIds.split(",");
		return ids[0];
	}

	public static ObjectId effectiveUserDbId() {
		String effectiveUserIds = session().get("effectiveUserIds");
		if (effectiveUserIds == null || effectiveUserIds.isEmpty())
			return null;
		String[] ids = effectiveUserIds.split(",");
		return new ObjectId(ids[0]);
	}
	
	
	public static String getLocale(Option<String> lang) {
		String locale = Language.DEFAULT.toString();
		if (!lang.isDefined()) {
			String sessionLocale = session().get("locale");
			if (sessionLocale != null)
				locale = sessionLocale;
		}
		else {
			if (lang.get().equals("ALL"))
				locale = lang.get();
			else {
				Language language = Language.valueOf(lang.get());
				if (language != null)
					locale = language.toString();
			}
		}
		return locale;
	}
	
	public static void filterResourceByLocale(Option<String> locale, WithResource resource) {
		String localeString = getLocale(locale);
		//assume only descriptiveData has literal type of fields
		DescriptiveData descriptiveData = resource.getDescriptiveData();
		List<Field> directFields = new ArrayList<Field>();
		getOneLevelInclParentFields(descriptiveData.getClass(), directFields);
		List<Field> literalFields = new ArrayList<Field>();
		getLiteralFields(directFields, literalFields);
		for (Field field : literalFields) {
			try {
		    	String methodName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
		    	Method method = descriptiveData.getClass().getMethod(methodName);
		    	Object value = method.invoke(descriptiveData);
		    	if (value != null) {
		        	HashMap<String, Object> hm = (HashMap<String, Object>) value;
		        	if (!hm.isEmpty()) {
			        	Class<?> type = value.getClass();
			        	HashMap<String, Object> filteredHm = filterHashMapByLocale(hm, localeString.toLowerCase());
			        	if (!filteredHm.isEmpty()) {
				        	methodName = "s" + methodName.substring(1);
				        	method = descriptiveData.getClass().getMethod(methodName, type);
					        if (type.equals(MultiLiteral.class) || type.equals(MultiLiteralOrResource.class)) {
					        	MultiLiteral fhm = new MultiLiteral();
					        	for (String k: filteredHm.keySet())
					        		fhm.put(k, (List<String>) filteredHm.get(k));
									method.invoke(descriptiveData, fhm);
					        }
					        else if (type.equals(Literal.class) || type.equals(LiteralOrResource.class)) {
					        	method.invoke(descriptiveData, (value.getClass().cast(filteredHm)));
					        }
			        	}
		        	}
		    	}
			}
			catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error in accessing or invoking a method of WithResource via reflection.", e);
			}
	    }
	}
	
	private static void getOneLevelInclParentFields(Class clazz, List<Field> fields) {
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null && !clazz.getSimpleName().equals("model.resources.WithResource")) {
            getOneLevelInclParentFields(clazz.getSuperclass(), fields);
        }
	}
	
	private static void getLiteralFields(List<Field> allFields, List<Field> literalFields) {
        for (Field cf: allFields) {
        	Class<?> type = cf.getType();
            if (!type.isEnum()) {
		        if (type.equals(MultiLiteral.class) || type.equals(MultiLiteralOrResource.class) || type.equals(Literal.class) || type.equals(LiteralOrResource.class)) {
		        	literalFields.add(cf);
		        }
		        else
		        	getLiteralFields(Arrays.asList(type.getDeclaredFields()), literalFields);
	        }
		}
    }
	
	private static HashMap<String, Object> filterHashMapByLocale(HashMap<String, Object> hm, String locale) {
    	HashMap<String, Object> filteredHm = new HashMap<String, Object>();
    	if (hm.containsKey(locale)) 
        	filteredHm.put(locale, hm.get(locale));
    	//URI entries are always returned
    	if (hm.containsKey("URI"))
    		filteredHm.put("URI", hm.get("URI"));
    	return filteredHm;
	}

}
