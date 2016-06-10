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


package general;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.*;

public class MetricsTest {

	static final MetricRegistry metrics = new MetricRegistry();
	private static final Histogram responseSizes = metrics.histogram("test");
	public static void main(String[] args) {
		startReport();
		metrics.name(MetricsTest.class, "example", "metrics");
		metrics.name(MetricsTest.class, "example", "metrics-2");
		Meter requests = metrics.meter("requests");
		responseSizes.update(500);
		requests.mark();
		wait5Seconds();
		responseSizes.update(500);

		requests.mark();
		wait5Seconds();
	}

	static void startReport() {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
				.filter(MetricFilter.ALL)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(1, TimeUnit.SECONDS);
	}

	static void wait5Seconds() {
		try {
			Thread.sleep(1 * 1000);
		} catch (InterruptedException e) {
		}
	}

}
