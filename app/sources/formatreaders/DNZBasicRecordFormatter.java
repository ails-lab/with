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

public class DNZBasicRecordFormatter extends CulturalRecordFormatter {

	public DNZBasicRecordFormatter(FilterValuesMap map) {
		super(map);
		object = new CulturalObject();
	}

	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = (CulturalObjectData) object.getDescriptiveData();
		model.setLabel(rec.getMultiLiteralValue("title"));
		model.setDescription(rec.getMultiLiteralValue("description"));
		// model.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		// model.setIsShownAt(rec.getStringValue("edmIsShownAt"));
		// model.setYear(Integer.parseInt(rec.getStringValue("year")));
		model.setDccreator(rec.getMultiLiteralOrResourceValue(rec.getStringValue("creator")));

		object.addToProvenance(new ProvenanceInfo(rec.getStringValue("dataProvider")));
		object.addToProvenance(
				new ProvenanceInfo(rec.getStringValue("provider.name"), null, rec.getStringValue("provider.@id")));
		String id = rec.getStringValue("id");
		object.addToProvenance(
				new ProvenanceInfo(Sources.DigitalNZ.toString(), "http://www.digitalnz.org/objects/" + id, id));
		EmbeddedMediaObject medThumb = new EmbeddedMediaObject();
		medThumb.setUrl(rec.getStringValue("thumbnail_url"));
		object.addMedia(MediaVersion.Thumbnail, medThumb);
		EmbeddedMediaObject medFullSize = new EmbeddedMediaObject();
		medFullSize.setUrl(rec.getStringValue("medFullSize"));
		// TODO: add rights
		// medFullSize.setWithRights(...);
		object.addMedia(MediaVersion.Original, medThumb);
		return object;
		// //TODO: add type
		// //TODO: add null checks
		// object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
		// // TODO: add years here:
		//// Utils.readArrayAttr(item, "issued",
		// // true);
		//// object.setYears(ListUtils.transform(rec.getStringArrayValue("year"),
		// (String y)->{return Year.parse(y);}));
		// // TODO: add rights
		//// object.setItemRights(rec.getStringValue("rights_url"));
	}

}
