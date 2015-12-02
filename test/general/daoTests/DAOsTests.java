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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.Json;
import db.DB;
import db.RecordResourceDAO;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.basicDataTypes.Literal;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.WithAccess.Access;
import model.resources.AgentObject;
import model.resources.CollectionObject;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResource;
import model.resources.CollectionObject.CollectionDescriptiveData;
import model.resources.WithResource.ExternalCollection;
import model.resources.WithResource.WithAdmin;
import model.resources.WithResource.WithResourceType;
import model.usersAndGroups.User;

public class DAOsTests {

	@Test
	public void testCommonResourceDAO() {

	}

	@Test
	public void testRecordResourceDAOPolymorphism() {

		RecordResource<RecordDescriptiveData> wres = new RecordResource<RecordDescriptiveData>();

		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u == null) {
			System.out.println("No user found");
			return;
		}

		/*
		 * Administative metadata
		 */
		WithAdmin wa = new WithAdmin();
		wa.setCreated(new Date());
		wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		waccess.put(u.getDbId(), Access.OWN);
		wa.setAccess(waccess);
		wres.setAdministrative(wa);

		/*
		 * This models a record so there's no need to provide this
		 */
		HashMap<ObjectId, ArrayList<Integer>> colIn =
				new HashMap<ObjectId, ArrayList<Integer>>();
		ArrayList<Integer> cols = new ArrayList<Integer>();
		cols.add(4);
		cols.add(87);
		cols.add(33);
		colIn.put(new ObjectId(), cols);
		//wres.setCollectedIn(colIn);

		//no externalCollections
		List<ExternalCollection> ec;

		//no provenance
		ProvenanceInfo pinfo = new ProvenanceInfo();
		pinfo.setProvider("Europeana");
		pinfo.setRecordId("18898");
		pinfo.setUri("http://the.uri.org/666");
		ArrayList<ProvenanceInfo> prov = new ArrayList<ProvenanceInfo>();
		prov.add(pinfo);
		//wres.setProvenance(prov);

		//resourceType is collectionObject
		wres.setResourceType(WithResourceType.WithResource);
		// type: metadata specific for a record
		Literal label = new Literal(Language.EN, "My record Title");
		RecordDescriptiveData ddata = new RecordDescriptiveData();
		ddata.setLabel(label);
		Literal desc = new Literal(Language.EN, "This is a description");
		ddata.setDescription(desc);
		wres.setDescriptiveData(ddata);


		/*
		 * no content for the collection
		 */
		Map<String, String> content;

		/*
		 * media thumbnail for collection
		 */
		ArrayList<EmbeddedMediaObject> medias = new ArrayList<EmbeddedMediaObject>();
		EmbeddedMediaObject emo = new EmbeddedMediaObject();
		medias.add(emo);
		wres.setMedia(medias);

		if(DB.getRecordResourceDAO().makePermanent(wres) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored!");
		if(wres.getDbId() != null) System.out.println("The first CollectionObject presenting a collection was saved!");


		List<RecordResource> co2 = DB.getRecordResourceDAO().getByLabel("EN", "My record Title");
		System.out.println("Retrieved by label: \n" + Json.toJson(co2) );

		List<RecordResource> co3 = DB.getRecordResourceDAO().getByOwner(u.getDbId(), 0, 10);
		System.out.println("Retrieved by Owner: \n" + Json.toJson(co3) );

	}
}
