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

import model.BasicDataTypes.CidocEvent;
import model.BasicDataTypes.Literal;
import model.BasicDataTypes.LiteralOrResource;
import model.BasicDataTypes.WithDate;
import model.DescriptiveData;
import model.WithResource;

public class CulturalObject extends WithResource<CulturalObject.CulturalObjectData>{
	
	public static class CulturalObjectData extends DescriptiveData {
		
		// provenance[0].recordId
		String dcidentifier;
		
		// language of object, if it has one. Not related to the metadata.
		// eg. If the object is a book, its the language it is written in
		// no language for most paintings, vases, sculptures ... etc
		ArrayList<Literal> dclanguage;
		
		// Painting, Sculpture, Building, Book .... 
		ArrayList<LiteralOrResource> dctype;
		
		// places or times
		ArrayList<LiteralOrResource> dccoverage;
		
		// places are here
		ArrayList<LiteralOrResource> dcspatial;
		
		ArrayList<LiteralOrResource> dccreator;

		ArrayList<WithDate> dccreated;
		ArrayList<WithDate> dcdate;
		
		//TODO: do we want multilinguality for dcformat and dctermsmedium?
		ArrayList<LiteralOrResource> dcformat;
		ArrayList<LiteralOrResource> dctermsmedium;
		
		ArrayList<LiteralOrResource> isRelatedTo;
		
		ArrayList<CidocEvent> events;
		
	}

}
