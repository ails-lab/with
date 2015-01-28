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


package espace.core;

import sun.security.pkcs.ParsingException;

import com.fasterxml.jackson.databind.JsonNode;

public class Utils {

	public static String spacesFormatQuery(String text) {
		return text.replaceAll("(\\s)+", "+");
	}

	// public static <T> T gsonLoad(String filepath, Class<T> className) {
	// try (Reader reader = new InputStreamReader(new
	// FileInputStream(filepath))) {
	// Gson gson = new GsonBuilder().create();
	// return gson.fromJson(reader, className);
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return null;
	// }
	//
	// public void gsonWrite(String filepath, Object object) {
	// try (Writer writer = new OutputStreamWriter(new
	// FileOutputStream(filepath))) {
	// Gson gson = new GsonBuilder().create();
	// gson.toJson(object, writer);
	// } catch (UnsupportedEncodingException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public static void parseJson(JsonNode json, CommonQuery q) throws ParsingException {
		q.query = readAttr(json, "searchTerm", true);
		q.page = Integer.parseInt(readAttr(json, "page", false, "1"));
		q.pageSize = Integer.parseInt(readAttr(json, "pageSize", false, "20"));
	}

	public static String readAttr(JsonNode json, String string, boolean force) throws ParsingException {
		return readAttr(json, string, force, null);
	}

	public static String readAttr(JsonNode json, String string, boolean force, String def) throws ParsingException {
		String res = json.findPath(string).asText();
		if (res == null) {
			if (force)
				throw new ParsingException("Missing " + string);
			else
				return null;
		}
		return res;
	}

}
