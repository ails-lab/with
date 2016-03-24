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


package sources.formatreaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.WithDate;
import model.resources.AgentObject;
import model.resources.AgentObject.AgentData;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import play.Logger;
import sources.FilterValuesMap;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;

public class DBPediaAgentRecordFormatter extends AgentRecordFormatter {

	public DBPediaAgentRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new AgentObject();
	}

	@Override

	public AgentObject fillObjectFrom(JsonContextRecord rec) {
//		Language[] language = new Language[] {Language.EN };
//		
//		language = getLanguagesFromText(rec.getStringValue("title"), 
//				rec.getStringValue("longTitle"));
//		rec.setLanguages(language);

		AgentData model = (AgentData) object.getDescriptiveData();
		
//		model.setDclanguage(StringUtils.getLiteralLanguages(language));
		
		model.setLabel(rec.getMultiLiteralValue("label"));
		model.setDescription(rec.getMultiLiteralValue("abstract"));
//		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
//		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
//		model.setDccreator(rec.getMultiLiteralOrResourceValue("principalOrFirstMaker"));
		
		String id = rec.getStringValue("uri");
		object.addToProvenance(new ProvenanceInfo(Sources.DBPedia.toString(), id , id));
		
		List<String> subject =  rec.getStringArrayValue("subject");
		if (subject != null && subject.size() > 0) {
			MultiLiteralOrResource ct = new MultiLiteralOrResource();
			ct.addURI(subject);
			model.setKeywords(ct);
		}
		

		String uri3 = rec.getStringValue("thumbnail");
		if (Utils.hasInfo(uri3)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setUrl(uri3);
//			medThumb.setWidth(rec.getIntValue("headerImage.width"));
//			medThumb.setHeight(rec.getIntValue("headerImage.height"));
//			medThumb.setType(type);
//			if (Utils.hasInfo(rights))
			med.setOriginalRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/deed.en"));
			med.setWithRights(WithMediaRights.Public);
			
			object.addMedia(MediaVersion.Thumbnail, med);
		}
		
		String uri2 = rec.getStringValue("depiction");
		if (Utils.hasInfo(uri2)){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setUrl(uri2);
//			medThumb.setWidth(rec.getIntValue("headerImage.width"));
//			medThumb.setHeight(rec.getIntValue("headerImage.height"));
//			medThumb.setType(type);
//			if (Utils.hasInfo(rights))
			med.setOriginalRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/deed.en"));
			med.setWithRights(WithMediaRights.Public);
			
			object.addMedia(MediaVersion.Original, med);
		}
		
		List<String> birthplaces =  rec.getStringArrayValue("birthplace");
		if (birthplaces != null && birthplaces.size() > 0) {
			MultiLiteralOrResource bp = new MultiLiteralOrResource();
			bp.addURI(birthplaces);
			model.setBirthPlace(bp);
		}
		
		String birthdate =  rec.getStringValue("birthdate");
		if (birthdate != null) {
			List<WithDate> wd = new ArrayList<WithDate>();
			wd.add(new WithDate(birthdate));
			
			model.setBirthDate(wd);
		}
		
		String deathdate =  rec.getStringValue("deathdate");
		if (deathdate != null) {
			List<WithDate> wd = new ArrayList<WithDate>();
			wd.add(new WithDate(deathdate));
			
			model.setBirthDate(wd);
		}
		
		return object;
	}

}
