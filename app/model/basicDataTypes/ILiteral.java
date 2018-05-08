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

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import sources.core.Utils;
import sources.utils.StringUtils;
import utils.ListUtils;

public interface ILiteral {
	
	/**
	 * confidence for language detection
	 */
	public static double THRESHOLD = 0.99;

	void addLiteral(Language lang, String value);

	default void addLiteral(String value) {
		addLiteral(Language.UNKNOWN, value);
	}
	
	default void addSmartLiteral(String value, Language... suggestedLanguages) {
		if (!Utils.isValidURL(value)){
			if ((suggestedLanguages!=null && suggestedLanguages.length==1)){
				addLiteral(suggestedLanguages[0], value);
				return;
			}
			boolean shortText = value.length()<100;
			// create a text object factory
			TextObjectFactory textObjectFactory = shortText ?
						CommonTextObjectFactories.forDetectingShortCleanText()
					:
						CommonTextObjectFactories.forDetectingOnLargeText();
			// query:
			TextObject textObject = textObjectFactory.forText(value);
			List<DetectedLanguage> probabilities = StringUtils.getLanguageDetector().getProbabilities(textObject);
			
	        if (!probabilities.isEmpty()) {
	        	boolean gotSome = false;
	            for (DetectedLanguage detectedLanguage : probabilities) {
					if (	// is suggested
							(Utils.hasInfo(suggestedLanguages) && ListUtils.anyof(suggestedLanguages, 
							(Language l)-> l.matches(detectedLanguage.getLanguage())))
							||
							// or has a very high probability
							detectedLanguage.getProbability()>=THRESHOLD
							){
						addLiteral(Language.getLanguage(detectedLanguage.getLanguage()), value);
//						Logger.info("Detected ["+detectedLanguage.getLanguage()+"] for " + value);
						gotSome = true;
					}
				}
	            if (gotSome)
	            	return;
	        }
//			Logger.warn("Unknown Language for text " + value);
		}
		addLiteral(Language.UNKNOWN, value);
	}

}