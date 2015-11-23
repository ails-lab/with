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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import model.BasicDataTypes.Literal;
import model.BasicDataTypes.LiteralOrResource;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DescriptiveData {
	
	// one line content description with identifiable characteristic
	Literal label;
	
	// arbitrary length content description
	Literal description;
	
	// an indexers dream !! They can be literal concepts and enriched easily
	ArrayList<LiteralOrResource> keywords;
	
	// This are reachable URLs
	String isShownAt, isShownBy;
	
	
	// The whole legal bla, unedited, from the source, mostly cc0
	LiteralOrResource metadataRights;
	
	// rdf  .. Agent, Artist, Painter, Painting, Series
	String rdfType;
	
	// URIs how this Resource is known elsewhere
	ArrayList<String> sameAs;
	
	// in a timeline where would this resource appear
	int year;	
	
	// alternative title or name or placename
	ArrayList<Literal> altLabels;
}
