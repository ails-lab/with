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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import views.html.main;

public class StringUtils {

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

	public static List<Year> getYears(String... dates) {
		return getYears(Arrays.asList(dates));
	}

	public static void main(String[] args) {
		System.out.println(getYears("2011 BC"));
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

}
