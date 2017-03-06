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

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

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

	public List<Literal> parseConcept1(String url) {
		List<Literal> res = new ArrayList<>();
		HttpConnector con = ApacheHttpConnector.getApacheHttpConnector();
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(new URL(url + ".rdf"));
			Element root = document.getRootElement();
			// iterate through child elements of root with element name "foo"
			List selectNodes = root.selectNodes("//RDF/Description");
			for (Iterator i = selectNodes.iterator(); i.hasNext();) {
				Element node = (Element) i.next();
				// do something
				// rdf:resource
				System.out.println(node.valueOf("@rdf:resource"));
				if (url.equals(node.valueOf("@rdf:resource"))) {
					for (Iterator i2 = root.elementIterator("skos:prefLabel"); i2.hasNext();) {
						Element translation = (Element) i2.next();
						Language language = Language.getLanguage(translation.attributeValue("@xml:lang"));
						res.add(new Literal(language, translation.getText()));
					}
				}
			}

			// JsonContextRecord tj = new JsonContextRecord(urlContent, '$');
			// res.add(tj.getLiteralValue(true,
			// "[.*]$http://www.w3.org/2004/02/skos/core#prefLabel"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public static void main(String[] args) {
		SKOSParser p = new SKOSParser();
		List<Literal> l = p
				.parseConcept1("http://bib.arts.kuleuven.be/photoVocabulary/en/concepts/-photoVocabulary-30159");
		System.out.println(l);
	}

}
