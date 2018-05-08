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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import model.basicDataTypes.ILiteral;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithDate;
import play.Logger;
import play.Logger.ALogger;
import sources.core.Utils;

public class StringUtils {
	
	public static final ALogger log = Logger.of( StringUtils.class );
	
	private static final String[] DATE_FORMATS = new String[] { "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy/MM/dd",
			"dd MMM yyyy", "dd MMMM yyyy", "yyyyMMddHHmm", "yyyyMMdd HHmm", "dd-MM-yyyy HH:mm", "yyyy-MM-dd HH:mm",
			"MM/dd/yyyy HH:mm", "yyyy/MM/dd HH:mm", "dd MMM yyyy HH:mm", "dd MMMM yyyy HH:mm", "yyyyMMddHHmmss",
			"yyyyMMdd HHmmss", "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "MM/dd/yyyy HH:mm:ss",
			"yyyy/MM/dd HH:mm:ss", "dd MMM yyyy HH:mm:ss", "dd MMMM yyyy HH:mm:ss", "yyyyMMdd"

	};

	public static Date parseDate(String date) {
		return parseDate(date, DATE_FORMATS);
	}

	public static Date parseDate(String dateString, String... formats) {
		Date date = null;
		boolean success = false;

		for (int i = 0; i < formats.length; i++) {
			String format = formats[i];
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);

			try {
				// parse() will throw an exception if the given dateString
				// doesn't match
				// the current format
				date = dateFormat.parse(dateString);
				success = true;
				break;
			} catch (ParseException e) {
				// don't do anything. just let the loop continue.
				// we may miss on 99 format attempts, but match on one format,
				// but that's all we need.
			}
		}

		return date;
	}

	public static List<Year> getYears(List<String> dates) {
		ArrayList<Year> res = new ArrayList<Year>();
		if (dates != null) {
			for (String string : dates) {
				try {
					long v = Long.parseLong(string);
					if (v < 9999)
						res.add(Year.of((int) v));
					else {
						Calendar c = Calendar.getInstance();
						c.setTimeInMillis(v);
						res.add(Year.of(c.get(Calendar.YEAR)));
					}

				} catch (Exception e) {
					DateFormat f = DateFormat.getDateInstance();
					Date d = parseDate(string);
					Calendar c = Calendar.getInstance();
					c.setTime(d);
					res.add(Year.of(c.get(Calendar.YEAR)));
				}
			}
		}
		return res;
	}

	public static List<WithDate> getDates(List<String> dates) {
		ArrayList<WithDate> res = new ArrayList<>();
		if (dates != null) {
			for (String string : dates) {
				res.add(new WithDate(string));
			}
		}
		return res;
	}

	public static List<WithDate> getYearsDate(String... dates) {
		return getDates(Arrays.asList(dates));
	}
	
	private static LanguageDetector languageDetector;

	public static LanguageDetector getLanguageDetector(){
		if (languageDetector==null){
			List<LanguageProfile> languageProfiles;
			try {
				languageProfiles = new LanguageProfileReader().readAll();
			
			//build language detector:
			languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
			        .withProfiles(languageProfiles).minimalConfidence(0.8)
			        .build();
			} catch (IOException e) {
				log.warn("problems loading language detector", e );
			}
		}
		return languageDetector;
	}

	public static List<Language> getLanguages(String text, double confidenceTH){
		boolean shortText = text.length()<100;
		// create a text object factory
		TextObjectFactory textObjectFactory = shortText ?
					CommonTextObjectFactories.forDetectingShortCleanText()
				:
					CommonTextObjectFactories.forDetectingOnLargeText();
		// query:
		TextObject textObject = textObjectFactory.forText(text);
		List<DetectedLanguage> probabilities = StringUtils.getLanguageDetector().getProbabilities(textObject);
		List<Language> res = new ArrayList<>();
        for (DetectedLanguage language : probabilities) {
			if (language.getProbability()>=confidenceTH)
			res.add(Language.getLanguage(language.getLanguage()));
		}
        return res;
	}
	
	public static List<Language> getLanguages(String text){
		return getLanguages(text, ILiteral.THRESHOLD);
	}
	

	public static void main(String[] args) {
		 Literal l = new Literal();
		 l.addSmartLiteral("hello world. this is an english text. Maybe?");
		 System.out.println(l.toString());
	}

	public static int count(String text, String subtext) {
		int cursor = 0;
		int count = 0;
		int pos = -1;
		do {
			pos = text.indexOf(subtext, cursor);
			if (pos >= 0) {
				count++;
				cursor = pos;
			}
		} while (pos >= 0);
		return count;
	}

	public static LiteralOrResource toLiteralOrResource(String value) {
		// UrlValidaror v;
		return null;
	}
	
	public static MultiLiteralOrResource getLiteralLanguages(Language... lang){
		MultiLiteralOrResource res = new MultiLiteralOrResource();
		for (Language language : lang) {
			if (language!=null)
			res.addLiteral(Language.EN, language.getName());
		}
		res.fillDEF();
		return res;
	}

	public static Point getPoint(String stringValue) {
		if (Utils.hasInfo(stringValue)){
			List<Double> coordinates = new ArrayList<>();
			for (String coo : stringValue.split("[,\\s]+")) {
				if (Utils.hasInfo(coo))
					coordinates.add(new Double(coo));
			}
			Point res = GeoJson.point(coordinates.get(0), coordinates.get(1));
			return res;
		} else return null;
	}
	public static Point getPoint(String lati, String longi) {
		if (Utils.isNumericDouble(lati) && Utils.isNumericDouble(longi)) {
			Point res = GeoJson.point(new Double(lati), new Double(longi));
			return res;
		} 
		else return null;
	}
}
