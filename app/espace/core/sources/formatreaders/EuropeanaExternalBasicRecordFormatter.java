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

import org.apache.commons.codec.digest.DigestUtils;

import espace.core.ExternalBasicRecordReader;
import espace.core.JsonContextRecordFormatReader;
import espace.core.sources.EuropeanaSpaceSource;
import espace.core.utils.JsonContextRecord;
import model.ExternalBasicRecord;
import model.Provider;
import utils.ListUtils;

public class EuropeanaExternalBasicRecordFormatter extends ExternalBasicRecordReader {
	
	@Override
	public void fillInExternalId(JsonContextRecord rec) {
		object.setIsShownBy(rec.getStringValue("edmIsShownBy"));
		object.setIsShownAt(rec.getStringValue("edmIsShownAt"));
	}

	@Override
	public void fillInValidRecord(JsonContextRecord rec) {
		object.addProvider(new Provider(rec.getStringValue("dataProvider")));
		object.addProvider(new Provider(rec.getStringValue("provider")));
		//TODO: add null checks
		object.setThumbnailUrl(rec.getStringValue("edmPreview"));
		object.setTitle(rec.getStringValue("title"));
		object.setDescription(rec.getStringValue("dcDescription"));
		object.setCreator(rec.getStringValue("dcCreator"));
		object.setContributors(rec.getStringArrayValue("dcContributor"));
		object.setYears(ListUtils.transform(rec.getStringArrayValue("year"), (String y)->{return Year.parse(y);}));
//		object.setItemRights(rec.getStringValue("rights"));
		Provider recordProvider = new Provider(EuropeanaSpaceSource.LABEL, rec.getStringValue("id"), rec.getStringValue("guid"));
		object.addProvider(recordProvider);
		
	}

}
