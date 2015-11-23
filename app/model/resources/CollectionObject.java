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
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import model.BasicDataTypes.LiteralOrResource;
import model.ExampleDataModels.WithAccess;
import model.ExampleDataModels.WithAdmin;
import model.DescriptiveData;
import model.WithResource;

public class CollectionObject extends WithResource<CollectionObject.CollectionDescriptiveData> {
	
	public static class CollectionAdmin extends WithAdmin {
		private int entryCount;
		private boolean isExhibition;
	}
	
	public static class CollectionDescriptiveData extends DescriptiveData {
		private ArrayList<LiteralOrResource> dccreator;
		private ArrayList<LiteralOrResource> dctermsaudience;
		private ArrayList<LiteralOrResource> dclanguage;
		//TODO: add link to external collection
	}

}
