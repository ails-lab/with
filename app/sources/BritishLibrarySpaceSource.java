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


package sources;

import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.WithResource;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.formatreaders.FlickrRecordFormatter;

public class BritishLibrarySpaceSource extends FlickrSpaceSource {
	public BritishLibrarySpaceSource() {
		super(Sources.BritishLibrary.toString(),"SECRET_KEY");
		formatreader = new FlickrRecordFormatter.BritishLibraryRecordFormatter();
	}
	
	@Override
	public SourceResponse getResults(CommonQuery q) {
		if (Utils.hasInfo(q.searchTerm) && q.searchTerm.equals("*"))
			q.searchTerm = "";
		return super.getResults(q);
	}

}
