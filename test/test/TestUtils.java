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


package test;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import play.Logger;
import play.Logger.ALogger;


public class TestUtils {
	public static final ALogger log = Logger.of(TestUtils.class);

	public static Random r = new Random();
	
	public static String randomString() {
		char[] text = new char[50];
		for (int i = 0; i < 50; i++)
			text[i] = "abcdefghijklmnopqrstuvwxyz".charAt(r
					.nextInt(25));
		return text.toString();
	}

	public static Date randomDate( String start, String end ) {
		Date res = new Date();
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd hh:mm:ss");
			long startLong = dateFormat.parse(start).getTime();
			long endLong = dateFormat.parse(start).getTime();

			long middle = Math.round( r.nextDouble()*(endLong-startLong)) + startLong;
			res = new Date( middle );
			
		} catch (Exception e) {
			// log perhaps
		}
		return res;
	}
	
	public static Date randomDate() {
		return TestUtils.randomDate("2000-01-01 00:00:00", "2015-12-31 23:59:59");
	}

	public static Date randomDateYear( int yearStart, int yearEnd ) {
		String start = String.format( "%04d-01-01 00:00:00", yearStart);
		String end = String.format( "%04d-12-31 23:59:59", yearEnd );
		return randomDate( start, end );
	}
	
	public static MessageDigest getMD5Digest() {
		MessageDigest res  = null;
		try {
			res = MessageDigest.getInstance("MD5");
		} catch( Exception e ) {
			log.error( "No MD5 ????");
		}
		return res;
	}
	
	/*
	 * Pretty print json
	 */
	public static void jsonPrettyPrint(String json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(json);
		String pretty = gson.toJson(je);
		System.out.println(pretty);
	}



}
