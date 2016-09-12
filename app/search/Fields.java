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


package search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * In this enum we collect all the fieldnames we allow as queries. There should be some documentation provided
 * @author Arne Stabenau
 *
 */
public enum Fields {
	anywhere, // when you look for anything, not part of an answer, any source interprets this differently
	resourceType, // the WITH resource type 
	// for the within search, this might be useful
	administrative_isPublic,
	administrative_access, // shortcut field for an entry in any other access field
	administrative_access_READ, // userids are in here
	administrative_access_WRITE,
	administrative_access_OWN,
	
	administrative_created, // date when this record was created
	administrative_lastModified, //
	administrative_externalId, // usually the id in the last provenance entry
	administrative_withURI, //
	
	// the collections this is in
	collectedIn,
	
	// this should allow to search for records inside a persons collections, a usergroups collection a space
	// access cannot do this, since public records don't have access for user ids
	administrative_collectedBy, // shortcut field for any other field
	administrative_collectedBy_READ, // which users have this record in READ collections
	administrative_collectedBy_WRITE, // in WRITE collections   
	administrative_collectedBy_OWN, //  in OWN collections
	
	// provenance info
	provenance_provider,
	provenance_uri,
	provenance_resourceId,
	
	// dataprovider, source 
	provenance_dataprovider, // an extra field for the first provider in the provenance chain
	provenance_source, // the last provider in the provenance chain
	
	// there is potentially more than one media in the resource, so the search will be not accurate if you combine 
	// attributes together (like withRights and quality .. can't search for free and high quality <b> records <b>
	// a separate media index obviously can do that
	media_type,
	media_withRights,
	media_quality, // needs some nice enum values
	media_mimeType,
	
	usage_tags,
	
 	//
	// descriptive data fields, first the basics
	//
	
	descriptiveData_label, // supports language
	descriptivedata_description,
	descriptiveData_keywords,
	descriptiveData_isShownAt, // this is mostly useful as exact match query
	descriptiveData_isShownBy,
	descriptiveData_rdfType, 
	descriptiveData_sameAs,
	descriptiveData_dates,
	descriptiveData_altLabels,
	descriptiveData_country,
	descriptiveData_city,
	descriptiveData_coordinates,
	descriptiveData_metadataQuality,
	
	//
	// Now some extensions for CulturalObjects
	//
	descriptiveData_dcidentifier, // supports language, without language, searches everything and the uri
	descriptiveData_dcidentifier_uri, // can be uri, 
	
	descriptiveData_dclanguage, // supports language (free text language value in different languages )
	descriptiveData_dclanguage_uri, // this is the language of the object (if it has), not of the metadata
	
	descriptiveData_dctype,
	descriptiveData_dctype_uri,
	
	// places or times
	descriptiveData_dccoverage,
	descriptiveData_dccoverage_uri,
	
	descriptiveData_dcrights,
	descriptiveData_dcrights_uri,
	
	// places are here
	descriptiveData_dctermsspatial,
	descriptiveData_dctermsspatial_uri,
	
	descriptiveData_dccreator,
	descriptiveData_dccreator_uri,
	
	descriptiveData_dccontributor,
	descriptiveData_dccontributor_uri,
	
	descriptiveData_dccreated_free,
	descriptiveData_dccreated_year,
	
	descriptiveData_dcdate_free,
	descriptiveData_dcdate_year,
	
	//TODO: do we want multilinguality for dcformat and dctermsmedium?
	descriptiveData_dcformat,
	descriptiveData_dcformat_uri,
	
	descriptiveData_dctermsmedium,
	descriptiveData_dctermsmedium_uri,

	descriptiveData_isRelatedTo, //no idea if we expect uris or literals

	// there are more event fields ...
	descriptiveData_events_CREATED_agent,
	descriptiveData_events_CREATED_place;
	
	
	//
	// some jackson magic to get fieldIds correct
	//
	@JsonValue
	public final String fieldId() {
	 	return this.name().replace("_", ".");
	}
	@JsonCreator
	public static Fields forFieldId( String fieldId ) {
		String fieldName = fieldId.replace(".", "_");
		return Fields.valueOf(fieldName);
	}
}

