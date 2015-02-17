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


package model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

/**
 * A class to represent media, merge of all the interesting attributes
 * and access to the byte[] that is the data for it
 * @author stabenau
 *
 */
public class Media {
	@Id
	private ObjectId dbID;
	// examples, but there might be more
	private int width, height;
	
	// IMAGE, VIDEO, AUDIO, TXT
	private String type;
	
	// more explicit media type
	private String mimeType;
	
	// how long in seconds
	private float duration;
	
	// the actual data .. GridFS
	private byte[] data;
}
