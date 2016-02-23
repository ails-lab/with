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


package model.basicDataTypes;

import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import db.converters.ProvenanceInfoConverter;

@Converters(ProvenanceInfoConverter.class)
@Indexes({
		@Index(fields = @Field(value = "provider", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "resourceId", type = IndexType.ASC), options = @IndexOptions()) })
public class ProvenanceInfo {

	public enum Sources {
		Mint("Mint"), Europeana("Europeana"), UploadedByUser("UploadedByUser"),
		BritishLibrary("BritishLibrary"),
		InternetArchive("Internet Archive"), DDB("DDB"),
		DigitalNZ("DigitalNZ"), DPLA("DPLA"), EFashion("EFashion"), NLA("NLA"),
		Rijksmuseum("Rijksmuseum");
		
		private final String text;

		private Sources(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}

	}

	private String provider;
	private String uri;
	private String resourceId;

	public ProvenanceInfo() {
	}

	public ProvenanceInfo(String provider) {
		this.provider = provider;
	}

	public ProvenanceInfo(String provider, String uri, String recordId) {
		this.provider = provider;
		this.resourceId = recordId;
		this.uri = uri;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String recordId) {
		this.resourceId = recordId;
	}

	// you can have entries for WITH records with provider "WITH" and
	// recordId the ObjectId of the annotated Record
}