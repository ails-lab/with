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

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DescriptiveData {
	
	public DescriptiveData() {
	}
	
	public DescriptiveData(Literal label) {
		this.label = label;
	}
	// one line content description with identifiable characteristic
	@NotNull
	@NotBlank
	private Literal label;
	
	// arbitrary length content description
	private Literal description;
	
	// an indexers dream !! They can be literal concepts and enriched easily
	private ArrayList<LiteralOrResource> keywords;
	
	// This are reachable URLs
	private String isShownAt, isShownBy;
	
	// The whole legal bla, unedited, from the source, mostly cc0
	private LiteralOrResource metadataRights;
	
	// rdf  .. Agent, Artist, Painter, Painting, Series
	private String rdfType;
	
	// URIs how this Resource is known elsewhere
	private ArrayList<String> sameAs;
	
	// in a timeline where would this resource appear
	private int year;	
	
	// alternative title or name or placename
	private ArrayList<Literal> altLabels;

	public Literal getLabel() {
		return label;
	}

	public void setLabel(Literal label) {
		this.label = label;
	}

	public Literal getDescription() {
		return description;
	}

	public void setDescription(Literal description) {
		this.description = description;
	}

	public ArrayList<LiteralOrResource> getKeywords() {
		return keywords;
	}

	public void setKeywords(ArrayList<LiteralOrResource> keywords) {
		this.keywords = keywords;
	}

	public String getIsShownAt() {
		return isShownAt;
	}

	public void setIsShownAt(String isShownAt) {
		this.isShownAt = isShownAt;
	}

	public String getIsShownBy() {
		return isShownBy;
	}

	public void setIsShownBy(String isShownBy) {
		this.isShownBy = isShownBy;
	}

	public LiteralOrResource getMetadataRights() {
		return metadataRights;
	}

	public void setMetadataRights(LiteralOrResource metadataRights) {
		this.metadataRights = metadataRights;
	}

	public String getRdfType() {
		return rdfType;
	}

	public void setRdfType(String rdfType) {
		this.rdfType = rdfType;
	}

	public ArrayList<String> getSameAs() {
		return sameAs;
	}

	public void setSameAs(ArrayList<String> sameAs) {
		this.sameAs = sameAs;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public ArrayList<Literal> getAltLabels() {
		return altLabels;
	}

	public void setAltLabels(ArrayList<Literal> altLabels) {
		this.altLabels = altLabels;
	}
}
