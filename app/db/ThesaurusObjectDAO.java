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


package db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import sources.core.ParallelAPICall;

import com.fasterxml.jackson.databind.JsonNode;

import elastic.ElasticEraser;
import model.annotations.Annotation;
import model.resources.RecordResource;
import model.resources.ThesaurusObject;

public class ThesaurusObjectDAO extends DAO<ThesaurusObject> {

	public ThesaurusObjectDAO() {
		super(ThesaurusObject.class);
	}
	
	public ThesaurusObject getByUri(String uri) {
		return this.findOne("semantic.uri", uri);
	}
	
	public ThesaurusObject getByPrefLabel(String label) {
		Query<ThesaurusObject> q = this.createQuery().disableValidation()
				.field("semantic.prefLabel.default").equal(label);
		return this.findOne(q);
	}
	
	public List<ThesaurusObject> getByExactMatch(String uri) {
		Query<ThesaurusObject> q = this.createQuery().field("semantic.exactMatch").equal(uri);
		
		return this.find(q).asList();
	}

	public void editRecord(String root, ObjectId dbId, JsonNode json) {
		Query<ThesaurusObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<ThesaurusObject> updateOps = this.createUpdateOperations();
		
		updateFields(root, json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}
	
	public void removeAllTermsFromThesaurus(String thesaurus) {
		ArrayList<String> retrievedFields = new ArrayList<String>(Arrays.asList("_id"));
		
		Query<ThesaurusObject> q = this.createQuery().field("semantic.vocabulary.name").equal(thesaurus);
		q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		
		List<ThesaurusObject> termRecords = find(q).asList();
		
		removeAll("semantic.vocabulary.name", "=", thesaurus);
		
		List<ObjectId> termIds = new ArrayList<ObjectId>();
		termRecords.forEach((r) -> {
			termIds.add(r.getDbId());
		});
		
		Function<List<ObjectId>, Boolean> deleteResources = (List<ObjectId> ids) -> (ElasticEraser.deleteManyTermsFromThesaurus(ids));
		ParallelAPICall.createPromise(deleteResources, termIds);
	}

}
