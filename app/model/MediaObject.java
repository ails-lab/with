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

import java.util.ArrayList;

import model.ExampleDataModels.EmbeddedMediaObject;

import org.bson.types.ObjectId;

public class MediaObject extends EmbeddedMediaObject {		
	// which resource is this Media part of, this is the access rights restriction
	// if there is none, the media object is publicly available
	private ArrayList<ObjectId> resources;
	
	private int width, height;
	
	private double durationSeconds;
			
	private byte[] thumbnailBytes;
	private byte[] mediaBytes;
}
