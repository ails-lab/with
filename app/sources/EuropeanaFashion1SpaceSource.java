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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import search.FiltersFields;
import search.Sources;
import sources.core.ApacheHttpConnector;
import sources.core.CommonFilter;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.SourceResponse;
import sources.core.Utils.Pair;
import sources.utils.FunctionsUtils;

public class EuropeanaFashion1SpaceSource extends EuropeanaSpaceSource {


	public EuropeanaFashion1SpaceSource() {
		super();
	}
	
	@Override
	public Sources getSourceName() {
		return Sources.EuropeanaFashion;
	}

	@Override
	public HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}

	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		return FunctionsUtils.toORList("qf", 
				(s)-> parameter + ":" + FunctionsUtils.quote().apply(s)
				);
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		CommonFilter f = new CommonFilter(FiltersFields.PROVIDER.getFilterId(), "Europeana Fashion");
		if (q.filters!=null)
			q.filters.add(f);
			else
		q.filters = Arrays.asList(f);
		return super.getResults(q);
	}


}

