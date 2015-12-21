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

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.codec.digest.DigestUtils;
import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.EuropeanaSpaceSource;
import espace.core.sources.NLASpaceSource;
import espace.core.sources.RijksmuseumSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.EmbeddedMediaObject;
import model.ExternalBasicRecord;
import model.MediaObject;
import model.Provider;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import utils.ListUtils;

public class RijksmuseumRecordFormatter extends JsonContextRecordFormatReader<CulturalObject> {
	
	public RijksmuseumRecordFormatter() {
		object = new CulturalObject();
	}



	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
		model.setLabel(rec.getLiteralValue("title"));
		model.setDescription(rec.getLiteralValue("longTitle"));
		model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");
//		model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(Arrays.asList(new LiteralOrResource(rec.getStringValue("principalOrFirstMaker"))));
		
//		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
//		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("provider")));
		String id = rec.getStringValue("objectNumber");
		object.addToProvenance(new ProvenanceInfo(RijksmuseumSpaceSource.LABEL, id, 
				"https://www.rijksmuseum.nl/en/search/objecten?q=dance&p=1&ps=12&ii=0#/"
				+ id + ",0"));
		ArrayList<EmbeddedMediaObject> media= new ArrayList<>();
		MediaObject med;
		media.add(med = new MediaObject());
		object.setMedia(media);
		med.setThumbnailUrl(rec.getStringValue("webImage.url"));
		med.setWidth(Integer.parseInt(rec.getStringValue("webImage.width")));
		med.setHeight(Integer.parseInt(rec.getStringValue("webImage.height")));
//		med.setUrl(rec.getStringValue("edmIsShownBy"));
		return object;
////		record.setContributors(rec.getStringArrayValue("contributor"));
//		// TODO: add years
//		object.setYears(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
//		// TODO: add rights
////		record.setItemRights(rec.getStringValue("rights"));
	}

}
