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


package db;

import model.Media;
import model.RecordLink;
import play.Logger;

public class RecordLinkDAO extends DAO<RecordLink> {
	static private final Logger.ALogger log = Logger.of(RecordLink.class);

	public RecordLinkDAO() {
		super( RecordLink.class );
	}

	public RecordLink getByDbId(String dbId) {
		return this.get(dbId);
	}

	public String getTitle(String dbId) {
		return this.get(dbId).getTitle();
	}

	public String getDescription(String dbId) {
		return this.get(dbId).getDescription();
	}

	public String getSource(String dbId) {
		return this.get(dbId).getSource();
	}

	public String getSourceId(String dbId) {
		return this.get(dbId).getSourceId();
	}

	public String getSourceUrl(String dbId) {
		return this.get(dbId).getSourceUrl();
	}

	public String getThumbnailUrl(String dbId) {
		return this.get(dbId).getThumbnailUrl();
	}

	public void blabla(Media media) {

	}
}
