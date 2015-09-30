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


package general;

import java.util.List;

import model.Collection;

import org.junit.Test;

import db.DB;

public class MongoScripts {

	@Test
	public void updateCollectionRights() {

		long colCount = DB.getCollectionDAO().count();
		for(int i = 0;i < (colCount/50); i++) {
			List<Collection> cols = DB.getCollectionDAO().getAll(i*50, (i*50)+50);
			for(Collection col: cols) {
				DB.getCollectionDAO().makePermanent(col);
			}
		}
		List<Collection> last = DB.getCollectionDAO().getAll(((int)colCount%50)+2, (int)colCount);
	}
}
