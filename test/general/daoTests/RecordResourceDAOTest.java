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

import java.util.Date;

import org.bson.types.ObjectId;
import org.junit.Test;

import db.DB;
import db.resources.RecordResourceDAO;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.resources.AgentObject;
import model.resources.CulturalObject;
import model.resources.WithResource;
import model.resources.WithResource.WithAdmin;

public class RecordResourceDAOTest {

	@Test
	public void testDAOPolymorphism() {
		RecordResourceDAO<WithResource> dao = new RecordResourceDAO<WithResource>(WithResource.class);
		WithAdmin wa = new WithAdmin();
		wa.setCreated(new Date());
		//wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		waccess.put(new ObjectId(), Access.OWN);
		wa.setAccess(waccess);
		AgentObject ao = new AgentObject();
		ao.setAdministrative(wa);
		dao.makePermanent(ao);
		
		CulturalObject co = new CulturalObject();
		
		co.setAdministrative(wa);
		dao.makePermanent(co);
		
		WithResource ao2 = dao.getById(ao.getDbId());
		System.out.println(ao2.getClass().getSimpleName());
		
		
		
		//set as generic type
		//done
	}
}
