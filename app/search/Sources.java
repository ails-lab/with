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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import play.Logger;
import search.ProxySource.BritishLibProxySource;
import search.ProxySource.DDBProxySource;
import search.ProxySource.DPLAProxySource;
import search.ProxySource.DigitalNZProxySource;
import search.ProxySource.EuropeanaFashionProxySource;
import search.ProxySource.EuropeanaProxySource;
import search.ProxySource.HistoryPinProxySource;
import search.ProxySource.InternetArchiveProxySource;
import search.ProxySource.NLAProxySource;
import search.ProxySource.RijksProxySource;
import search.ProxySource.YoutubeProxySource;
import sources.ElasticSource;

/**
 * In here are sources that are backend search engines.
 * @author stabenau
 *
 */
public enum Sources {
	Europeana(EuropeanaProxySource.class, "Europeana"),
	BritishLibrary(BritishLibProxySource.class, "The British Library"),
	InternetArchive(InternetArchiveProxySource.class, "Internet Archive"),
	DDB(DDBProxySource.class,"Deutsche Digitale Bibliothek"),
	DigitalNZ(DigitalNZProxySource.class,"DigitalNZ"),
	DPLA(DPLAProxySource.class,"Digital Public Library of America"),
	EuropeanaFashion(EuropeanaFashionProxySource.class,"EuropeanaFashion"),
	YouTube(YoutubeProxySource.class,"Youtube"),
	NLA(NLAProxySource.class,"National Library of Australia"),
	WITHin(ElasticSource.class,"WITH Collections"),
	Rijksmuseum(RijksProxySource.class,"Rijksmuseum"),
	Historypin(HistoryPinProxySource.class,"Historypin")
	;
	 
	
    @JsonCreator
    public static Sources forValue(String sourceID) {
    	return Sources.getSourceByID(sourceID);
    }
    
    @JsonValue
    public String toValue(Sources s) {
    	return s.getID();	
    }


	private final String sourceName;
	private final String sourceID;

	private Class<? extends Source> driver;
	private Sources( Class<? extends Source> driver, String id, String text) {
		this.driver = driver;
		this.sourceName = text;
		this.sourceID = id;
	}

	private Sources( Class<? extends Source> driver, String id) {
		this(driver,id,id);
	}

	public Source getDriver() {
		try {
			return driver.newInstance();
		} catch( Exception e ) {
			Logger.of( Sources.class )
				.error( "Fatal error, source instance cannot be created" );
			throw new RuntimeException( e );
		}
	}

	@Override
	public String toString() {
		return sourceID;
	}

	public String getText() {
		return sourceName;
	}

	public String getID() {
		return sourceID;
	}

	public static Sources getSourceByID(String id){
		for (Sources e : Sources.values()) {
			if (e.getID().equals(id) || e.getText().equals(id)){
				return e;
			}
		}
		return null;
	}


}
