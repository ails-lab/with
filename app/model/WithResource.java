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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bson.types.ObjectId;

import model.BasicDataTypes.Literal;
import model.BasicDataTypes.LiteralOrResource;
import model.ExampleDataModels.Annotation;
import model.ExampleDataModels.EmbeddedMediaObject;
import model.ExampleDataModels.ExternalCollection;
import model.ExampleDataModels.ProvenanceInfo;
import model.ExampleDataModels.Usage;
import model.ExampleDataModels.WithAccess;

public class WithResource<T extends DescriptiveData> {
	
	public static class CollectionInfo {
		ObjectId collectionId;
		int position;
	}
	
	public class WithAdmin {
		
		ObjectId dbId;
		WithAccess access;
		
		// uri that this resource has in the rdf repository
		String withURI;
		
		Date created;
		Date lastModified;
	}

	
	WithAdmin administrative;
	ArrayList<CollectionInfo> collectedIn;
	
	Usage usage;
	
	//What is the 
	ArrayList<ExternalCollection> externalCollections;		
	ArrayList<ProvenanceInfo> provenance;

	// enum of classes that are derived from DescriptiveData
	String resourceType;
	
	T model;
	
	// All the available content serializations 
	// all keys in here should be understood by the WITH system
	HashMap<String, String> content;
	
	// all attached media Objects (their embedded part)
	ArrayList<EmbeddedMediaObject> media;
	
	//each time an annotation is added by a user, a copy of the Resource is made, i.e. colId-posId is removed from the parent record collectedIn array,
	//and added in the collectedIn array of the copy. The new copy has all annotations of the parent, plus the new ones added by the user.
	//If a user adds an annotation of an annotationType that already exists in the resource, we edit the annotation entry.
	ArrayList<Annotation> annotations;
	

}
