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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.Quality;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.LiteralOrResource.ResourceType;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CollectionObject;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.CollectionObject.CollectionDescriptiveData;
import static org.fest.assertions.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.common.net.MediaType;

import db.DB;

public class WithResourceDAOTest {
	
	ObjectId dbidForParent;
	
	private EmbeddedMediaObject createImage(int i, int j) {
		EmbeddedMediaObject image = new EmbeddedMediaObject();
		
		//embedded
		image.setType(WithMediaType.IMAGE);
		Set<WithMediaRights> set = new HashSet<WithMediaRights>();
		set.add(WithMediaRights.Public);
		image.setWithRights(set);
		if(j==0){
			image.setMediaVersion(MediaVersion.Original);
			image.setHeight(599);
			image.setWidth(755);
		} else if (j==1){
			image.setMediaVersion(MediaVersion.Medium);
			image.setHeight(599);
			image.setWidth(755);
		} else if (j==2) {
			image.setMediaVersion(MediaVersion.Tiny);
			image.setHeight(599);
			image.setWidth(755);
		} else if (j==3) {
			image.setMediaVersion(MediaVersion.Thumbnail);
			image.setHeight(599);
			image.setWidth(755);

		} else {
			image.setMediaVersion(MediaVersion.Square);
			image.setHeight(599);
			image.setWidth(755);
		}
		//image.setThumbHeight(120);
		//image.setThumbWidth(100);
		image.setUrl("http://www.ntua.gr/ntua-0"+i+".jpg");
		//image.setThumbnailUrl("http://www.ntua.gr/ntua-01.jpg");
		LiteralOrResource lor = new LiteralOrResource();
		lor.setResource(ResourceType.dbpedia, "<http://pt.dbpedia.org/resource/Brasil>");
		image.setOriginalRights(lor);
		image.setMimeType(MediaType.parse("image/jpeg"));
		image.setSize(10000000);
		image.setQuality(Quality.IMAGE_SMALL);
		
		//extended
		//image.setMediaBytes(rawbytes);
		//image.setThumbnailBytes(rawbytes);
		//image.setOrientation(); //auto
		//set the rest!
		
		
		try {
			DB.getMediaObjectDAO().makePermanent((MediaObject) image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertThat( image.getDbId()).isNotNull();
		if (j==0){
			image.setParentID(image.getDbId());
			dbidForParent = image.getDbId();
		} else{
			image.setParentID(dbidForParent);
		}
		return image;
	}
	
	
	@Test
	public void storeMediaResource() {

		CulturalObject withResource = new CulturalObject();
		
		EmbeddedMediaObject image10 = createImage(1, 0);
		EmbeddedMediaObject image11 = createImage(1, 1);
		EmbeddedMediaObject image12 = createImage(1, 2);
		EmbeddedMediaObject image13 = createImage(1, 3);
		EmbeddedMediaObject image14 = createImage(1, 4);
		EmbeddedMediaObject image20 = createImage(2, 0);
		EmbeddedMediaObject image21 = createImage(2, 1);
		EmbeddedMediaObject image22 = createImage(2, 2);
		EmbeddedMediaObject image23 = createImage(2, 3);
		EmbeddedMediaObject image24 = createImage(2, 4);
		
		HashMap<MediaVersion, EmbeddedMediaObject> object1 = new HashMap<MediaVersion, EmbeddedMediaObject>();
		HashMap<MediaVersion, EmbeddedMediaObject> object2 = new HashMap<MediaVersion, EmbeddedMediaObject>();
		
		object1.put(image10.getMediaVersion(), image10);
		object1.put(image11.getMediaVersion(), image11);
		object1.put(image12.getMediaVersion(), image12);
		object1.put(image13.getMediaVersion(), image13);
		object1.put(image14.getMediaVersion(), image14);
		
		object2.put(image20.getMediaVersion(), image20);
		object2.put(image21.getMediaVersion(), image21);
		object2.put(image22.getMediaVersion(), image22);
		object2.put(image23.getMediaVersion(), image23);
		object2.put(image24.getMediaVersion(), image24);

		
		List<HashMap<MediaVersion, EmbeddedMediaObject>> media = new ArrayList<HashMap<MediaVersion, EmbeddedMediaObject>>();
		
		media.set(0, object1);
		media.set(1, object2);
		
		withResource.setMedia(media);
		
		withResource.addToProvenance(new ProvenanceInfo("provider0", "http://myUri", "12345"));
		withResource.addToProvenance(new ProvenanceInfo("provider1", "http://myUri", "12345"));
		withResource.addToProvenance(new ProvenanceInfo("ΜintTest", "http://myUri", "12345"));
		WithAccess access = new WithAccess();
		access.put(new ObjectId(), WithAccess.Access.READ);
		access.setPublic(true);
		withResource.getAdministrative().setAccess(access);
		withResource.getAdministrative().setCreated(new Date());
		withResource.getAdministrative().setLastModified(new Date());
		RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
		model.setLabel(new MultiLiteral(Language.EN, "TestWithResourceNew"));
		model.setDescription(new MultiLiteral(Language.EN, "Some description"));
		withResource.setDescriptiveData(model);

		//withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1+j);
		assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
		
		
		
		

		/*List<RecordResource> cos = DB.getRecordResourceDAO().getByCollectionBtwPositions(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0, 2);
		for (RecordResource co: cos) {
			System.out.println(Json.toJson(co));
		}*/
		//DB.getRecordResourceDAO().shiftRecordsToLeft(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1);
		System.out.println(DB.getWithResourceDAO().getByLabel(Language.ENG, "TestWithResourceNew0").size());
	}

	
	@Test
	public void storeWithResource() {

		for (int i = 0; i < 1; i++) {
			CulturalObject withResource = new CulturalObject();
			withResource.getUsage().setLikes(i);
			withResource.addToProvenance(new ProvenanceInfo("provider0", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("provider1", "http://myUri", "12345"));
			withResource.addToProvenance(new ProvenanceInfo("ΜintTest", "http://myUri", "12345"));
			WithAccess access = new WithAccess();
			access.put(new ObjectId(), WithAccess.Access.READ);
			access.setPublic(true);
			withResource.getAdministrative().setAccess(access);
			withResource.getAdministrative().setCreated(new Date());
			withResource.getAdministrative().setLastModified(new Date());
			RecordResource.RecordDescriptiveData model = new RecordResource.RecordDescriptiveData();
			model.setLabel(new MultiLiteral(Language.EN, "TestWithResourceNew" + i));
			model.setDescription(new MultiLiteral(Language.EN, "Some description"));
			withResource.setDescriptiveData(model);

			int j=0;
			if (i==0) j=i; else j=i+1;
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 0+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb80"), 1+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0+j);
			withResource.addPositionToCollectedIn(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1+j);
			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
			
			/*CollectionObject co = new CollectionObject();
			co.getAdministrative().setCreated(new Date());
			co.getAdministrative().setLastModified(new Date());
			Literal label = new Literal(Language.EN, "TestWithResourceNew" + i);
			CollectionObject.CollectionDescriptiveData cdd = new CollectionDescriptiveData();
			cdd.setLabel(label);
			Literal desc = new Literal(Language.EN, "This is a description");
			cdd.setDescription(desc);
			co.setDescriptiveData(cdd);
			co.addToProvenance(new ProvenanceInfo("ΜintTest", "http://myUri", "12345"));
			assertThat(DB.getCollectionObjectDAO().makePermanent(co)).isNotEqualTo(null);*/
			//CulturalObject o = (CulturalObject) resources.get(0);
			//System.out.println(o.getProvenance().get(0).getProvider() + " " + o.getProvenance().get(0).getUri());
			//System.out.println(Json.toJson(o));
			//assertThat(o instanceof CulturalObject);
/*			assertThat(DB.getRecordResourceDAO().makePermanent(withResource)).isNotEqualTo(null);
			if(DB.getRecordResourceDAO().removeFromCollection(withResource.getDbId(), new ObjectId("5666bea55cf494714b7a71c6"), 3))
				System.out.println("Position removed correclty!");
			//DB.getRecordResourceDAO().shiftRecordsToLeft(new ObjectId("5656dd6ce4b0b19378e1cb80"), 2);
			//List<RecordResource> resources = DB.getRecordResourceDAO().getByLabel(Language.EN, "TestWithResourceZ" + i);
			//CulturalObject o = (CulturalObject) resources.get(0);
			//System.out.println(o.getProvenance().get(0).getProvider() + " " + o.getProvenance().get(0).getUri());
			//System.out.println(Json.toJson(o));

*/
		}
		/*List<RecordResource> cos = DB.getRecordResourceDAO().getByCollectionBtwPositions(new ObjectId("5656dd6ce4b0b19378e1cb81"), 0, 2);
		for (RecordResource co: cos) {
			System.out.println(Json.toJson(co));
		}*/
		//DB.getRecordResourceDAO().shiftRecordsToLeft(new ObjectId("5656dd6ce4b0b19378e1cb81"), 1);
		System.out.println(DB.getWithResourceDAO().getByLabel(Language.ENG, "TestWithResourceNew0").size());
	}

}
