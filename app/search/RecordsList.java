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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import model.basicDataTypes.Literal;
import model.resources.RecordResource;

public class RecordsList {
	
	private int count;
	private String identifier;
	private Literal title;
	private Literal description;
	private List<RecordResource<?>> records;
	
	public RecordsList(String identifier,Literal title) {
		this(identifier,title,title);
	}
	
	public RecordsList(String identifier,Literal title, Literal description) {
		this();
		this.identifier = identifier;
		this.title = title;
		this.description = description;
	}
	public RecordsList() {
		super();
		records = new ArrayList<>();
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public Literal getTitle() {
		return title;
	}
	public void setTitle(Literal title) {
		this.title = title;
	}
	public Literal getDescription() {
		return description;
	}
	public void setDescription(Literal description) {
		this.description = description;
	}
	public List<RecordResource<?>> getRecords() {
		return records;
	}
	public void setRecords(List<RecordResource<?>> records) {
		this.records = records;
	}
	
	public void addRecords(Collection<? extends RecordResource<?>> records) {
		this.records.addAll(records);
		count = this.records.size();
	}
	
	public void addRecords(Collection<? extends RecordResource<?>> records, int size) {
		if (size<=0)
			addRecords(records);
		else {
			Iterator<? extends RecordResource<?>> it = records.iterator();
			for (int i = 0 ; i < Math.min(size, records.size()) && it.hasNext(); i++) {
				this.records.add(it.next());
			}
		}
		this.records.addAll(records);
		count = this.records.size();
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	
	
}
