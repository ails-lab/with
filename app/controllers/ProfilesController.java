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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Option;
import play.mvc.Controller;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordAdmin;
import model.resources.WithResource;
import model.resources.WithResource.WithResourceType;
import model.resources.collection.CollectionObject;
import model.resources.collection.CollectionObject.CollectionAdmin;
import model.resources.collection.Exhibition;
import model.resources.collection.Exhibition.ExhibitionDescriptiveData;
import model.resources.collection.SimpleCollection;

public class ProfilesController extends Controller {
	
	public enum Profile {FULL, BASIC, MEDIUM};
	
	public static CollectionObject getCollectionProfile(String profileString, CollectionObject input) {
		Profile profile = Profile.valueOf(profileString);
		if (profile == null)
			profile = Profile.BASIC;
		if (profile.equals(Profile.FULL))
			return input;
		else if (profile.equals(Profile.BASIC) || profile.equals(Profile.MEDIUM)) {
			if (input.getResourceType().equals(WithResourceType.SimpleCollection)) {
				SimpleCollection output = new SimpleCollection();
				addCommonCollectionFields(input, output);
				return output;
			}
			else if (input.getResourceType().equals(WithResourceType.Exhibition)) {
				Exhibition output = new Exhibition();
				addCommonCollectionFields(input, output);
				ExhibitionDescriptiveData edd = (ExhibitionDescriptiveData) input.getDescriptiveData();
				if (edd.getBackgroundImg() != null && edd.getBackgroundImg().containsKey(MediaVersion.Original))
					edd.getBackgroundImg().remove(MediaVersion.Original);
				output.getDescriptiveData().setBackgroundImg(edd.getBackgroundImg());
				output.getDescriptiveData().setCredits(edd.getCredits());
				output.getDescriptiveData().setIntro(edd.getIntro());
				return output;
			}
			else return input;
		}
		else return input;
	}
	
	public static void addCommonCollectionFields(CollectionObject input, CollectionObject output) {
		output.setAdministrative((CollectionAdmin) input.getAdministrative());
		output.setDbId(input.getDbId());
		output.getDescriptiveData().setLabel(input.getDescriptiveData().getLabel());
		output.getDescriptiveData().setDescription(input.getDescriptiveData().getDescription());
		output.setProvenance(input.getProvenance());
		if (input.getMedia().size() > 3)
			output.setMedia(input.getMedia().subList(0, 2));
		output.setResourceType(input.getResourceType());
		//output.setUsage(input.getUsage());
	}
	
	public static RecordResource getRecordProfile(String profileString, RecordResource input) {
		Profile profile = Profile.valueOf(profileString);
		if (profile == null)
			profile = Profile.BASIC;
		if (profile.equals(Profile.FULL))
			return input;
		else {
			WithResourceType recordType = input.getResourceType();
			try {
				Class<?> clazz = Class.forName("model.resources." + recordType.toString());
				RecordResource output;
				output = (RecordResource) clazz.newInstance();
				if (profile.equals(Profile.BASIC)) { //for thumbnails view
				//if (input.getResourceType().equals(WithResourceType.CulturalObject)) {
					addBasicRecordFields(input, output);
					HashMap<MediaVersion, EmbeddedMediaObject> media = new HashMap<MediaVersion, EmbeddedMediaObject>(1);
					if (input.getMedia() != null && input.getMedia().size() > 0) {
						EmbeddedMediaObject thumbnail = ((HashMap<MediaVersion, EmbeddedMediaObject>) input.getMedia().get(0)).get(MediaVersion.Thumbnail);
						if (thumbnail != null) {
							media.put(MediaVersion.Thumbnail, thumbnail);
						}
					}
					output.setMedia(new ArrayList<>(Arrays.asList(media)));
					return output;
				//}
				}
				else if (profile.equals(Profile.MEDIUM)) {
					addBasicRecordFields(input, output);
					EmbeddedMediaObject thumbnail = ((HashMap<MediaVersion, EmbeddedMediaObject>) input.getMedia().get(0)).get(MediaVersion.Thumbnail);
					EmbeddedMediaObject original = ((HashMap<MediaVersion, EmbeddedMediaObject>) input.getMedia().get(0)).get(MediaVersion.Original);
					HashMap<MediaVersion, EmbeddedMediaObject> media = new HashMap<MediaVersion, EmbeddedMediaObject>(2);
					media.put(MediaVersion.Thumbnail, thumbnail);
					media.put(MediaVersion.Original, original);
					output.setMedia(new ArrayList<>(Arrays.asList(media)));
					return output;
				}
				else return input;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public static void addBasicRecordFields(RecordResource input, RecordResource output) {
		output.setResourceType(input.getResourceType());
		output.setAdministrative((RecordAdmin) input.getAdministrative());
		output.getDescriptiveData().setLabel(input.getDescriptiveData().getLabel());
		output.getDescriptiveData().setDescription(input.getDescriptiveData().getDescription());
		//add more fields more from descriptive
		output.setDbId(input.getDbId());
		output.setProvenance(input.getProvenance());
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
	        } catch (IllegalArgumentException | IllegalAccessException | NoSuchMethodException | SecurityException e) {
	        	e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
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
