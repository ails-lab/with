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
import espace.core.sources.DDBSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;

public class DDBBasicRecordFormatter extends ExternalBasicRecordReader {

	@Override
	public void fillInExternalId(JsonContextRecord rec) {
		object.setIsShownBy(null);
		object.setIsShownAt(object.getIsShownBy());
	}

	@Override
	public void fillInValidRecord(JsonContextRecord rec) {
		object.setThumbnailUrl("https://www.deutsche-digitale-bibliothek.de/"+rec.getStringValue("thumbnail"));
		object.setTitle(rec.getStringValue("title"));
		object.setDescription(rec.getStringValue("description"));
		object.setCreator(rec.getStringValue("principalOrFirstMaker"));
//		object.setContributors(rec.getStringArrayValue("contributor"));
		// TODO: add years
//		object.setYear(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
		// TODO: add rights
//		object.setItemRights(rec.getStringValue("rights"));
		String id = rec.getStringValue("id");
		Provider recordProvider = new Provider(DDBSpaceSource.LABEL, id, 
				"https://www.deutsche-digitale-bibliothek.de/item/"
						+ id );
		object.addProvider(recordProvider);
	}

}
