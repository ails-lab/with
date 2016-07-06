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

import play.Logger;

/**
 * All potential query sources need an enum entry here.
 * 
 * The class that extends from Source and responsible for this Source is listed as parameter.
 * @author Arne Stabenau
 *
 */
public enum Sources {
	
	WITHin( EmptySource.class ), Europeana( EmptySource.class ), BritishLibrary( EmptySource.class );

	//....
	private Class<? extends Source> driver;
	private Sources( Class<? extends Source> driver ) {
		this.driver = driver;
	}
	
	public Source getDriver() {
		try {
			return (Source) driver.newInstance();
		} catch( Exception e ) {
			Logger.of( Sources.class )
				.error( "Fatal error, source instance cannot be created" );
			throw new RuntimeException( e );
		}
	}
}
