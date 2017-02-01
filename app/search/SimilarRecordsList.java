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


package search;

import org.bson.types.ObjectId;

import db.DB;
import model.basicDataTypes.Literal;
import model.resources.RecordResource;

public abstract class SimilarRecordsList extends RecordsList {

	public SimilarRecordsList(String identifier, Literal title) {
		super(identifier,title);
		// TODO Auto-generated constructor stub
	}

	public SimilarRecordsList(String identifier, Literal title, Literal description) {
		super(identifier,title, description);
		// TODO Auto-generated constructor stub
	}

	public SimilarRecordsList() {
		super();
	}
	
	public RecordResource<?> getTheRecord(SimilarsQuery q){
		RecordResource record = DB.getRecordResourceDAO().get(
				new ObjectId(q.getRecordId()));
		return record;
	}
	
	public abstract void query(SimilarsQuery q);

}
