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


package utils;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import db.DB;
import play.Logger;
import play.Logger.ALogger;

public class MetricsUtils {

	public static final ALogger log = Logger.of(MetricsUtils.class);

	static public final MetricRegistry registry = new MetricRegistry();
	static public final CsvReporter reporter = CsvReporter.forRegistry(registry)
            .formatFor(Locale.US)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build(new File(DB.getConf().getString("metrics.csv.directory")));

	public static void dummyESMeter() {
		final Counter incomingRequestsMeter = registry.counter("dummy");
		incomingRequestsMeter.inc();
	}

	//static public final Graphite graphite = new Graphite(new InetSocketAddress("collab.image.ntua.gr", 80));
	static public final Graphite graphite = new Graphite(new InetSocketAddress(
				DB.getConf().getString("metrics.graphite.host"),
				DB.getConf().getInt("metrics.graphite.port")));
	static public final GraphiteReporter gr_reporter = GraphiteReporter.forRegistry(registry)
	                                                  .prefixedWith(DB.getConf().getString("metrics.graphite.prefix"))
	                                                  .convertRatesTo(TimeUnit.SECONDS)
	                                                  .convertDurationsTo(TimeUnit.MILLISECONDS)
	                                                  .filter(MetricFilter.ALL)
	                                                  .build(graphite);

}
