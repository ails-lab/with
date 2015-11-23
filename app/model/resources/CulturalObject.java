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

import org.bson.types.ObjectId;

import model.BasicDataTypes.CidocEvent;
import model.BasicDataTypes.Literal;
import model.BasicDataTypes.LiteralOrResource;
import model.BasicDataTypes.WithDate;
import model.ExampleDataModels.WithAdmin;
import model.DescriptiveData;
import model.WithResource;

public class CulturalObject extends WithResource<CulturalObject.CulturalObjectData>{
	
	public static class RecordAdmin extends WithAdmin {
		// last entry of provenance chain hash of provider and recordId
		private String externalId;
				
		// if this resource / record is derived (modified) from a different Record.
		private ObjectId parentResourceId;
	}
	
	
	public static class CulturalObjectData extends DescriptiveData {
		
		// provenance[0].recordId
		private String dcidentifier;
		
		// language of object, if it has one. Not related to the metadata.
		// eg. If the object is a book, its the language it is written in
		// no language for most paintings, vases, sculptures ... etc
		private ArrayList<Literal> dclanguage;
		
		// Painting, Sculpture, Building, Book .... 
		private ArrayList<LiteralOrResource> dctype;
		
		// places or times
		private ArrayList<LiteralOrResource> dccoverage;
		
		// places are here
		private ArrayList<LiteralOrResource> dcspatial;
		
		private ArrayList<LiteralOrResource> dccreator;

		private ArrayList<WithDate> dccreated;
		private ArrayList<WithDate> dcdate;
		
		//TODO: do we want multilinguality for dcformat and dctermsmedium?
		private ArrayList<LiteralOrResource> dcformat;
		private ArrayList<LiteralOrResource> dctermsmedium;
		
		private ArrayList<LiteralOrResource> isRelatedTo;
		
		private ArrayList<CidocEvent> events;
		
	}

}
