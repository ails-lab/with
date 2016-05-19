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
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.jena.atlas.logging.Log;
import org.elasticsearch.metrics.ElasticsearchReporter;

import play.Logger;
import play.Logger.ALogger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import elastic.Elastic;

public class MetricsUtils {

	public static final ALogger log = Logger.of(MetricsUtils.class);

	static public final MetricRegistry registry = new MetricRegistry();
	static public final CsvReporter reporter = CsvReporter.forRegistry(registry)
            .formatFor(Locale.US)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build(new File("/home/yiorgos/"));


	public static ElasticsearchReporter getESReporter() {
	ElasticsearchReporter es_reporter = null;
	try {
		es_reporter = ElasticsearchReporter.forRegistry(registry)
				.hosts(Elastic.host+":"+ (Elastic.port-100))
				.timeout(3000)
				.build();
	} catch (IOException e) {
		log.error(e.getMessage());
	}
	return es_reporter;
	}

	public static void dummyESMeter() {
		final Counter incomingRequestsMeter = registry.counter("dummy");
		incomingRequestsMeter.inc();
	}

	static public final Graphite graphite = new Graphite(new InetSocketAddress("collab.image.ntua.gr", 80));
	static public final GraphiteReporter gr_reporter = GraphiteReporter.forRegistry(registry)
	                                                  .prefixedWith("withculture.image.gr")
	                                                  .convertRatesTo(TimeUnit.SECONDS)
	                                                  .convertDurationsTo(TimeUnit.MILLISECONDS)
	                                                  .filter(MetricFilter.ALL)
	                                                  .build(graphite);

}
