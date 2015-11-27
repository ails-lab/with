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

import model.resources.RecordResource;
import model.resources.RecordResource_;


/*
 * This class is the aggregator of methods
 * generically referring to *Object entities. We may assume
 * that these entities represent a Record of a Collection more or less.
 */
public class RecordResourceDAO extends CommonResourcesDAO<RecordResource>{

	public RecordResourceDAO(Class<?> entityClass) {
		super(RecordResource.class);
	}

	/*
	 * These are embedded classes for very specific queries
	 * in the far future.
	 */
	public class WithResourceDAO extends RecordResourceDAO {

		public WithResourceDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}

	public class AgentObjectDAO extends RecordResourceDAO {

		public AgentObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}
	
	public class CulturalObjectDAO extends RecordResourceDAO {

		public CulturalObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}

	public class EuscreenObjectDAO extends RecordResourceDAO {

		public EuscreenObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}
	
	public class EventObjectDAO extends RecordResourceDAO {

		public EventObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}
	
	public class PlaceObjectDAO extends RecordResourceDAO {

		public PlaceObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}
		
	}

	public class TimespanObjectDAO extends RecordResourceDAO {

		public TimespanObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}
	/*
	 * End of embedded D
	 */


}
