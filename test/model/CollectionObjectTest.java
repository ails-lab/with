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
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.basicDataTypes.WithDate;
import model.resources.WithResource.ExternalCollection;
import model.resources.WithResource.WithResourceType;
import model.resources.collection.CollectionObject;
import model.resources.collection.CollectionObject.CollectionAdmin;
import model.resources.collection.CollectionObject.CollectionDescriptiveData;
import model.resources.collection.CollectionObject.CollectionAdmin.CollectionType;
import model.usersAndGroups.User;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import play.libs.Json;
import db.DB;

public class CollectionObjectTest {

	@Test
	public void modelCollection() {

		//RecordResource<RecordDescriptiveData> co = new RecordResource<RecordResource.RecordDescriptiveData>();
		CollectionObject co = new CollectionObject();

		/*
		 * Owner of the CollectionObject
		 *
		 */
		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u == null) {
			System.out.println("No user found");
			return;
		}

		/*
		 * Administative metadata
		 */
		co.getAdministrative().setCreated(new Date());
		((CollectionAdmin)co.getAdministrative()).setCollectionType(CollectionType.SimpleCollection);
		//wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		waccess.setIsPublic(true);
		waccess.addToAcl(new AccessEntry(u.getDbId(), Access.OWN));
		co.getAdministrative().setWithCreator(u.getDbId());
		co.getAdministrative().setAccess(waccess);

		//no externalCollections
		List<ExternalCollection> ec;

		//no provenance
		List<ProvenanceInfo> prov;

		//resourceType is collectionObject
		//co.setResourceType(WithResourceType.CollectionObject);
		// type: metadata specific for a collection
		MultiLiteral label = new MultiLiteral(Language.EN,"MyTitle");
		//RecordDescriptiveData cdd = new RecordDescriptiveData();
		CollectionDescriptiveData cdd = new CollectionDescriptiveData();
		cdd.setLabel(label);
		MultiLiteral desc = new MultiLiteral(Language.EN, "This is a description");
		cdd.setDescription(desc);
		LiteralOrResource metaRights = new LiteralOrResource("CCO");
		cdd.setMetadataRights(metaRights);

		WithDate date = new WithDate();
		date.setYear(1998);
		List<WithDate> dates = new ArrayList<WithDate>();
		dates.add(date);
		WithDate date1 = new WithDate();
		date1.setYear(2004);
		dates.add(date1);
		cdd.setDates(dates);


		co.setDescriptiveData(cdd);

		/*
		 * More information
		 */
		co.setResourceType(WithResourceType.CollectionObject);


		/*
		 * no content for the collection
		 */
		Map<String, String> content;

		/*
		 * media thumbnail for collection
		 */
		EmbeddedMediaObject emo = new EmbeddedMediaObject();
		co.addMedia(MediaVersion.Original, emo);
		//if (DB.getRecordResourceDAO().makePermanent(co) == null) { System.out.println("No storage!"); return; }
		if (DB.getCollectionObjectDAO().makePermanent(co) == null) { System.out.println("No storage!"); return; }
		CollectionObject co1 = DB.getCollectionObjectDAO().get(co.getDbId());
		if((co1 != null))
			System.out.println("Stored!");
		System.out.println(Json.toJson(co));
		System.out.println(co.transformCO());
		//RecordResource rr1 =  DB.getRecordResourceDAO().getById(co.getDbId());
		//if(DB.getCollectionObjectDAO().makeTransient(co) != -1 ) System.out.println("Deleted");

		/*JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
		ObjectNode json = new ObjectNode(nodeFactory);
		ObjectNode labelJson = nodeFactory.objectNode();
		ObjectNode titleJson = nodeFactory.objectNode();
		labelJson.put(Language.EN.toString(), "New Title");
		titleJson.put("label", labelJson);
		json.put("descriptiveData", titleJson);
		titleJson.put("isExhibition", true);
		json.put("administrative", titleJson);
		for (CollectionObject c: DB.getCollectionObjectDAO().getByLabel("en", "MyTitle")) {
			System.out.println(Json.toJson(c));
			ObjectId colId = c.getDbId();
			DB.getCollectionObjectDAO().editCollection(colId, json);
		}*/

	}


	private MediaObject getMediaObject() {

		MediaObject mo = new MediaObject();
		byte[] rawbytes = null;
		URL url = null;
		try {
			url = new URL("http://www.ntua.gr/schools/ece.jpg");
			File file = new File("test_java.txt");
			ImageInputStream iis = ImageIO.createImageInputStream(file);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

			if (readers.hasNext()) {

                // pick the first available ImageReader
                ImageReader reader = readers.next();

                // attach source to the reader
                reader.setInput(iis, true);

                // read metadata of first image
                IIOMetadata metadata = reader.getImageMetadata(0);

                String[] names = metadata.getMetadataFormatNames();
                int length = names.length;
                for (int i = 0; i < length; i++) {
                    System.out.println( "Format name: " + names[ i ] );
                }
            }

			FileUtils.copyURLToFile(url, file);
			FileInputStream fileStream = new FileInputStream(
					file);

			rawbytes = IOUtils.toByteArray(fileStream);
		} catch(Exception e) {
			System.out.println(e);
			System.exit(-1);
		}

		mo.setMediaBytes(rawbytes);
		//mo.setMimeType(MediaType.ANY_IMAGE_TYPE);
		mo.setHeight(875);
		mo.setWidth(1230);
		LiteralOrResource lor = new LiteralOrResource(Language.EN, url.toString());
		mo.setOriginalRights(lor);
		HashSet<WithMediaRights> set = new HashSet<EmbeddedMediaObject.WithMediaRights>();
		set.add(WithMediaRights.Creative);
		//mo.setWithRights(set);
		mo.setType(WithMediaType.IMAGE);
		mo.setUrl(url.toString());

		try {
			DB.getMediaObjectDAO().makePermanent(mo);
			System.out.println("Media succesfully saved!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mo;
	}

}
