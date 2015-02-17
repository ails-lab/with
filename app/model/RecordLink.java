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


// This is just for embedding, won't have its own id
// there is an option Record link if the link is already materialized
public class RecordLink {
	
	// optional link to the materialized Record
	private Record recordReference;
	
	// which backend provided this entry
	private String source;
	
	// an optional URL for the thumbnail
	private String thumbnailUrl;
	
	// an optional cached version of a thumbnail for this record
	private Media thumbnail;
	
	private String title;
	private String description;
	
	// the id in the source system
	private String sourceId;
	// a link to the record on its source
	private String sourceUrl;
}
