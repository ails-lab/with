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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import play.Logger;
import play.Logger.ALogger;
import utils.Deserializer;
import utils.Serializer;

/**
 * Capture accurate and inaccurate dates in a visualisable way. Enable search
 * for year. This is a point in time. If you mean a timespan, use different
 * class.
 */
public class WithDate {
	public static final ALogger log = Logger.of(WithDate.class);

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date isoDate;
	// facet
	// year should be filled in whenever possible
	// 100 bc is translated to -100
	int year = Integer.MAX_VALUE;

	// controlled expression of an epoch "stone age", "renaissance", "16th
	// century"
	LiteralOrResource epoch;

	// if the year is not accurate, give the inaccuracy here( 0- accurate)
	int approximation;

	// mandatory, other fields are extracted from that
	String free;

	public WithDate() {
		super();
	}

	public Date getIsoDate() {
		return isoDate;
	}

	public void setIsoDate(Date d) {
		if (d != null) {
			this.isoDate = d;
			Calendar instance = Calendar.getInstance();
			instance.setTime(d);
			if (year == Integer.MAX_VALUE)
				year = instance.get(Calendar.YEAR);
		}
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public LiteralOrResource getEpoch() {
		return epoch;
	}

	public void setEpoch(LiteralOrResource epoch) {
		this.epoch = epoch;
	}

	public int getApproximation() {
		return approximation;
	}

	public void setApproximation(int approximation) {
		this.approximation = approximation;
	}

	public String getFree() {
		return free;
	}

	public void setFree(String free) {
		this.free = free;
	}

	public WithDate(String free) {
		super();
	}

	public void sanitizeDates() {
		// code to init the other Date representations
		boolean fillYear = (this.year == Integer.MAX_VALUE);
		boolean fillIsoDate = (this.isoDate == null);
		if (sources.core.Utils.isNumericInteger(this.free)) {
			if (fillYear)
				setYear(Integer.parseInt(this.free));
		} else if (this.free.matches("\\d+\\s+(bc|BC)")) {
			if (fillYear)
				setYear(Integer.parseInt(free.split("\\s")[0]));
		} else if (this.free.matches("\\d\\d\\d\\d-\\d\\d\\d\\d")) {
			if (fillYear)
				setYear(Integer.parseInt(free.split("-")[0]));
		} else if (this.free.matches("\\d\\d-\\d\\d-\\d\\d\\d\\d")) {
			try {
				if (fillIsoDate)
					setIsoDate((new SimpleDateFormat("dd-mm-yyyy")).parse(this.free));
			} catch (ParseException e) {
				log.warn("Parse Exception: " + this.free);
			}
		} else if (this.free.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d")) {
			try {
				if (fillIsoDate)
					setIsoDate((new SimpleDateFormat("yyyy-mm-dd")).parse(this.free));
			} catch (ParseException e) {
				log.warn("Parse Exception: " + this.free);
			}
		} else if (sources.core.Utils.isValidURL(this.free)) {
			this.epoch = new LiteralOrResource(this.free);
		} else {
			log.warn("unrecognized date: " + this.free);
		}
	}

}
