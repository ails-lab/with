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


package sources.formatreaders;

import java.util.Collection;

import org.bson.types.ObjectId;
import org.mupop.model.group.Cho;
import org.mupop.model.group.Group;
import org.mupop.model.group.Sequence;
import org.mupop.model.group.Together;
import org.mupop.model.media.Image;
import org.mupop.model.media.Text;

import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CollectionObjectController;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.Resource;
import model.resources.WithResourceType;
import model.resources.collection.Exhibition;
import model.resources.collection.Exhibition.ExhibitionDescriptiveData;
import play.libs.Json;
import sources.utils.JsonContextRecord;

public abstract class ExhibitionReader {

	public ExhibitionReader() {
		super();
	}

	public abstract Object importExhibitionObjectFrom(JsonContextRecord text, ObjectId creatorDbId);

	protected Text buildTextElement(String stringValue) {
		Text text = new Text();
		text.add("en", stringValue);
		return text;
	}

	protected Group buildSequenceElement(Collection<? extends Group> records, Text title) {
		Sequence sq = new Sequence();
		sq.TOC = true;
		sq.setTitle(title);
		if (records != null)
			for (Group group : records) {
				sq.addGroup(group);
			}
		return sq;
	}

	protected Group buildTogetherElement(Collection<? extends Group> records, Text t) {
		Together sq = new Together();
		if (records != null)
			for (Group group : records) {
				sq.addGroup(group);
			}
		sq.setDescription(t);
		return sq;
	}

	protected Cho buildCHOElement(JsonContextRecord o, Text caption) {
		Cho object = new Cho();
		object.setTitle(caption);
		object.setThumbnail(Image.create(o.getStringValue("media[0].Thumbnail.url")));
		object.url = o.getStringValue("administrative.withURI");
		return object;
	}

}