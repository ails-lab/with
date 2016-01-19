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

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.Provider.Sources;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import sources.FilterValuesMap;
import sources.utils.JsonContextRecord;

public class DPLARecordFormatter extends CulturalRecordFormatter {

	public DPLARecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
		model.setLabel(rec.getLiteralValue("sourceResource.title"));
		model.setDescription(rec.getLiteralValue("sourceResource.description"));
		model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		model.setMetadataRights(LiteralOrResource.build("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType("http://www.europeana.eu/schemas/edm/ProvidedCHO");
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(rec.getLiteralOrResourceValue("sourceResource.creator"));

		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
		object.addToProvenance(
				new ProvenanceInfo(rec.getStringValue("provider.name"), null, rec.getStringValue("provider.@id")));
		String id = rec.getStringValue("id");
		object.addToProvenance(new ProvenanceInfo(Sources.DPLA.toString(), "http://dp.la/item/" + id, id));
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		medThumb.setUrl(rec.getStringValue("object"));
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		// TODO: add rights!
		EmbeddedMediaObject med = new EmbeddedMediaObject();
		med.setUrl(rec.getStringValue("edmIsShownBy"));
		object.addMedia(MediaVersion.Original, med);
		return object;
		// //TODO: add type
		// //TODO: add null checks
		// object.setDescription(rec.getStringValue("sour
		// ceResource.description"));
		// object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
		// // TODO: add years
		//// object.setYears(ListUtils.transform(rec.getStringArrayValue("year"),
		// (String y)->{return Year.parse(y);}));
		// // TODO: add rights
		//// object.setItemRights(rec.getStringValue("sourceResource.rights"));
	}

}
