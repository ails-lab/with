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


package espace.core.sources.formatreaders;

import java.util.ArrayList;
import java.util.Arrays;
import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.EuropeanaSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.EmbeddedMediaObject;
import model.MediaObject;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;


public class EuropeanaRecordFormatter extends JsonContextRecordFormatReader<CulturalObject> {
	
	public EuropeanaRecordFormatter() {
		object = new CulturalObject();
	}
	
	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
		model.setLabel(rec.getLiteralValue("dcTitleLangAware"));
		model.setDescription(rec.getLiteralValue("dcDescriptionLangAware"));
		model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");
//		model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(Arrays.asList(new LiteralOrResource(rec.getStringValue("dcCreatorLangAware"))));
		
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider")));
		object.addToProvenance(new ProvenanceInfo(EuropeanaSpaceSource.LABEL, rec.getStringValue("id"), rec.getStringValue("guid")));
		ArrayList<EmbeddedMediaObject> media= new ArrayList<>();
		MediaObject med;
		media.add(med = new MediaObject());
		object.setMedia(media);
		med.setThumbnailUrl(rec.getStringValue("edmIsShownBy"));
		med.setUrl(rec.getStringValue("edmIsShownBy"));
		return object;
		//TODO: add null checks
//		object.setThumbnailUrl(rec.getStringValue("edmPreview"));
//		object.setContributors(rec.getStringArrayValue("dcContributor"));
//		object.setItemRights(rec.getStringValue("rights"));
	}
	
}
