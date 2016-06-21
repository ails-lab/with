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


package sources.core;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.validator.routines.UrlValidator;

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.resources.WithResource;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

public class Utils {
	public static final ALogger log = Logger.of( Utils.class );
	
	public static class Lang {
		public Lang(String languageCode, String textValue) {
			this.lang = languageCode;
			this.value = textValue;
		}

		public Lang() {
			super();
		}

		public String lang;
		public String value;
	}

	public final static Comparator<WithResource<?,?>> compareThumbs = new Comparator<WithResource<?,?>>() {
		public int compare(WithResource<?,?> o1, WithResource<?,?> o2) {
			return getRank1(o2) - getRank1(o1);
		}

		private int getRank(WithResource<?, ?> o1) {
			EmbeddedMediaObject f = o1.getMedia().get(0).get(MediaVersion.Thumbnail);
			if (!Utils.hasInfo(f.getUrl()) || !Utils.isValidURL(f.getUrl())){
				return -1;
			} else {
				return (int)f.getSize();
			}
		}
		
		private int getRank1(WithResource<?, ?> o1) {
			EmbeddedMediaObject f = o1.getMedia().get(0).get(MediaVersion.Thumbnail);
			if (!Utils.hasInfo(f.getUrl()) || !Utils.isValidURL(f.getUrl())){
				log.info("Rank: -1 for "+f.getUrl());
				return -1;
			} else {
				try{
					URL url = new URL(f.getUrl());
					final BufferedImage bi = ImageIO.read(url);
					int size = bi.getWidth()* bi.getHeight();
					log.info("Rank: "+size+" for "+f.getUrl());
					return size;
				} catch(Exception e) {
					log.info("Rank: -1 for "+f.getUrl());
					return -1;
				}
			}
		}
		
	};
	
	public final static Comparator<WithResource<?,?>> compareQuality = new Comparator<WithResource<?,?>>() {
		public int compare(WithResource<?,?> o1, WithResource<?,?> o2) {
			double d = o2.getQualityMeasure() - o1.getQualityMeasure();
			return (int)Math.signum(d);
		}
		
	};

	public static String toLower(String text) {
		if (text != null) {
			return text.toLowerCase();
		}
		return text;
	}

	public static String spacesFormatQuery(String text, String space) {
		return text.replaceAll("(\\s)+", space);
	}

	public static String spacesPlusFormatQuery(String text) {
		return text.replaceAll("(\\s)+", "+");
	}

	public static CommonQuery parseJson(JsonNode json) throws Exception {
		CommonQuery q1 = Json.fromJson(json, CommonQuery.class);
		return q1;
	}

	public static String replaceQuotes(String text) {
		String[] temp = text.split("\"");
		text = "";
		int b = 1;

		for (String s : temp) {
			text = text + s;
			if (b != temp.length) {
				text = text + "%22";
			}
			b++;
		}

		// NOTE: THESE WON'T WORK for some reason
		// url.replace("\"", "\\\"");
		// String.join("%20", temp);

		return text;
	}

	public static String readAttr(JsonNode json, String string, boolean force) throws Exception {
		return readAttr(json, string, force, null);
	}

	public static int readIntAttr(JsonNode json, String string, boolean force) throws Exception {
		return readIntAttr(json, string, force, 0);
	}

	public static String readAttr(JsonNode json, String string, boolean force, String def) throws Exception {
		if (json == null)
			return null;
		String res = json.path(string).asText();
		if (res == null) {
			if (force)
				throw new Exception("Missing " + string);
			else
				return null;
		}
		return res;
	}

	public static List<String> readArrayAttr(JsonNode json, String string, boolean force) throws Exception {
		if (json == null)
			return null;
		JsonNode a = json.path(string);
		if (a == null) {
			if (force)
				throw new Exception("Missing " + string);
			else
				return null;
		} else {
			List<String> res = new ArrayList<String>(a.size());
			if (a.isArray()) {
				for (int i = 0; i < a.size(); i++) {
					JsonNode jsonNode = a.get(i);
					res.add(getPlainString(jsonNode));
				}
			} else {
				res.add(a.asText());
			}
			return res;
		}
	}

	private static String getPlainString(JsonNode jsonNode) {
		if (jsonNode.isTextual())
			return jsonNode.textValue();
		else {
			return jsonNode.path(0).textValue();
		}
	}

	public static List<Lang> readLangAttr(JsonNode json, String string, boolean force) throws Exception {
		if (json == null)
			return null;
		JsonNode a = json.path(string);
		if (a == null) {
			if (force)
				throw new Exception("Missing " + string);
			else
				return null;
		} else {
			List<Lang> res = new ArrayList<Lang>(a.size());
			if (a.isArray()) {
				for (int i = 0; i < a.size(); i++) {
					res.add(new Lang(null, a.get(i).textValue()));
				}
			} else {
				res.add(new Lang(null, a.asText()));
			}
			return res;
		}
	}

	public static int readIntAttr(JsonNode json, String string, boolean force, int def) throws Exception {
		if (json == null)
			return def;
		String readAttr = readAttr(json, string, force, "" + def);
		if (readAttr == null || readAttr.equals(""))
			return def;
		// System.out.println("Int? " + readAttr);
		return Integer.parseInt(readAttr);
	}

	public static Pair<String> getAttr(String value, String attrName) {
		if (value != null)
			return new Pair<String>(attrName, value);
		return null;
	}

	public static Pair<String> getFacetsAttr(List<String> values, String NAME) {
		if (values != null) {
			return new Pair<String>(NAME, getORList(values));
		}
		return null;
	}

	public static Pair<String> getFacetsAttr(String value, String NAME) {
		if (value != null) {
			return new Pair<String>(NAME, value);
		}
		return null;
	}

	public static String getORList(List<String> values) {
		return getORList(values, true);
	}

	public static String getORList(List<String> values, boolean paren) {
		String res = "";
		if (values.size() > 1) {
			if (paren)
				res += "(";
			res += values.get(0);
			for (int i = 1; i < values.size(); i++) {
				res += " OR " + values.get(i);
			}
			if (paren)
				res += ")";
		} else {
			res += values.get(0);
		}
		return res;
	}

	public static String getStringList(List<String> values, String separator) {
		String res = "";
		if (values.size() > 1) {
			res += values.get(0);
			for (int i = 1; i < values.size(); i++) {
				res += separator + values.get(i);
			}
		} else {
			res += values.get(0);
		}
		return res;
	}

	public static class Pair<T> {
		public T first;
		public T second;

		public Pair() {
			super();
		}

		public Pair(T first, T second) {
			super();
			this.first = first;
			this.second = second;
		}

		public String getHttp() {
			return getHttp(true);
		}
		
		public String getHttp(boolean encode) {
			String string = first + "=" + second.toString();
			if (encode){
				try {
					String encoded = URLEncoder.encode(second.toString(), "UTF-8");
					string = first + "=" + encoded;
				} catch (UnsupportedEncodingException e) {
					log.error( "",e );
				}
			}
			return string;
		}


	}

	public static class LongPair<T> extends Pair<T> {

		public LongPair() {
			super();
		}

		public LongPair(T first, T second) {
			super(first, second);
		}

		public String getHttp() {
			try {
				return first + "=%22" + URLEncoder.encode(second.toString(), "UTF-8") + "%22";
			} catch (UnsupportedEncodingException e) {
				log.error("",e);
				return first + "=%22" + second.toString() + "%22";
			}
		}
	}

	public static JsonNode findNode(JsonNode path, Pair<String>... pair) {
		boolean found;
		// System.out.println(path);
		for (JsonNode node : path) {
			found = true;
			for (Pair<String> p : pair) {
				String value = node.path(p.first).asText();
				// System.out.println(value + "?=" + p.second);
				if (!p.second.contains(value)) {
					found = false;
					break;
				}
			}
			if (found) {
				// System.out.println("got it");
				return node;
			}
		}
		return null;
	}

	public static boolean hasAny(String term) {
		return term != null && !term.equals("");
	}

	public static boolean isValidURL(String str) {
		UrlValidator val = new UrlValidator();
		return str!=null && val.isValid(str);
	}
	
	public static String decodeURL(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("",e);
		}
		return str;
	}

	public static boolean isNumericInteger(String date) {
		return date!=null && date.matches("[-]{0,1}[0-9]+");
	}
	
	public static boolean isNumericDouble(String date) {
		return date!=null && date.matches("[-]{0,1}[0-9]+[\\.]{0,1}[0-9]*");
	}

	public static boolean hasInfo(String string) {
		return !(string==null || string.equals("") || string.matches("[\\s]*") || string.equals("null"));
	}
	
	public static boolean hasInfo(JsonNode node) {
		return !(node==null || node.isMissingNode() || (node.isArray() && node.size()==0));
	}
	
	public static <T> boolean hasInfo(T[] array) {
		return !(array==null || array.length==0 );
	}
	
	public static <T> boolean hasInfo(Collection<T> array) {
		return !(array==null || array.isEmpty());
	}

	public static <T> List<T> asList(T... a) {
		ArrayList<T> list = new ArrayList<>();
		for (T o : a) {
			if (o!=null)
				list.add(o);
		}
		return list;
	}

	public static <K,T> boolean hasInfo(HashMap<K,T> res) {
		return res!=null && !res.isEmpty();
	}
	
	public static List<String> parseArray(String[] array){
		ArrayList<String> res = new ArrayList<>();
		for (String string : array) {
			for (String string1 : string.split("\\s*,\\s*")) {
				if (Utils.hasInfo(string1)){
					res.add(string1);
				}
			}
		}
		return res;
	}

}
