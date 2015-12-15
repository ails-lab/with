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


package model.resources;

import java.util.ArrayList;

import org.mongodb.morphia.annotations.Entity;

import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithPeriod;
import model.DescriptiveData;
import model.resources.RecordResource.RecordDescriptiveData;

@Entity("RecordResource")
public class EventObject extends RecordResource<EventObject.EventData> {
	
	public static class EventData extends RecordDescriptiveData {
		ArrayList<WithPeriod> period;
		MultiLiteralOrResource personsInvolved;
		MultiLiteralOrResource placesInvolved;
		MultiLiteralOrResource objectsInvolved;
	}
}
