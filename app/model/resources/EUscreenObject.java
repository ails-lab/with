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

import java.util.Date;

import org.mongodb.morphia.annotations.Entity;
import model.resources.RecordResource.RecordDescriptiveData;


@Entity("RecordResource")
public class EUscreenObject extends RecordResource<EUscreenObject.EUscreenData> {
	
	public static class EUscreenData extends RecordDescriptiveData { 
		// title is filled in with original language title and english title
		// description dito
		
		String broadcastChannel;
		Date brodcastDate;
		
		// in year we keep the production year
		
	}

}
