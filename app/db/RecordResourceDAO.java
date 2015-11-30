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

import model.resources.AgentObject;
import model.resources.CulturalObject;
import model.resources.EUscreenObject;
import model.resources.EventObject;
import model.resources.PlaceObject;
import model.resources.RecordResource;
import model.resources.TimespanObject;
import model.resources.WithResource;


/*
 * This class is the aggregator of methods
 * generically referring to *Object entities. We may assume
 * that these entities represent a Record of a Collection more or less.
 * 
 * Type T is used in order for Morphia to know in which type is going
 * deserialize the object retrieved fro Mongo. So we have to options
 * 
 * 1. Either pass WithResource when instansiating so that all entities 
 * handled as WithResources.
 *  
 * 2. Every time create a new DAO class associated with the explicit class
 * that I want to retieve.
 */
public class RecordResourceDAO extends CommonResourcesDAO<RecordResource> {

	public RecordResourceDAO() {
		super(RecordResource.class);
	}

	/*
	 * These are embedded classes for very specific queries
	 * in the far future.
	 */
/*
	public class AgentObjectDAO extends RecordResourceDAO<AgentObject> {

		public AgentObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}
	
	public class CulturalObjectDAO extends RecordResourceDAO<CulturalObject> {

		public CulturalObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}

	public class EuscreenObjectDAO extends RecordResourceDAO<EUscreenObject> {

		public EuscreenObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}
	
	public class EventObjectDAO extends RecordResourceDAO<EventObject> {

		public EventObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}
	
	public class PlaceObjectDAO extends RecordResourceDAO<PlaceObject> {

		public PlaceObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}

	public class TimespanObjectDAO extends RecordResourceDAO<TimespanObject> {

		public TimespanObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}

*/

}
