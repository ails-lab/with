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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.libs.F.Option;
import play.mvc.QueryStringBindable;

public class MyPlayList implements QueryStringBindable<MyPlayList>{
	
	public List<StringTuple> list = new ArrayList<StringTuple>();

	// play framework requires the Bindable to provide a "no Argument" public constructor.
	public MyPlayList() {
	}
	
	@Override
	public Option<MyPlayList> bind(String key, Map<String, String[]> data) {
		String[] vs = data.get(key);
	    if (vs != null && vs.length > 0) {
	        String v = vs[0];
			try {
				JsonNode actualObj = new ObjectMapper().readTree(v);
				if (actualObj.isArray()) {
					for (final JsonNode o: actualObj) {
						String[] tupleValue = new String[1];
			        	tupleValue[0] = o.toString();
			        	HashMap<String, String[]> tupleMap = new HashMap<String, String[]>(1);
			        	tupleMap.put(key, tupleValue);
			        	StringTuple x = new StringTuple();
			        	Option<StringTuple> tuple = x.bind(key, tupleMap);
			        	if (tuple.isDefined()) {
			        		list.add(tuple.get());
			        	}
					}
					return Option.Some(this);
				}
				else
					return Option.None();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
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
		String listString = "[";
		HashMap<String, String[]> tupleMap = new HashMap<String, String[]>(1);
		for (int i=0; i <list.size(); i++){
			StringTuple st = list.get(i);
			listString+=st.unbind(key);
			if (i < list.size() -1)
				listString+=",";
		}
		return listString+"]";
	}
}
