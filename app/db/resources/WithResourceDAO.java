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


package db.resources;
import model.resources.RecordResource_;
import model.resources.RecordResource;
import play.Logger;
import play.Logger.ALogger;

public class WithResourceDAO extends CommonResourcesDAO<RecordResource_> {
	public static final ALogger log = Logger.of(RecordResource.class);

	public WithResourceDAO() {
		super(RecordResource.class);
	}
}
