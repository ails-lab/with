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


package model.annotations.targets;

public class AnnotationTarget {

	/**
	 * The withURI to which the annotation refers.
	 */
	String withURI;
	
	/**
	 * The external id of the object to which the annotations refers.
	 */
	String externalId;

	public String getWithURI() {
		return withURI;
	}

	public void setWithURI(String withURI) {
		this.withURI = withURI;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

}
