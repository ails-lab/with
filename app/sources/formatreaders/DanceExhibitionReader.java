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
import java.util.HashMap;
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
import play.mvc.Results.Status;
import sources.core.ApacheHttpConnector;
import sources.core.HttpConnector;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public class DanceExhibitionReader extends ExhibitionReader {

	static private final Logger.ALogger log = Logger.of(DanceExhibitionReader.class);

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
		model.setLabel(text.getMultiLiteralValue("children[0].title"));
		model.setDescription(text.getMultiLiteralValue("children[0].description"));
		model.setCredits(text.getStringValue("children[0].who"));
		model.setKeywords(text.getMultiLiteralOrResourceValue("children[0].tags"));
		EmbeddedMediaObject media = new EmbeddedMediaObject();
		LiteralOrResource originalRights = null;
		media.setOriginalRights(originalRights);
		media.setUrl(text.getStringValue("children[0].mediaThumbURL"));
		media.setType(WithMediaType.IMAGE);
		model.setBackgroundImg(new HashMap<MediaVersion, EmbeddedMediaObject>() {
			{
				put(MediaVersion.Thumbnail, media);
			}
		});

		ProvenanceInfo provInfo = new ProvenanceInfo("Dance Space", text.getStringValue("id"),
				text.getStringValue("url"));
		exhibition.addToProvenance(provInfo);
		exhibition.getAdministrative().setExternalId(text.getStringValue("id"));
		ObjectNode resultInfo = Json.newObject();
		boolean success = CollectionObjectController.internalAddCollection(exhibition, WithResourceType.Exhibition,
				creatorDbId, resultInfo);
		if (success)
			return importExhibitionPagesObjectFrom(text, exhibition.getDbId());
		return null;
	}

	@Override
	public void importExhibitions(ObjectId creatorDbId, String exhibitionID) {
		JsonNode response;
		try {
			response = getHttpConnector()
					.getURLContent("https://dancespace.in-two.com/stages/" + exhibitionID + "?format=json");
			JsonContextRecord colobject = new JsonContextRecord(response);
			colobject.setValue("id", exhibitionID);
			colobject.setValue("url", "https://dancespace.in-two.com/stages/" + exhibitionID);
			importExhibitionObjectFrom(colobject, creatorDbId);
		} catch (Exception e) {
			log.error("Exeption", e);
		}
	}

	protected Object importExhibitionPagesObjectFrom(JsonContextRecord text, ObjectId collectionId) {
		int position = 0;
		try {
			Function<JsonNode, JsonContextRecord> function = (x) -> new JsonContextRecord(x);
			String  caption = text.getStringValue("children[0].title");
//			System.out.println("Caption "+caption);
			List<JsonContextRecord> pages = ListUtils.transform(text.getValues("children[.*]"), function);
			List<Group> sequences = new ArrayList<>(Collections.nCopies(pages.size(), null));
			for (JsonContextRecord page : pages) {
				JsonContextRecord parseTheItem = parseTheItem(collectionId, page, caption);
				position++;
			}
			Group o = buildSequenceElement(sequences, buildTextElement(text.getStringValue("title")));
			// o.setTitle(buildTextElement(text.getStringValue("title")));
			// o.setDescription(buildTextElement(text.getStringValue("description")));
			return o;
			// System.out.println(Json.toJson(o));
		} catch (Exception e) {
			log.error("Exeption", e);
		}
		return null;
	}


	private JsonContextRecord parseTheItem(ObjectId collectionId, JsonContextRecord itemJsonContextRecord,
			String caption) {
		try {
			String source = null;
			String id = itemJsonContextRecord.getStringValue("linkURL");
			CulturalObject record = new CulturalObject();
			CulturalObjectData descData = new CulturalObjectData();
			record.setDescriptiveData(descData);
			// if (source == null || id == null) {
			source = "UploadedByUser";

			record.addToProvenance(new ProvenanceInfo(source, id, id));
			MultiLiteral t = itemJsonContextRecord.getMultiLiteralValue("title");
//			System.out.println("label "+t);
			descData.setLabel(((!Utils.hasInfo(t)) ? new MultiLiteral(caption)
					: t));
			descData.setDescription(itemJsonContextRecord.getMultiLiteralValue("description"));
			descData.setDccreator(itemJsonContextRecord.getMultiLiteralOrResourceValue("tags"));
			descData.setDates(itemJsonContextRecord.getWithDateArrayValue("pubDate"));

			String rights = null;// rec1.getStringValue("element_texts[element.name=Rights].text");
			String type = null;// rec1.getStringValue("element_texts[element.name=Type].text");

			String thumbnail = itemJsonContextRecord.getStringValue("mediaThumbURL");

			EmbeddedMediaObject media = new EmbeddedMediaObject();
			// media.setOriginalRights(originalRights);
			media.setUrl(thumbnail);
			media.setType(WithMediaType.IMAGE);
			// media.setType(type);
			record.addMedia(MediaVersion.Thumbnail, media);

			
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
