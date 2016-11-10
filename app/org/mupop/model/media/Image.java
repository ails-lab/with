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


package org.mupop.model.media;

import org.mupop.model.Referenceable;

/**
 * Contains multiple Images with different resolution, but essentially the same content.
 * Created by stabenau on 11/2/16.
 */
public class Image implements Referenceable {
    /**
     * The Image is normally part of a grouping that provides explanation for the visual.
     * Caption is typically not needed then.
     */
    public Text caption;
    public ImageData[] images;
    
    //
    // Convenience methods
    //
    
    public static Image create() { return new Image(); }
    public static Image create( String url ) {
    	Image i = new Image();
    	ImageData id = new ImageData();
    	id.url = url;
    	i.images = new ImageData[1];
    	i.images[0]= id;
    	return i;
    }
}
