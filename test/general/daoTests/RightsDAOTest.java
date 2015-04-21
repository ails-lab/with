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


package general.daoTests;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Date;

import model.CollectionRecord;
import model.Rights;
import model.Rights.Access;

import org.bson.types.ObjectId;
import org.junit.Test;

import db.DB;

public class RightsDAOTest {


	@Test
	public void storeRights() {

		String[] objIds = { "55229980e4b0d981287be04e", "55229981e4b0d981287be052",
							"55229981e4b0d981287be056", "55229981e4b0d981287be05a" };

		String[] ownerIds = { "55227fc7e4b0d981285d5812", "55227fc7e4b0d981285d5804",
							"55227fc7e4b0d981285d584a", "55227fc7e4b0d981285d582a" };

		String[] recIds  = { "55227fc7e4b0d981285d580e", "55227fc8e4b0d981285d5a16",
							"55227fc7e4b0d981285d5829", "55227fc7e4b0d981285d5916" };

		Rights r = null;
		for(int i=0;i<500;i++) {
			r = new Rights();
			r.setAccess(Access.values()[i%4]);
			r.setCreated(new Date());

			CollectionRecord entry = DB.getCollectionRecordDAO().get(new ObjectId(objIds[i%4]));
			r.setCollectionName(entry.getCollection().getTitle());
			r.setObjectId(entry.getDbId());
			r.setOwnerId(DB.getUserDAO().getById(new ObjectId(ownerIds[i%4])).getDbId());
			r.setReceiverId(DB.getUserDAO().getById(new ObjectId(recIds[i%4])).getDbId());

			DB.getRightsDAO().makePermanent(r);
			assertThat(r.getDbId()).isNotNull();
		}
	}
}
