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
import espace.core.sources.DigitalNZSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;

public class DNZBasicRecordFormatter extends ExternalBasicRecordReader {

	
	@Override
	public void fillInExternalId(JsonContextRecord rec) {
		object.setIsShownBy(rec.getStringValue("large_thumbnail_url"));
		object.setIsShownAt(rec.getStringValue("landing_url"));
	}

	@Override
	public void fillInValidRecord(JsonContextRecord rec) {
		object.addProvider(new Provider(rec.getStringValue("dataProvider")));
		object.addProvider(new Provider(rec.getStringValue("provider.name"),rec.getStringValue("provider.@id"),rec.getStringValue("provider.@id")));
		//TODO: add type
		//TODO: add null checks
		object.setThumbnailUrl(rec.getStringValue("thumbnail_url"));
		object.setTitle(rec.getStringValue("title"));
		object.setDescription(rec.getStringValue("description"));
		object.setCreator(rec.getStringValue("creator"));
		object.setContributors(rec.getStringArrayValue("sourceResource.contributor"));
		// TODO: add years here:
//		Utils.readArrayAttr(item, "issued",
				// true);
//		object.setYears(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
		// TODO: add rights
//		object.setItemRights(rec.getStringValue("rights_url"));
		String id = rec.getStringValue("id");
		Provider recordProvider = new Provider(DigitalNZSpaceSource.LABEL, id, 
				"http://www.digitalnz.org/objects/" +id);
		object.addProvider(recordProvider);
	}

}
