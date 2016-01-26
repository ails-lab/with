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


package general.elasticsearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import model.Collection;
import model.CollectionRecord;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.MediaObject;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResource.ExternalCollection;
import model.usersAndGroups.User;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import db.DB;
import elastic.Elastic;
import elastic.ElasticEraser;
import elastic.ElasticIndexer;
import elastic.ElasticReindexer;
import elastic.ElasticUpdater;
import elastic.ElasticUtils;

public class ElasticTest {

	@Test
	public void testIndex() {


		RecordResource co = getRecordResource();
		//CollectionObject co = DB.getCollectionObjectDAO().getById(new ObjectId("569e1f284f55a2655367ec1e"));
		if (DB.getRecordResourceDAO().makePermanent(co) == null) { System.out.println("No storage!"); return; }
		System.out.println("Stored!");
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		Json.setObjectMapper(mapper);
		System.out.println(Json.toJson(co));
		System.out.println(ElasticUtils.transformRR(co));

		ElasticIndexer.index(Elastic.type, co.getDbId(), ElasticUtils.transformRR(co));

	}

	@Test
	public void testIndexMany() {
		List<ObjectId> ids = new ArrayList<ObjectId>();
		ids.add(new ObjectId("5697ad734f55a2518f8982cb"));
		ids.add(new ObjectId("5697ae5f4f55a254f3b2a3d5"));
		ids.add(new ObjectId("5694e7774f55a2444eec1c27"));
		ids.add(new ObjectId("5694e98f4f55a24fb6d01e10"));
		List<RecordResource> rrs = DB.getRecordResourceDAO().getByIds(ids);

		List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
		ids.clear();
		for(RecordResource rr: rrs) {
			ids.add(rr.getDbId());
			docs.add(ElasticUtils.transformRR(rr));
		}
		ElasticIndexer.indexMany(Elastic.type, ids, docs);
	}


	@Test
	public void testDeleteResource() {
		RecordResource rr = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr);
		ElasticIndexer.index(Elastic.type, rr.getDbId(), ElasticUtils.transformRR(rr));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));

		ElasticEraser.deleteResource(rr.getDbId().toString());

		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 0);
		DB.getRecordResourceDAO().deleteById(rr.getDbId());
	}

	@Test
	public void testDeleteManyResources() {
		RecordResource rr1 = getRecordResource();
		RecordResource rr2 = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr1);
		DB.getRecordResourceDAO().makePermanent(rr2);

		ElasticIndexer.index(Elastic.type, rr1.getDbId(), ElasticUtils.transformRR(rr1));
		ElasticIndexer.index(Elastic.type, rr2.getDbId(), ElasticUtils.transformRR(rr2));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr1.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();
		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));

		termQ = QueryBuilders.termQuery("_id", rr2.getDbId());
		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();
		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));


		List<String> ids = new ArrayList<String>();
		ids.add(rr1.getDbId().toString());
		ids.add(rr2.getDbId().toString());
		ElasticEraser.deleteManyResources(ids);

		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 0);
		DB.getRecordResourceDAO().deleteById(rr1.getDbId());
		DB.getRecordResourceDAO().deleteById(rr2.getDbId());
	}

	@Test
	public void testUpdateOne() {

		RecordResource rr = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr);

		ElasticIndexer.index(Elastic.type, rr.getDbId(), ElasticUtils.transformRR(rr));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		//assertEquals(resp.getHits().getTotalHits(), 1);

		Map<String, Object> doc = new HashMap<String, Object>();
		doc.put("label.en", "666 title");


		ElasticUpdater.updateOne(rr.getDbId(), doc);

		termQ = QueryBuilders.termQuery("label.en", "666 title");
		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 1);

		DB.getRecordResourceDAO().deleteById(rr.getDbId());
	}

	@Test
	public void testUpdateMany() throws Exception {
		RecordResource rr1 = getRecordResource();
		RecordResource rr2 = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr1);
		DB.getRecordResourceDAO().makePermanent(rr2);


		ElasticIndexer.index(Elastic.type, rr1.getDbId(), ElasticUtils.transformRR(rr1));
		ElasticIndexer.index(Elastic.type, rr2.getDbId(), ElasticUtils.transformRR(rr2));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr1.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();
		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));

		termQ = QueryBuilders.termQuery("_id", rr2.getDbId());
		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();
		assertThat(resp.getHits().getTotalHits(), not(equalTo(0)));

		Map<String, Object> doc1 = new HashMap<String, Object>();
		doc1.put("label.en", "667 title");
		Map<String, Object> doc2 = new HashMap<String, Object>();
		doc2.put("label.en", "667 title");

		List<ObjectId> ids = new ArrayList<ObjectId>();
		List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
		ids.add(rr1.getDbId());
		ids.add(rr2.getDbId());
		docs.add(doc1);
		docs.add(doc2);

		ElasticUpdater.updateMany(ids, docs);

		termQ = QueryBuilders.termQuery("label", "This is the new 1 label");
		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 1);

		termQ = QueryBuilders.termQuery("label", "This is the new 2 label");
		resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 1);

		DB.getRecordResourceDAO().deleteById(rr1.getDbId());
		DB.getRecordResourceDAO().deleteById(rr2.getDbId());
	}

	@Test
	public void testAddResourceToCollection() {
		RecordResource rr = getRecordResource();
		DB.getRecordResourceDAO().makePermanent(rr);

		ElasticIndexer.index(Elastic.type, rr.getDbId(), ElasticUtils.transformRR(rr));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		//assertEquals(resp.getHits().getTotalHits(), 1);
		List<CollectionInfo> list = rr.getCollectedIn();
		list.add(new CollectionInfo(new ObjectId(), 666));
		try {
			ElasticUpdater.addResourceToCollection(rr.getDbId().toString(), list);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testRemoveResourceFromCollection() {
		RecordResource rr = getRecordResource();
		//collectedIn
		ObjectId oid = new ObjectId();
		List<CollectionInfo> collectedIn = new ArrayList<CollectionInfo>();
		CollectionInfo ci = new CollectionInfo(oid, 42);
		collectedIn.add(ci);
		rr.setCollectedIn(collectedIn);
		DB.getRecordResourceDAO().makePermanent(rr);

		ElasticIndexer.index(Elastic.type, rr.getDbId(), ElasticUtils.transformRR(rr));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 1);

		ElasticUpdater.removeResourceFromCollection(rr.getDbId().toString(), oid, 42);
	}

	@Test
	public void testUpdatePositionInCollection() {
		RecordResource rr = getRecordResource();
		//collectedIn
		ObjectId oid = new ObjectId();
		List<CollectionInfo> collectedIn = new ArrayList<CollectionInfo>();
		CollectionInfo ci = new CollectionInfo(oid, 42);
		collectedIn.add(ci);
		rr.setCollectedIn(collectedIn);
		DB.getRecordResourceDAO().makePermanent(rr);

		ElasticIndexer.index(Elastic.type, rr.getDbId(), ElasticUtils.transformRR(rr));

		TermQueryBuilder termQ = QueryBuilders.termQuery("_id", rr.getDbId());
		SearchResponse resp = Elastic.getTransportClient().prepareSearch(Elastic.index)
				.setSize(0)
				.setTerminateAfter(1)
				.setQuery(termQ)
				.execute().actionGet();

		assertEquals(resp.getHits().getTotalHits(), 1);

		ElasticUpdater.updatePositionInCollection(rr.getDbId().toString(), oid, 42, 6666);
	}

	@Test
	public void testUpdateVisibility() {

	}

	@Test
	public void testIndexInconsistentDocs() {
		ElasticReindexer.indexInconsistentDocs();
	}

	/* ************************ OLD CODE ****************** */
	@Test
	public void reindex_records_from_mongo() {
		List<CollectionRecord> allRecs = DB.getCollectionRecordDAO().find().asList();
		for(CollectionRecord r: allRecs) {
			//ElasticIndexer indexer = new ElasticIndexer(r);
			//indexer.index();
		}
	}


	@Test
	public void reindex_collection_from_mongo() {
		List<Collection> allCols = DB.getCollectionDAO().find().asList();
		for(Collection c: allCols) {
			//ElasticIndexer indexer = new ElasticIndexer(c);
			//indexer.indexCollectionMetadata();
		}
	}


	/* **************** PRIVATE METHODS ********************** */


	private RecordResource getRecordResource() {
		RecordResource<RecordDescriptiveData> co = new RecordResource<RecordResource.RecordDescriptiveData>();

		/*
		 * Owner of the CollectionObject
		 *
		 */
		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u == null) {
			System.out.println("No user found");
			return null;
		}

		/*
		 * Administative metadata
		 */
		co.getAdministrative().setCreated(new Date());
		//wa.setWithCreator(u.getDbId());
		WithAccess waccess = new WithAccess();
		waccess.getAcl().add(new AccessEntry(u.getDbId(), Access.OWN));
		co.getAdministrative().setAccess(waccess);

		//no externalCollections
		List<ExternalCollection> ec;

		//no provenance
		List<ProvenanceInfo> prov;

		//collectedIn
		List<CollectionInfo> collectedIn = new ArrayList<CollectionInfo>();
		CollectionInfo ci = new CollectionInfo(new ObjectId(), 42);
		collectedIn.add(ci);
		co.setCollectedIn(collectedIn);

		//resourceType is collectionObject
		//co.setResourceType(WithResourceType.CollectionObject);
		// type: metadata specific for a collection
		MultiLiteral label = new MultiLiteral(Language.EN, "This is title");
		RecordDescriptiveData cdd = new RecordDescriptiveData();
		cdd.setLabel(label);
		MultiLiteral desc = new MultiLiteral(Language.EN, "This is a description");
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
		EmbeddedMediaObject emo = getMediaObject();
		medias.add(emo);
		//co.setMedia(medias);

		return co;
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
		mo.setWithRights(WithMediaRights.Creative);
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
