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

import org.apache.commons.codec.digest.DigestUtils;

import espace.core.ExternalBasicRecordReader;
import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.DPLASpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;

public class DPLAExternalBasicRecordFormatter extends ExternalBasicRecordReader<CulturalObject> {


	@Override
	public CulturalObject fillObjectFrom(JsonContextRecord rec) {
		CulturalObjectData model = new CulturalObjectData();
		object.setDescriptiveData(model);
//		object.addProvider(new Provider(rec.getStringValue("dataProvider")));
//		object.addProvider(new Provider(rec.getStringValue("provider.name"),rec.getStringValue("provider.@id"),rec.getStringValue("provider.@id")));
//		//TODO: add type
//		//TODO: add null checks
//		object.setThumbnailUrl(rec.getStringValue("objtect"));
//		object.setTitle(rec.getStringValue("sourceResource.title"));
//		object.setDescription(rec.getStringValue("sourceResource.description"));
//		object.setCreator(rec.getStringValue("sourceResource.creator"));
//		object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
//		// TODO: add years
////		object.setYears(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
//		// TODO: add rights
////		object.setItemRights(rec.getStringValue("sourceResource.rights"));
//		String id = rec.getStringValue("id");
//		Provider recordProvider = new Provider(DPLASpaceSource.LABEL, id, 
//				"http://dp.la/item/"+id);
//		object.addProvider(recordProvider);
		return object;
	}

}
