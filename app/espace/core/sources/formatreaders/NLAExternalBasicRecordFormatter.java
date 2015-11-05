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
import espace.core.sources.NLASpaceSource;
import espace.core.utils.JsonContextRecord;
import espace.core.utils.StringUtils;
import model.ExternalBasicRecord;
import model.Provider;

public class NLAExternalBasicRecordFormatter extends ExternalBasicRecordReader {

	@Override
	public void fillInExternalId(JsonContextRecord rec) {
		object.setIsShownBy(null);
		object.setIsShownAt(rec.getStringValue("identifier[type=url,linktype=fulltext|restricted|unknown].value"));
	}

	@Override
	public void fillInValidRecord(JsonContextRecord rec) {
		object.setThumbnailUrl(rec.getStringValue("identifier[type=url,linktype=thumbnail].value"));
		object.setTitle(rec.getStringValue("title"));
		object.setDescription(rec.getStringValue("abstract"));
		object.setCreator(rec.getStringValue("contributor"));
		// object.setContributors(rec.getStringArrayValue("contributor"));
		// TODO: add years
		object.setYears(StringUtils.getYears(rec.getStringArrayValue("issued")));
		// TODO: add rights
		// object.setItemRights(rec.getStringValue("sourceResource.rights"));
		String id = rec.getStringValue("id");
		Provider recordProvider = new Provider(NLASpaceSource.LABEL, id, rec.getStringValue("troveUrl"));
		object.addProvider(recordProvider);
		
	}

}
