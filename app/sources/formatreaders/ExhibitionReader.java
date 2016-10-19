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

import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.japi.Option;
import controllers.CollectionObjectController;
import controllers.WithResourceController;
import db.DB;
import model.annotations.ContextData;
import model.annotations.ExhibitionData;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.Resource;
import model.resources.CulturalObject;
import model.resources.WithResourceType;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.collection.Exhibition;
import model.resources.collection.Exhibition.ExhibitionDescriptiveData;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import sources.core.ApacheHttpConnector;
import sources.core.HttpConnector;
import sources.utils.JsonContextRecord;

public class ExhibitionReader {
	
	static private final Logger.ALogger log = Logger.of(ExhibitionReader.class);

	
	public static HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}
	public void importOmeka(ObjectId creatorDbId){
		JsonNode response;
		try {
			response = getHttpConnector().getURLContent("http://tellyourphotostory.be/espace_test/api/exhibits");
			JsonContextRecord rec = new JsonContextRecord(response);
			fillExhibitionObjectFrom(rec, creatorDbId);
		} catch (Exception e) {
			log.error("Exeption",e);
		}
		
	}
	protected Exhibition fillExhibitionObjectFrom(JsonContextRecord text, ObjectId creatorDbId) {
		
		Exhibition exhibition = new Exhibition();
		exhibition.getAdministrative().getAccess().setIsPublic(true);

		ExhibitionDescriptiveData model = new ExhibitionDescriptiveData();
		exhibition.setDescriptiveData(model);
		model.setMetadataRights(new LiteralOrResource("http://creativecommons.org/publicdomain/zero/1.0/"));
		model.setRdfType(new Resource( "http://www.europeana.eu/schemas/edm/ProvidedCHO"));
		model.setLabel(text.getMultiLiteralValue("title"));
		model.setDescription(text.getMultiLiteralValue("description"));
		model.setCredits(text.getStringValue("credits"));
		ProvenanceInfo provInfo = new ProvenanceInfo("OMECA",  text.getStringValue("id"), text.getStringValue("url"));
		exhibition.addToProvenance(provInfo);
		exhibition.getAdministrative().setExternalId(text.getStringValue("id"));
        ObjectNode resultInfo = Json.newObject();
		boolean success = CollectionObjectController.internalAddCollection(exhibition,
                WithResourceType.SimpleCollection, creatorDbId, resultInfo);
		if (success)
			importExhibitionPagesObjectFrom(text, exhibition.getDbId());
		return exhibition;
	}
	
	protected void importExhibitionPagesObjectFrom(JsonContextRecord text, ObjectId collectionId) {
		String url = text.getStringValue("pages.url");
		try {
		JsonNode response = getHttpConnector().getURLContent(url);
		JsonContextRecord rec = new JsonContextRecord(response);
		List<String> list = rec.getStringArrayValue("[0].page_blocks[.*].attachments[0].item.url");
			for (int i = 0; i < list.size(); i++) {

				JsonNode response1;
				try {
					response1 = getHttpConnector().getURLContent(list.get(i));
					JsonContextRecord rec1 = new JsonContextRecord(response1);
					String source = rec1.getStringValue("item_type.name");
					String id = rec1.getStringValue("element_texts[element.name=Identifier].text");

					CulturalObject record = new CulturalObject();
					CulturalObjectData descriptiveData = new CulturalObjectData();
					descriptiveData.setLabel(new MultiLiteral(id).fillDEF());
					record.setDescriptiveData(descriptiveData);
					record.addToProvenance(new ProvenanceInfo(source, null, id));
					play.libs.F.Option<Integer> p = play.libs.F.Option.Some(new Integer(i));
					WithResourceController.addRecordToCollection(Json.toJson(record), collectionId, p, false);
				} catch (Exception e) {
					log.error("Exeption", e);
				}
			}
		} catch (Exception e) {
			log.error("Exeption", e);
		}
	}

}

