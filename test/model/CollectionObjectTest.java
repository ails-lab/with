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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.Literal;
import model.basicDataTypes.Literal.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.LiteralOrResource.ResourceType;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionDescriptiveData;
import model.resources.WithResource.ExternalCollection;
import model.resources.WithResource.WithAdmin;
import model.resources.WithResource.WithResourceType;
import model.usersAndGroups.User;
import model.MediaObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.common.net.MediaType;

import db.DB;

public class CollectionObjectTest {

	@Test
	public void modelCollection() {

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
		WithAdmin wa = new WithAdmin();
		wa.setCreated(new Date());
		//wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		//waccess.put(u.getDbId(), Access.OWN);
		wa.setAccess(waccess);
		co.setAdministrative(wa);

		/*
		 * This models a collection so there's no need to provide this
		 */
		HashMap<ObjectId, ArrayList<Integer>> colIn =
				new HashMap<ObjectId, ArrayList<Integer>>();
		ArrayList<Integer> cols = new ArrayList<Integer>();
		cols.add(4);
		cols.add(87);
		cols.add(33);
		colIn.put(new ObjectId(), cols);
		co.setCollectedIn(colIn);

		//no externalCollections
		List<ExternalCollection> ec;

		//no provenance
		List<ProvenanceInfo> prov;

		//resourceType is collectionObject
		co.setResourceType(WithResourceType.CollectionObject);
		// type: metadata specific for a collection
		Literal label = new Literal(Language.EN, "MyTitle");
		CollectionObject.CollectionDescriptiveData cdd = new CollectionDescriptiveData();
		cdd.setLabel(label);
		Literal desc = new Literal(Language.EN, "This is a description");
		cdd.setDescription(desc);
		co.setDescriptiveData(cdd);


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
		co.setMedia(medias);

		if(DB.getCollectionObjectDAO().makePermanent(co) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored!");
		if(co.getDbId() != null) System.out.println("The first CollectionObject presenting a collection was saved!");

		//if(DB.getCollectionObjectDAO().makeTransient(co) != -1 ) System.out.println("Deleted");

		System.out.println(DB.getCollectionObjectDAO().getByLabel("en", "MyTitle"));

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
		mo.setMimeType(MediaType.ANY_IMAGE_TYPE);
		mo.setHeight(875);
		mo.setWidth(1230);
		LiteralOrResource lor = new LiteralOrResource(ResourceType.uri, url.toString());
		mo.setOriginalRights(lor);
		HashSet<WithMediaRights> set = new HashSet<EmbeddedMediaObject.WithMediaRights>();
		set.add(WithMediaRights.Creative);
		mo.setWithRights(set);
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
