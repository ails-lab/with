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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import espace.core.sources.BritishLibrarySpaceSource;
import espace.core.sources.DDBSpaceSource;
import espace.core.sources.DPLASpaceSource;
import espace.core.sources.DigitalNZSpaceSource;
import espace.core.sources.EuropeanaFashionSpaceSource;
/*import espace.core.sources.BritishLibrarySpaceSource;
import espace.core.sources.DDBSpaceSource;
import espace.core.sources.DPLASpaceSource;
import espace.core.sources.DigitalNZSpaceSource;
import espace.core.sources.EuropeanaFashionSpaceSource;
import espace.core.sources.NLASpaceSource;
import espace.core.sources.RijksmuseumSpaceSource;
import espace.core.sources.YouTubeSpaceSource;*/
import espace.core.sources.EuropeanaSpaceSource;
import espace.core.sources.NLASpaceSource;
import espace.core.sources.RijksmuseumSpaceSource;
import espace.core.sources.WithSpaceSource;
import espace.core.sources.ElasticSource;
import espace.core.sources.YouTubeSpaceSource;


public class ESpaceSources {

	public static List<ISpaceSource> esources;

	static void init() {
		esources = new ArrayList<ISpaceSource>();
		esources.add(new EuropeanaSpaceSource());
		esources.add(new DPLASpaceSource());
		esources.add(new NLASpaceSource());
		esources.add(new DigitalNZSpaceSource());
		esources.add(new EuropeanaFashionSpaceSource());
		esources.add(new YouTubeSpaceSource());
		esources.add(new ElasticSource());
		esources.add(new RijksmuseumSpaceSource());
		esources.add(new DDBSpaceSource());
		esources.add(new BritishLibrarySpaceSource());
		esources.add(new WithSpaceSource());
		Logger.info("Initialization of sources list");
	}

	public static Map<String, ISpaceSource> initSourceByNameMap() {
		Map<String, ISpaceSource> sourcesMap = new HashMap<String, ISpaceSource>();
		ISpaceSource s = new EuropeanaSpaceSource();
		sourcesMap.put(s.getSourceName(), s);
		/*s = new DPLASpaceSource();
		sourcesMap.put(s.getSourceName(), s);
		s = new NLASpaceSource();
		sourcesMap.put(s.getSourceName(), s);
		s = new DigitalNZSpaceSource();
		sourcesMap.put(s.getSourceName(), s);
		s = new EuropeanaFashionSpaceSource();
		sourcesMap.put(s.getSourceName(), s);
		s = new YouTubeSpaceSource();
		sourcesMap.put(s.getSourceName(), s);*/
		s = new WithSpaceSource();
		sourcesMap.put(s.getSourceName(), s);
		return sourcesMap;
	}

	public static List<ISpaceSource> getESources() {
		if (esources == null) {
			init();
		}
		return esources;

	}

	public static List<SourceResponse> fillResults(final CommonQuery q) {
		final ArrayList<SourceResponse> srcs = new ArrayList<SourceResponse>();
		ArrayList<Thread> t = new ArrayList<Thread>();
		for (final ISpaceSource src : ESpaceSources.getESources()) {
			if ((q.source == null) || (q.source.size() == 0) || q.source.contains(src.getSourceName())) {
				Thread tit = new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						SourceResponse results = src.getResults(q);
						synchronized (srcs) {
							srcs.add(results);
							System.out.println(results.source + " found " + results.count);
						}
					}
				});
				t.add(tit);
				tit.start();
			}
		}
		for (Thread thread : t) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return srcs;
	}

}
