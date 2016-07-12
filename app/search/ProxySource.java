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


package search;

import java.util.Set;
import java.util.function.Function;

import model.basicDataTypes.ProvenanceInfo;
import model.resources.RecordResource;
import play.libs.F.Promise;
import sources.BritishLibrarySpaceSource;
import sources.DDBSpaceSource;
import sources.DPLASpaceSource;
import sources.DigitalNZSpaceSource;
import sources.EuropeanaSpaceSource;
import sources.FlickrSpaceSource;
import sources.HistorypinSpaceSource;
import sources.NLASpaceSource;
import sources.RijksmuseumSpaceSource;
import sources.YouTubeSpaceSource;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.SourceResponse;
import utils.ListUtils;

public class ProxySource extends EmptySource {
	
	private Sources source;
	private ISpaceSource spaceSource;
	
	public ProxySource(Sources source, ISpaceSource spaceSource) {
		super();
		this.source = source;
		this.spaceSource = spaceSource;
	}
	
	public ProxySource(ISpaceSource spaceSource) {
		super();
		this.source = spaceSource.getSourceName();
		this.spaceSource = spaceSource;
	}
	
	@Override
	public Sources thisSource() {
		return source;
	}
	
	@Override
	public Promise<Response.SingleResponse>  execute(Query query) {
		Function<Query, Response.SingleResponse> f = (Query q)->{
			SourceResponse oldresult = spaceSource.getResults(new CommonQuery(q));
			return oldresult.exportToSingleSource();
		};
		return ParallelAPICall.createPromise(f , query);
	}
	
	@Override
	public Promise<Object> completeRecord(Object incompleteRecord) {
		Function<Object, Object> f = (rec)->{
			if (rec instanceof RecordResource){
				RecordResource o = (RecordResource)rec;
				
			return spaceSource.getRecordFromSource(
							ListUtils.<ProvenanceInfo>last(o.getProvenance()).getResourceId(), o);
			} else {
				// TODO not supported
				return rec;
			}
		};
		return ParallelAPICall.createPromise(f , incompleteRecord);
	}
	
	@Override
	public Promise<Object> getById(String id) {
		return ParallelAPICall.createPromise((myid) -> 
			spaceSource.getRecordFromSource(myid,null) , id);
	}
	
	@Override
	public Set<String> supportedFieldIds() {
		Set<String> filters = spaceSource.getVmap().getFilters();
		filters.add(FiltersFields.ANYWHERE.getFilterId());
		return filters;
	}
	
	public static class EuropeanaProxySource extends ProxySource{

		public EuropeanaProxySource() {
			super(new EuropeanaSpaceSource());
		}
		
	}
	public static class DDBProxySource extends ProxySource{

		public DDBProxySource() {
			super(new DDBSpaceSource());
		}
		
	}
	
	public static class BritishLibProxySource extends ProxySource{

		public BritishLibProxySource() {
			super(new BritishLibrarySpaceSource());
		}
		
	}
	
	public static class InternetArchiveProxySource extends ProxySource{

		public InternetArchiveProxySource() {
			super(new FlickrSpaceSource.InternetArchiveSpaceSource());
		}
		
	}
	
	public static class DigitalNZProxySource extends ProxySource{

		public DigitalNZProxySource() {
			super(new DigitalNZSpaceSource());
		}
		
	}
	
	public static class DPLAProxySource extends ProxySource{

		public DPLAProxySource() {
			super(new DPLASpaceSource());
		}
		
	}
	
	public static class HistoryPinProxySource extends ProxySource{

		public HistoryPinProxySource() {
			super(new HistorypinSpaceSource());
		}
		
	}
	
	public static class RijksProxySource extends ProxySource{

		public RijksProxySource() {
			super(new RijksmuseumSpaceSource());
		}
		
	}
	
	public static class YoutubeProxySource extends ProxySource{

		public YoutubeProxySource() {
			super(new YouTubeSpaceSource());
		}
		
	}
	
	public static class NLAProxySource extends ProxySource{

		public NLAProxySource() {
			super(new NLASpaceSource());
		}
		
	}

}
