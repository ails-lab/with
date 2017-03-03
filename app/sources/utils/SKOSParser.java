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


package sources.utils;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import model.basicDataTypes.*;
import sources.core.ApacheHttpConnector;
import sources.core.HttpConnector;

public class SKOSParser {

	public List<Literal> parseConcept(String url) {
		List<Literal> res = new ArrayList<>();
		HttpConnector con = ApacheHttpConnector.getApacheHttpConnector();
		try {
			JsonNode urlContent = con.getURLContent(url);
			JsonContextRecord tj = new JsonContextRecord(urlContent, '$');
			res.add(tj.getLiteralValue(true, "[.*]$http://www.w3.org/2004/02/skos/core#prefLabel"));
		} catch (Exception e) {

		}
		return res;
	}

	public static void main(String[] args) {
		SKOSParser p = new SKOSParser();
		List<Literal> l = p.parseConcept("http://id.loc.gov/vocabulary/iso639-1/en");
		System.out.println(l);
	}

}
