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

import model.DescriptiveData;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.WithDate;


public class AgentObject extends WithResource<AgentObject.AgentData> {

	public static class AgentData extends DescriptiveData {
		public AgentData(Literal label) {
			super(label);
		}
		ArrayList<WithDate> birthdate;
		ArrayList<LiteralOrResource> birthplace;
		ArrayList<WithDate> deathdate;
		public static enum Gender {
			MALE, FEMALE, UNKNOWN
		}
		Gender genderEnum;
		Literal gender;
	}
}
