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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.common.net.MediaType;

import play.libs.Json;
import utils.Tuple;
import db.DB;
import db.RecordResourceDAO;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ContextData.ContextDataType;
import model.annotations.ExhibitionData;
import model.annotations.ExhibitionData.ExhibitionAnnotationBody;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.basicDataTypes.WithDate;
import model.resources.AgentObject;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResource;
import model.resources.WithResource.ExternalCollection;
import model.resources.WithResource.WithAdmin;
import model.resources.WithResource.WithResourceType;
import model.resources.collection.CollectionObject;
import model.resources.collection.CollectionObject.CollectionAdmin;
import model.resources.collection.CollectionObject.CollectionDescriptiveData;
import model.usersAndGroups.User;

public class DAOsTests {

	@Test
	public void testCommonResourceDAO() {

	}

	@Test
	public void testRecordResourceDAOPolymorphism() {

		//RecordResource<RecordDescriptiveData> wres = new RecordResource<RecordDescriptiveData>();
		CollectionObject wres = new CollectionObject();

		User u = DB.getUserDAO().getByUsername("maria.ralli");
		if(u == null) {
			System.out.println("No user found");
			return;
		}

		User u1 = DB.getUserDAO().getByUsername("maria.ralli");
		User u2 = DB.getUserDAO().getByUsername("maria.ralli");

		/*
		 * Administative metadata
		 */
		CollectionAdmin wa = new CollectionObject.CollectionAdmin();
		wa.setCreated(new Date());
		wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		AccessEntry ae1 = new AccessEntry(u.getDbId(), Access.OWN);
		AccessEntry ae2 = new AccessEntry(u1.getDbId(), Access.READ);
		AccessEntry ae3 = new AccessEntry(u2.getDbId(), Access.WRITE);
		waccess.addToAcl(ae1);
		waccess.addToAcl(ae2);
		waccess.addToAcl(ae3);
		wa.setAccess(waccess);
		wres.setAdministrative(wa);

		CollectionAdmin wa2 = new CollectionObject.CollectionAdmin();
		wa2.setCreated(new Date());
		wa2.setWithCreator(u.getDbId());
		WithAccess waccess2 = new WithAccess();
		ae1 = new AccessEntry(u1.getDbId(), Access.OWN);
		ae2 = new AccessEntry(u2.getDbId(), Access.READ);
		ae3 = new AccessEntry(u.getDbId(), Access.WRITE);
		waccess.addToAcl(ae1);
		waccess.addToAcl(ae2);
		waccess.addToAcl(ae3);
		wa.setAccess(waccess2);


		CollectionAdmin wa3 = new CollectionObject.CollectionAdmin();
		wa3.setCreated(new Date());
		wa3.setWithCreator(u.getDbId());
		WithAccess waccess3 = new WithAccess();
		ae1 = new AccessEntry(u2.getDbId(), Access.OWN);
		ae2 = new AccessEntry(u.getDbId(), Access.READ);
		ae3 = new AccessEntry(u1.getDbId(), Access.WRITE);
		waccess.addToAcl(ae1);
		waccess.addToAcl(ae2);
		waccess.addToAcl(ae3);
		wa.setAccess(waccess3);



		/*
		 * This models a record so there's no need to provide this
		 */
		CollectionInfo ci = new CollectionInfo();
		ci.setCollectionId(new ObjectId());
		ci.setPosition(6);
		ArrayList<CollectionInfo> ents = new ArrayList<CollectionInfo>();
		ents.add(ci);


		//no externalCollections
		List<ExternalCollection> ec;

		// a dummy provenance
		ProvenanceInfo pinfo = new ProvenanceInfo();
		pinfo.setProvider("Europeana");
		pinfo.setResourceId("18898");
		pinfo.setUri("http://the.uri.org/666");

		ProvenanceInfo pinfo2 =  new ProvenanceInfo();
		pinfo2.setProvider("Mint");
		pinfo2.setResourceId("6627");
		pinfo2.setUri("http://ming.org/6638");

		List<ProvenanceInfo> prov = new ArrayList<ProvenanceInfo>();
		prov.add(pinfo);
		prov.add(pinfo2);
		wres.setProvenance(prov);

		//resourceType is collectionObject
		wres.setResourceType(WithResourceType.WithResource);
		// type: metadata specific for a record
		MultiLiteral label = new MultiLiteral(Language.EN, "My record Title");
		CollectionDescriptiveData ddata = new CollectionDescriptiveData();
		ddata.setLabel(label);
		MultiLiteral desc = new MultiLiteral(Language.EN, "This is a description");

		ddata.setDescription(desc);

		MultiLiteralOrResource keywords =
				new MultiLiteralOrResource("http://www.uri.org");
				keywords.put("es", new ArrayList<String>(Arrays.asList("Buenos", "Córdoba", "La Plata")));
				keywords.put("en", new ArrayList<String>(Arrays.asList("Buenos", "Cordoba", "Plata")));
		ddata.setKeywords(keywords);


		MultiLiteralOrResource altLabels =
				new MultiLiteralOrResource("http://www.uri.org");
				altLabels.put("en", new ArrayList<String>(Arrays.asList("music", "dance")));
				altLabels.put("fr", new ArrayList<String>(Arrays.asList("musiciens", "dancers")));
		ddata.setAltLabels(altLabels);

		LiteralOrResource lor = new LiteralOrResource();
		lor.put("en", "free rights");
		lor.put("fr", "rights liberales");
		ddata.setMetadataRights(lor);

		WithDate d1 = new WithDate();
		d1.setYear(1984);
		try {
			d1.setIsoDate(new SimpleDateFormat("dd-MM-yyyy").parse("24-11-1967"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		WithDate d2 = new WithDate();
		d2.setYear(1988);
		try {
			d2.setIsoDate(new SimpleDateFormat("dd-MM-yyyy").parse("25-07-1953"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ddata.setDates(new ArrayList<WithDate>(Arrays.asList(d1, d2)));

		wres.setDescriptiveData(ddata);


		/*
		 * no content for the collection
		 */
		Map<String, String> content;

		/*
		 * media thumbnail for collection
		 */
		EmbeddedMediaObject emo = new EmbeddedMediaObject();
		wres.addMedia(MediaVersion.Thumbnail, emo);

		ExhibitionData cd = new ExhibitionData();
		ExhibitionAnnotationBody eab = new ExhibitionAnnotationBody();
		eab.setAudioUrl("http://hello.com");
		eab.setText(new Literal("Text"));
		eab.setVideoDescription("aaa");
		eab.setVideoUrl("http://dasda");
		cd.setBody(eab);
		cd.setContextDataType(ContextDataType.ExhibitionData);
		ContextDataTarget cdt =  new ContextDataTarget();
		cdt.setCollectionId(new ObjectId());
		cdt.setRecordId(new ObjectId());
		cd.setTarget(cdt);
		List<ContextData<ContextDataBody>> list = new ArrayList<ContextData<ContextDataBody>>();
		list.add((ContextData)cd);
		wres.setCollectedResources(list );

		MultiLiteralOrResource dccreator = new MultiLiteralOrResource();
		dccreator.put("es", new ArrayList<String>(Arrays.asList("Buenos", "Córdoba", "La Plata")));
		wres.getDescriptiveData().setDccreator(dccreator);


		if(DB.getCollectionObjectDAO().makePermanent(wres) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored! 1");
		CollectionObject wres2 = wres;
		wres2.setAdministrative(wa2);
		wres2.setDbId(null);
		if(DB.getCollectionObjectDAO().makePermanent(wres2) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored! 2");
		CollectionObject wres3 = wres;
		wres3.setAdministrative(wa3);
		wres3.setDbId(null);
		if(DB.getCollectionObjectDAO().makePermanent(wres3) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored! 3");
		if((wres.getDbId() != null) && (wres2.getDbId() != null)
				&& (wres3.getDbId() != null)) System.out.println("The first CollectionObject presenting a collection was saved!");



		List<CollectionObject> co2 = DB.getCollectionObjectDAO().getByLabel("EN", "My record Title");
		System.out.println("Retrieved by label: \n" + Json.toJson(co2) );

		List<CollectionObject> co3 = DB.getCollectionObjectDAO().getByCreator(u.getDbId(), 0, 10);
		System.out.println("Retrieved by Owner: \n" + Json.toJson(co3) );

		/*List<String> fields = new ArrayList<String>();
		fields.add("descriptiveData.description.EN");
		fields.add("resourceType");
		RecordResource co4 =  DB.getRecordResourceDAO().getById(wres.getDbId(), fields);
		System.out.println("Retrieved some fields by Id: \n" + Json.toJson(co4)
				+ " resourceType: " + co4.getResourceType());*/

		CollectionObject co5 = DB.getCollectionObjectDAO().getByOwnerAndLabel(u.getDbId(),
				"EN", "My record Title");
		System.out.println("Retrieved by owner and label: " + Json.toJson(co5));


		/*List<RecordResource> cos7 = DB.getRecordResourceDAO().getByProvider("Europeana");
		System.out.println("retrieved All Resources provided from Europeana: " + Json.toJson(cos7));*/


		List<List<Tuple<ObjectId, Access>>> access = new ArrayList<List<Tuple<ObjectId,Access>>>();
		List<Tuple<ObjectId, Access>> or1 = new ArrayList<Tuple<ObjectId,Access>>();
		or1.add(new Tuple<ObjectId, WithAccess.Access>(u1.getDbId(), Access.READ));
		List<Tuple<ObjectId, Access>> or2 = new ArrayList<Tuple<ObjectId,Access>>();
		access.add(or1);
		//access.add(or2);
		Tuple<List<CollectionObject>, Tuple<Integer, Integer>> c08 = DB.getCollectionObjectDAO().getByAcl(access, u.getDbId(), false, true, 0, 10);
		System.out.println("Retrieve by ACL " + c08.y.x + " " + c08.y.y + " resources.\n" + Json.toJson(c08.x));


		/*List<RecordResource> co9 =  DB.getRecordResourceDAO()
				.getUsersAccessibleWithACL(loggeInEffIds, accessedByUserOrGroup,
						creator, isExhibition, totalHits, offset, count);*/


		/*List<RecordResource> co10 = DB.getRecordResourceDAO()
				.getSharedWithACL(userId, accessedByUserOrGroup,
						isExhibition, totalHits, offset, count);*/


		/*List<RecordResource> co11 = DB.getRecordResourceDAO()
				.getPublicWithACL(accessedByUserOrGroup, creator,
						isExhibition, totalHits, offset, count);
*/

		System.out.println("Total likes of this Resource are: " +
				DB.getCollectionObjectDAO().getTotalLikes(wres.getDbId()));


		System.out.println("Total Resources from this source are: " +
				DB.getCollectionObjectDAO().countBySource("Europeana"));


		System.out.println("If I add another like then we have:");
		DB.getCollectionObjectDAO().incrementLikes(wres.getDbId());
		System.out.println(DB.getCollectionObjectDAO().getTotalLikes(wres.getDbId()) + " total likes");


		if(DB.getCollectionObjectDAO().removeById(wres.getDbId()) == 1)
			System.out.println("Document deleted succesfully 1");

		if(DB.getCollectionObjectDAO().removeById(wres2.getDbId()) == 1)
			System.out.println("Document deleted succesfully 2");

		if(DB.getCollectionObjectDAO().removeById(wres3.getDbId()) == 1)
			System.out.println("Document deleted succesfully 3");

		//if(DB.getCollectionObjectDAO().removeFromCollection(wres.getDbId(), , ));
	}

	@Test
	public void ensureIndexes() {

		DB.getDs();
	}
}
