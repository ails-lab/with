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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
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
	
	public static CollectionObject getCollectionProfile(Profile profile, CollectionObject input) {
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
		output.setMedia(input.getMedia());
		output.setResourceType(input.getResourceType());
		//output.setUsage(input.getUsage());
	}
	
	public static RecordResource getRecordProfile(Profile profile, RecordResource input) {
		if (profile.equals(Profile.FULL))
			return input;
		else {
			RecordResource output = new RecordResource();
			if (profile.equals(Profile.BASIC)) { //for thumbnails view
			//if (input.getResourceType().equals(WithResourceType.CulturalObject)) {
				addBasicRecordFields(input, output);
				EmbeddedMediaObject thumbnail = ((HashMap<MediaVersion, EmbeddedMediaObject>) input.getMedia().get(0)).get(MediaVersion.Thumbnail);
				HashMap<MediaVersion, EmbeddedMediaObject> media = new HashMap<MediaVersion, EmbeddedMediaObject>(1);
				media.put(MediaVersion.Thumbnail, thumbnail);
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
	
	public static String getLocale(String lang) {
		String locale = Language.DEFAULT.toString();
		if (lang == null) {
			String sessionLocale = session().get("locale");
			if (sessionLocale != null)
				locale = sessionLocale;
		}
		else {
			if (lang.equals("ALL"))
				locale = lang;
			else {
				Language language = Language.valueOf(lang);
				if (language!= null)
					locale = language.toString();
				else {
					
				}
			}
		}
		return locale;
	}
	
	public void filterResourceByLocale(String locale, WithResource resource) {
		Field[] fields = resource.getClass().getDeclaredFields();
		for (Field field : fields) {
	        Class<?> type = field.getType();
	        try {
		        if (type.equals(MultiLiteral.class) || type.equals(MultiLiteralOrResource.class) || type.equals(Literal.class) || type.equals(LiteralOrResource.class)) {
		        	HashMap<String, Object> hm = (HashMap<String, Object>) field.get(resource);
		        	HashMap<String, Object> filteredHm = filterHashMapByLocale(hm, locale);
		        	if (!filteredHm.isEmpty())
		        	field.set(resource, filteredHm);
		        } 
	        } catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}	
	    }
	}
	
	private static HashMap<String, Object> filterHashMapByLocale(HashMap<String, Object> hm, String locale) {
    	HashMap<String, Object> filteredHm = new HashMap<String, Object>(1);
    	if (hm.containsKey(locale)) 
        	filteredHm.put(locale, hm.get(locale));
    	if (hm.containsKey("URI"))
    		hm.put("URI", hm.get("URI"));
    	return hm;
	}
	
	
}
