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


package db.resources;

import java.util.List;

import model.CollectionRecord;
import model.resources.CulturalObject;
import model.resources.WithResource;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import db.DAO;
import play.Logger;
import play.Logger.ALogger;

public class CulturalObjectDAO extends CommonResourcesDAO<CulturalObject> {
	public static final ALogger log = Logger.of(CulturalObject.class);

	public CulturalObjectDAO() {
		super(WithResource.class);
	}
}
