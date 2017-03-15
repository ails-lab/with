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


package sources.formatreaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.bson.types.ObjectId;
import org.mupop.model.group.Group;
import org.mupop.model.media.Text;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CollectionObjectController;
import controllers.WithResourceController;
import db.DB;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaType;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ContextData.ContextDataType;
import model.annotations.ExhibitionData;
import model.annotations.ExhibitionData.ExhibitionAnnotationBody;
import model.annotations.ExhibitionData.MediaType;
import model.annotations.ExhibitionData.TextPosition;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.Resource;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.WithResourceType;
import model.resources.collection.Exhibition;
import model.resources.collection.Exhibition.ExhibitionDescriptiveData;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;
import sources.core.ApacheHttpConnector;
import sources.core.HttpConnector;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public class OmekaExhibitionReader extends ExhibitionReader {

	static private final Logger.ALogger log = Logger.of(OmekaExhibitionReader.class);

	public static HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}

	public Object importExhibitionObjectFrom(JsonContextRecord text, ObjectId creatorDbId) {

		Exhibition exhibition = new Exhibition();
		exhibition.getAdministrative().getAccess().setIsPublic(true);

		ExhibitionDescriptiveData model = new ExhibitionDescriptiveData();
		exhibition.setDescriptiveData(model);
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType(new Resource("http://www.europeana.eu/schemas/edm/ProvidedCHO"));
		model.setLabel(text.getMultiLiteralValue("title"));
		model.setDescription(text.getMultiLiteralValue("description"));
		model.setCredits(text.getStringValue("credits"));
		ProvenanceInfo provInfo = new ProvenanceInfo("OMECA", text.getStringValue("id"), text.getStringValue("url"));
		exhibition.addToProvenance(provInfo);
		exhibition.getAdministrative().setExternalId(text.getStringValue("id"));
		ObjectNode resultInfo = Json.newObject();
		boolean success = CollectionObjectController.internalAddCollection(exhibition, WithResourceType.Exhibition,
				creatorDbId, resultInfo);
		if (success)
			return importExhibitionPagesObjectFrom(text, exhibition.getDbId());
		else
			log.error(resultInfo.toString());
		return null;
	}

	public void importExhibitions(ObjectId creatorDbId, String exhibitionID){
		importOmeka(creatorDbId, exhibitionID);
	}

	public void importOmeka(ObjectId creatorDbId, String exhibitionID) {
		JsonNode response;
		try {
			response = getHttpConnector().getURLContent("http://tellyourphotostory.be/espace_test/api/exhibits");
			int ncollections = response.size();
			for (int i = 0; i < ncollections; i++) {
				JsonContextRecord colobject = new JsonContextRecord(response.get(i));
				String stringValue = colobject.getStringValue("id");

				if (stringValue.matches(exhibitionID))
					importExhibitionObjectFrom(colobject, creatorDbId);
			}
		} catch (Exception e) {
			log.error("Exeption", e);
		}

	}

	protected Object importExhibitionPagesObjectFrom(JsonContextRecord text, ObjectId collectionId) {
//		log.debug("loading pages");
		String url = text.getStringValue("pages.url");
		int position = 0;
		try {
			JsonNode response = getHttpConnector().getURLContent(url);
			JsonContextRecord rec = new JsonContextRecord(response);
			Function<JsonNode, JsonContextRecord> function = (x) -> new JsonContextRecord(x);
			List<JsonContextRecord> pages = ListUtils.transform(rec.getValues("[.*]"), function);
			List<Group> sequences = new ArrayList<>(Collections.nCopies(pages.size(), null));
			Comparator<JsonContextRecord> c = new Comparator<JsonContextRecord>() {
				@Override
				public int compare(JsonContextRecord o1, JsonContextRecord o2) {
					// TODO Auto-generated method stub
					return o1.getIntValue("order")-o2.getIntValue("order");
				}
				
			};
			pages.sort(c);
			for (JsonContextRecord page : pages) {
				position = parsePage(collectionId, position, sequences, page);
			}
			
			Group o = buildSequenceElement(sequences, buildTextElement(text.getStringValue("title")));

			o.setTitle(buildTextElement(text.getStringValue("title")));
			o.setDescription(buildTextElement(text.getStringValue("description")));
			return o;
			// System.out.println(Json.toJson(o));
		} catch (Exception e) {
			log.error("Exeption", e);
		}
		return null;
	}

	private int parsePage(ObjectId collectionId, int position,
			List<Group> sequences, JsonContextRecord page) throws Exception {
		Function<JsonNode, JsonContextRecord> function = (x) -> new JsonContextRecord(x);
		List<JsonContextRecord> pageBlocks = ListUtils.transform(page.getValues("page_blocks[.*]"), function);
		List<Group> blocks = new ArrayList<>(Collections.nCopies(pageBlocks.size(), null));
		Comparator<JsonContextRecord> c = new Comparator<JsonContextRecord>() {
			@Override
			public int compare(JsonContextRecord o1, JsonContextRecord o2) {
				// TODO Auto-generated method stub
				return o1.getIntValue("order") - o2.getIntValue("order");
			}
		};
		pageBlocks.sort(c);
		for (JsonContextRecord pageBlock : pageBlocks) {
			String blocktext = pageBlock.getStringValue("text");
			Text texto = buildTextElement(blocktext);
			List<JsonContextRecord> records = ListUtils.transform(pageBlock.getValues("attachments[.*]"),
					function);
			List<Group> recs = new ArrayList<>();
			if (records.size()>0)
			for (JsonContextRecord item : records) {
				String caption = item.getStringValue("caption");
				if (caption==null)
					caption = page.getStringValue("title");
				JsonContextRecord parseTheItem = parseTheItem(collectionId, item, caption);
				if (parseTheItem != null) {
					Group cho = buildCHOElement(parseTheItem, buildTextElement(caption));
					recs.add(cho);
				}
				
				
				if (blocktext!=null){
					ObjectId recordId = new ObjectId(parseTheItem.getStringValue("dbId"));
					ExhibitionAnnotationBody body;
					// TODO check the caption here
					ExhibitionData newContextData = new ExhibitionData();
					body = newContextData.getBody();
					body.setText(new Literal(Language.DEFAULT,blocktext));
					body.setTextPosition(TextPosition.RIGHT);
					body.setMediaType(MediaType.VIDEO);
					ContextDataTarget target = new ContextDataTarget();
					target.setRecordId(recordId);
					newContextData.setTarget(target);
					DB.getCollectionObjectDAO()
					.updateContextData(collectionId, newContextData,
							position);
				}
				position++;
					
			} else {
				// Add a dummy record with the text
				if (blocktext!=null){
					String source = "UploadedByUser";
					String id = System.currentTimeMillis()+"";
					CulturalObject record = new CulturalObject();
					CulturalObjectData descData = new CulturalObjectData();
					record.setDescriptiveData(descData);
					record.addToProvenance(new ProvenanceInfo(source, null, id));
					descData.setLabel(new MultiLiteral(page.getStringValue("title")));
					descData.setDescription(new MultiLiteral(blocktext));
					
					play.libs.F.Option<Integer> p = play.libs.F.Option.None();
					// TODO check how it is added for WITHin source
					JsonNode ojson = Json.toJson(record);
					Result result = WithResourceController.addRecordToCollection(ojson, collectionId, p, false);
					log.debug(result.toString());
					
					ObjectId recordId = new ObjectId(new JsonContextRecord(ojson).getStringValue("dbId"));
					ExhibitionAnnotationBody body;
					// TODO check the caption here
					ExhibitionData newContextData = new ExhibitionData();
					body = newContextData.getBody();
					body.setText(new Literal(Language.DEFAULT,blocktext));
					body.setTextPosition(TextPosition.RIGHT);
					body.setMediaType(MediaType.VIDEO);
					ContextDataTarget target = new ContextDataTarget();
					target.setRecordId(recordId);
					newContextData.setTarget(target);
					DB.getCollectionObjectDAO()
					.updateContextData(collectionId, newContextData,
							position);
					
					
					position++;
					
					
				}
			}
			// TODO take the items in order
			int pi = pageBlock.getIntValue("order") - 1;
//			System.out.println("block " + pi);
			blocks.set(pi, buildTogetherElement(recs, texto));
			// TODO add a together
		}
		int si = Math.max(0, page.getIntValue("order") - 1);
//		System.out.println("page " + si);
		sequences.set(si, buildSequenceElement(blocks, buildTextElement(page.getStringValue("title"))));
		// TODO add the group
		return position;
	}

	private JsonContextRecord parseTheItem(ObjectId collectionId, JsonContextRecord itemJsonContextRecord, String caption) {
		JsonNode response1;
		try {
			response1 = getHttpConnector().getURLContent(itemJsonContextRecord.getStringValue("item.url"));
			JsonContextRecord rec1 = new JsonContextRecord(response1);
			String source = rec1.getStringValue("item_type.name");
			String id = rec1.getStringValue("element_texts[element.name=Identifier].text");
			CulturalObject record = new CulturalObject();
			CulturalObjectData descData = new CulturalObjectData();
			record.setDescriptiveData(descData);
//			if (source == null || id == null) {
				source = "UploadedByUser";
				id = rec1.getStringValue("id");
				id+=("-"+itemJsonContextRecord.getStringValue("file.id"));
				
				record.addToProvenance(new ProvenanceInfo(source, null, id));

				descData.setLabel(((caption!=null)?new MultiLiteral(caption):rec1.getMultiLiteralValue("element_texts[element.name=Title].text")));
				descData.setAltLabels(rec1.getMultiLiteralValue("element_texts[element.name=Title].text"));
				descData.setDescription(rec1.getMultiLiteralValue("element_texts[element.name=Description].text"));
				descData.setDccreator(rec1.getMultiLiteralOrResourceValue("element_texts[element.name=Creator].text"));
				descData.setDates(rec1.getWithDateArrayValue("element_texts[element.name=Date].text"));

				String rights = rec1.getStringValue("element_texts[element.name=Rights].text");
				String type = rec1.getStringValue("element_texts[element.name=Type].text");
				String fileurl = itemJsonContextRecord.getStringValue("file.url");
				
				if (fileurl!=null){
					response1 = getHttpConnector().getURLContent(fileurl);
					JsonContextRecord rec2 = new JsonContextRecord(response1);
				
					String original = rec2.getStringValue("file_urls.original");
					String thumbnail = rec2.getStringValue("file_urls.thumbnail");

					EmbeddedMediaObject media = new EmbeddedMediaObject();
					LiteralOrResource originalRights = rights != null ? new LiteralOrResource(rights) : null;
					media.setOriginalRights(originalRights);
					media.setUrl(original);
					// media.setType(type);
					media.setType(WithMediaType.IMAGE);
					record.addMedia(MediaVersion.Original, media);

					media = new EmbeddedMediaObject();
					media.setOriginalRights(originalRights);
					media.setUrl(thumbnail);
					media.setType(WithMediaType.IMAGE);
					// media.setType(type);
					record.addMedia(MediaVersion.Thumbnail, media);
				}

//			} else {
//
//				descData.setLabel(((caption!=null)?new MultiLiteral(caption):rec1.getMultiLiteralValue("element_texts[element.name=Title].text")));
//				descData.setAltLabels(rec1.getMultiLiteralValue("element_texts[element.name=Title].text"));
//				descData.setDescription(rec1.getMultiLiteralValue("element_texts[element.name=Description].text"));
//				descData.setDccreator(rec1.getMultiLiteralOrResourceValue("element_texts[element.name=Creator].text"));
//				descData.setDates(rec1.getWithDateArrayValue("element_texts[element.name=Date].text"));
//
//				
//				record.addToProvenance(new ProvenanceInfo(source, null, id));
//
//				String rights = rec1.getStringValue("element_texts[element.name=Rights].text");
//				String fileurl = itemJsonContextRecord.getStringValue("file.url");
//				
//				if (fileurl!=null){
//					response1 = getHttpConnector().getURLContent(fileurl);
//					JsonContextRecord rec2 = new JsonContextRecord(response1);
//				
//					String original = rec2.getStringValue("file_urls.original");
//					String thumbnail = rec2.getStringValue("file_urls.thumbnail");
//
//					EmbeddedMediaObject media = new EmbeddedMediaObject();
//					LiteralOrResource originalRights = rights != null ? new LiteralOrResource(rights) : null;
//					media.setOriginalRights(originalRights);
//					media.setUrl(original);
//					// media.setType(type);
//					media.setType(WithMediaType.IMAGE);
//					record.addMedia(MediaVersion.Original, media);
//
//					media = new EmbeddedMediaObject();
//					media.setOriginalRights(originalRights);
//					media.setUrl(thumbnail);
//					media.setType(WithMediaType.IMAGE);
//					// media.setType(type);
//					record.addMedia(MediaVersion.Thumbnail, media);
//				}
//				
//			}
			play.libs.F.Option<Integer> p = play.libs.F.Option.None();
			// TODO check how it is added for WITHin source
			JsonNode ojson = Json.toJson(record);
			Result result = WithResourceController.addRecordToCollection(ojson, collectionId, p, false);
			log.debug(result.toString());
			// TODO put the text somewhere
			return new JsonContextRecord(ojson);
		} catch (Exception e) {
			log.error("Exeption", e);
		}
		return null;
	}

}
