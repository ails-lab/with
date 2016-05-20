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


package sources;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
import java.util.function.Function;

import play.Logger;
import play.Logger.ALogger;
import sources.core.QueryBuilder;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public class EuropeanaTermsCount {

	public static final ALogger log = Logger.of( EuropeanaTermsCount.class);
	
	public static String getHTML(String urlToRead) throws Exception {
	      StringBuilder result = new StringBuilder();
	      log.info("calling " + urlToRead);
	      URL url = new URL(urlToRead);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      log.info("result "+System.lineSeparator() + result.toString());
	      return result.toString();
	   }
	
	
	static String apiKey = "SECRET_KEY";
	public static void main(String[] args) throws Exception {
		Scanner s = new Scanner(System.in);
		while (s.hasNextLine()){
			String term = s.nextLine();
			Function<String, String> function = (String ss) -> {
				return "\"" + ss + "\"";
			};
			QueryBuilder builder = new QueryBuilder("http://europeana.eu/api/v2/search.json");
			builder.addSearchParam("wskey", apiKey);
			builder.addQuery("query", term);
			JsonContextRecord response = new JsonContextRecord(getHTML(builder.getHttp()));
			int count = response.getIntValue("totalResults");
			
			Collection<String> t = Arrays.asList("Europeana Food and Drink");
			builder.addSearchParam("qf", "PROVIDER:" + Utils.getORList(ListUtils.transform(t , function)));
			response = new JsonContextRecord(getHTML(builder.getHttp()));
			int count2 = response.getIntValue("totalResults");
		}
		s.close();
	}
}
