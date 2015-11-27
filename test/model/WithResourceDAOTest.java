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


package model;
import java.util.Date;

import model.DescriptiveData;
import model.basicDataTypes.Literal;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.WithResource;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import db.DB;

public class WithResourceDAOTest {
	
	@Test
	public void storeWithResource() {

		/*for (int i = 0; i < 5; i++) {
			RecordResource withResource = new RecordResource();
			withResource.setUsage(new WithResource.Usage());
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0"));
			WithAccess access = new WithAccess();
			//access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			DescriptiveData description = new RecordResource.RecordDescriptiveData(new Literal(Language.EN, "TestWebResource" + i));
			description.setDescription(new Literal(Language.EN, "Some description"));
			//CulturalObject c = new CulturalObject();
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
		}*/
		
		for (int i = 0; i < 5; i++) {

			// a user creates a new collection
			CulturalObject withResource = new CulturalObject();
			withResource.setUsage(new WithResource.Usage());
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0"));
			WithAccess access = new WithAccess();
			//access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			DescriptiveData description = new RecordResource.RecordDescriptiveData(new Literal(Language.EN, "TestWebResource" + i));
			description.setDescription(new Literal(Language.EN, "Some description"));
			/*CulturalObject c = new CulturalObject();*/
			assertThat(DB.getCulturalObjectDAO().makePermanent(withResource)).isNotEqualTo(null);
		}
	}

}
