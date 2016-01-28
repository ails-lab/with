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

import com.google.common.base.Optional;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import play.Logger;
import sources.utils.StringUtils;

public interface ILiteral {
	

	void addLiteral(Language lang, String value);

	default void addLiteral(String value) {
		addLiteral(Language.UNKNOWN, value);
	}
	
	default void addSmartLiteral(String value) {
		boolean shortText = value.length()<20;
		// create a text object factory
		TextObjectFactory textObjectFactory = shortText ?
					CommonTextObjectFactories.forDetectingShortCleanText()
				:
					CommonTextObjectFactories.forDetectingOnLargeText();
		// query:
		TextObject textObject = textObjectFactory.forText(value);
		Optional<String> lang = StringUtils.getLanguageDetector().detect(textObject);
		if (lang.isPresent()) {
			addLiteral(Language.getLanguage(lang.get()), value);
			return;
		}
		Logger.warn("Unknown Language for text " + value);
		addLiteral(Language.UNKNOWN, value);
	}

}