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

public class LiteralOrResource extends Literal {
	// resources we do understand about and can process further (or not)
	// uri being general and difficult to process
	public static enum ResourceType {
		uri, skos, dbpedia, getty, wikidata, geodata, gemet, withRepository
	}
			
	// etc etc		
	public String getResource( ResourceType resourceType ) {
		if( get( "uriType").equals( resourceType.toString())) 
			return get( "uri");
		else 
			return "";
	}
	
	public void setResource( ResourceType resourceType, String uri ) {
		put( "uriType", resourceType.toString());
		put( "uri", uri );
	}
}