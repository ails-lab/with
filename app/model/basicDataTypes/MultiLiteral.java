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

public class MultiLiteral extends KeyValuesPair<String> {

	public MultiLiteral() {
		super();
	}

	public MultiLiteral(String label) {
		super();
		addLiteral(label);
	}

	public MultiLiteral(Language lang, String label) {
		super();
		addLiteral(lang, label);
	}

	public void addLiteral(Language lang, String value) {
		add(lang.toString(), value);
		if (lang.equals(Language.EN) && !this.containsKey(Language.DEF.toString()))
			add(Language.DEF.toString(), value);
	}

	public void addLiteral(String value) {
		addLiteral(Language.UNKNOWN, value);
	}
}