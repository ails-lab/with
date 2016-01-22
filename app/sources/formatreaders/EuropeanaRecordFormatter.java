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

import java.util.List;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.Provider.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.core.CommonFilters;
import sources.utils.JsonContextRecord;
import sources.utils.StringUtils;
import utils.ListUtils;

public class EuropeanaRecordFormatter extends CulturalRecordFormatter {

	public EuropeanaRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		
		
		model.setDcspatial(rec.getMultiLiteralOrResourceValue("dctermsSpatial"));
		
		model.setLabel(rec.getMultiLiteralValue("dcTitleLangAware"));
		model.setDescription(rec.getMultiLiteralValue("dcDescriptionLangAware"));
		model.setIsShownBy(rec.getLiteralOrResourceValue("edmIsShownBy"));
		model.setIsShownAt(rec.getLiteralOrResourceValue("edmIsShownAt"));
		model.setDates(rec.getWithDateArrayValue("year"));
		model.setDccreator(rec.getMultiLiteralOrResourceValue("dcCreatorLangAware"));
		model.setKeywords(rec.getMultiLiteralOrResourceValue("dcSubjectLangAware"));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider"), model.getIsShownAt().getURI(),null));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider")));
		String recID = rec.getStringValue("id");
		String uri = "http://www.europeana.eu/portal/record"+recID+".html";
		object.addToProvenance(
				new ProvenanceInfo(Sources.Europeana.toString(), uri, recID));
		List<String> rights = rec.getStringArrayValue("rights");
		WithMediaType type = (WithMediaType) getValuesMap().translateToCommon(CommonFilters.TYPE.name(), rec.getStringValue("type")).get(0);
		WithMediaRights withRights = (rights==null || rights.size()==0)?null:(WithMediaRights) getValuesMap().translateToCommon(CommonFilters.RIGHTS.name(), rights.get(0)).get(0);
		String uri3 = rec.getStringValue("edmPreview");
		if (uri3!=null){
			EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
			medThumb.setUrl(uri3);
			medThumb.setType(type);
			medThumb.setParentID(model.getIsShownBy().getURI());
			medThumb.setOriginalRights(new LiteralOrResource(rights.get(0)));
			medThumb.setWithRights(withRights);
			object.addMedia(MediaVersion.Thumbnail, medThumb);
		}
		String uri2 = model.getIsShownBy()==null?null:model.getIsShownBy().getURI();
		if (uri2!=null){
			EmbeddedMediaObject med = new EmbeddedMediaObject();
			med.setParentID("self");
			med.setUrl(uri2);
			med.setOriginalRights(new LiteralOrResource(rights.get(0)));
			med.setWithRights(withRights);
			med.setType(type);
			object.addMedia(MediaVersion.Original, med);
		}
		return object;
	}

}
