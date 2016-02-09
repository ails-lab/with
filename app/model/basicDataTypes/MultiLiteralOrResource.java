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

import java.util.List;

import org.mongodb.morphia.annotations.Converters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import utils.Deserializer.MultiLiteralOrResourceDesiarilizer;
import db.converters.MultiLiteralOrResourceConverter;
import sources.core.Utils;

@Converters(MultiLiteralOrResourceConverter.class)

@JsonDeserialize(using = MultiLiteralOrResourceDesiarilizer.class)
public class MultiLiteralOrResource extends MultiLiteral {

	public MultiLiteralOrResource() {
	}

	public MultiLiteralOrResource(Language lang, String label) {
		super(lang, label);
	}

	public MultiLiteralOrResource(String label) {
		if (Utils.isValidURL(label)) {
			addURI(label);
		} else
			addLiteral(label);
	}

	public void addLiteral(Language lang, String value) {
		if (Utils.isValidURL(value))
			addURI(value);
		else
			super.addLiteral(lang, value);
	}

	public void addURI(String uri) {
		add(LiteralOrResource.URI, uri);
	}

	public void addURI(List<String> uris) {
		for (String uri : uris) {
			addURI(uri);
		}
	}
	
	public MultiLiteralOrResource merge(MultiLiteral other) {
		for (String k : other.keySet()) {
			List<String> value = other.get(k);
			for (String string : value) {
				this.add(k, string);
			}
		}
		return this;
	}

}
