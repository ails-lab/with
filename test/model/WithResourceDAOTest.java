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
import java.util.List;

import model.DescriptiveData;
import model.basicDataTypes.Literal;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.WithResource;
import static org.fest.assertions.Assertions.assertThat;

import org.bson.types.ObjectId;
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
			DescriptiveData model = new RecordResource.RecordDescriptiveData(new Literal(Language.EN, "TestWithResource" + i));
			model.setDescription(new Literal(Language.EN, "Some description"));
			withResource.setModel(model);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
		}*/
		
		for (int i = 0; i < 1; i++) {
			System.out.println(i);
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			//withResource.addToProvenance(new ProvenanceInfo("provider0"));
			WithAccess access = new WithAccess();
			//access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
			Literal label  = new Literal();
			label.setLiteral(Language.EN, "TestWithResourceDD" + i);
			model.setLabel(label);
			Literal description = new Literal();
			description.setLiteral(Language.EN, "Some description");
			model.setDescription(description);
			withResource.setModel(model);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
			List<RecordResource> resources = DB.getRecordResourceDAO().getByLabel(label);
			System.out.println(resources);
			/*System.out.println(resources.get(0) instanceof CulturalObject);
			System.out.println(resources.get(1) instanceof CulturalObject);*/
		}
	}

}
