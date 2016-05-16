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


package controllers.parameterTypes;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.mvc.QueryStringBindable;

public class StringTuple implements QueryStringBindable<StringTuple>{
	public static final ALogger log = Logger.of(StringTuple.class);

	public String x = null; 
	public String y = null; 

	// play framework requires the Bindable to provide a "no Argument" public constructor.
	public StringTuple() {
	}
	
	@Override
	public Option<StringTuple> bind(String key, Map<String, String[]> data) {
		String[] vs = data.get(key);
	    if (vs != null && vs.length > 0) {
	        String v = vs[0];
	        ObjectMapper mapper = new ObjectMapper(); 
	        JsonNode actualObj;
			try {
				actualObj = mapper.readTree(v);
				//if (actualObj.has("username") && actualObj.has("access")) {
				if (actualObj.size() == 2) {
					Iterator<String> iterator = actualObj.fieldNames();
					x = actualObj.get(iterator.next()).asText();
					y = actualObj.get(iterator.next()).asText();
					return Option.Some(this);
		        }
		        else 
		        	return Option.None();
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				log.error("",e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error("",e);
			}
	    }
	    return Option.None();
	}
	@Override
	public String javascriptUnbind() {
		return "function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)}";
	}
	@Override
	public String unbind(String key) {
		return "{ 'x': " + x + ", 'y: '" + y + "}";
	}
}
