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


package model;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

/**
 * A fully materialized Record from the backend ... indexed and all.
 * @author stabenau
 *
 */
public class Record {
	@Id
	private ObjectId dbID;
	
	@Embedded 
	private RecordLink baseLinkData;
	
	// there will be different serializations of the record available in here
	// like "EDM" -> xml for the EDM
	// "json EDM" -> json format of the EDM?
	// "json UI" -> ...
	// "source format" -> ...
	private Map<String, String> content;
	
	// capped, denormalization of Tags on this record
	// When somebody adds a tag to a record, and the cap is not reached, it will go here 
	// This might get out of sync on tag deletes, since a deleted tag from one user doesn't necessarily delete
	// the tag from here. Tag cleaning has to be performed regularly.
	private Set<String> tags;
	
}
