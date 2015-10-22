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

import espace.core.FormatReader;
import espace.core.Utils;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.utils.JsonContextRecord;
import espace.core.utils.JsonNodeUtils;

public class DPLASearchFormatReader extends JsonContextRecordFormatReader {

	@Override
	public Object buildObjectFrom() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object fillObjectFrom(JsonContextRecord r, Object object) {
		// TODO Auto-generated method stub
		ItemsResponse it = new ItemsResponse();
		System.out.println(r.getValue("f.size"));
		System.out.println(r.getValue("ages[1]"));
		it.id = r.getStringValue("id");
		it.thumb = r.getStringArrayValue("object");
		it.fullresolution = null;
		it.title = r.getStringValue("sourceResource.title");
		it.description = r.getStringValue("sourceResource.description");
		it.creator = r.getStringValue("sourceResource.creator");
		it.year = null;
		it.dataProvider = r.getStringValue("dataProvider.name");
		/*it.provider = Utils.readAttr(item.path("provider"), "name", false);*/
		it.url = new MyURL();
		it.url.original = r.getStringArrayValue("isShownAt");
		it.url.fromSourceAPI = "http://dp.la/item/" + it.id;
		it.rights = r.getStringValue("sourceResource.rights");
		it.externalId = it.url.original.get(0);
		it.externalId = DigestUtils.md5Hex(it.externalId);
		return null;
	}

}
