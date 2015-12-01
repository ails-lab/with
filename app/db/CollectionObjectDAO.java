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
import java.util.List;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import utils.Tuple;
import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.usersAndGroups.User;

public class CollectionObjectDAO extends CommonResourcesDAO<CollectionObject> {

	/*
	 * The constructor is optional becuse the explicit
	 * type is passed through generics.
	 */
	public CollectionObjectDAO() {
		super(CollectionObject.class);
	}

	/**
	 * Increment entryCount (number of entries collected) in a CollectionObject
	 * @param dbId
	 */
	public void incEntryCount(ObjectId dbId) {
		incField(dbId, "administrative.entryCount");
	}

	/**
	 * Decrement entryCount (number of entries collected) in a CollectionObject
	 * @param dbId
	 */
	public void decEntryCount(ObjectId dbId) {
		decField(dbId, "administrative.entryCount");
	}
}
