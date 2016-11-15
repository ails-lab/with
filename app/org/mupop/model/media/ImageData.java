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

/**
 * Represents single ImageData, either with the data inside or via URL.
 * Created by stabenau on 11/2/16.
 */
public class ImageData {
    /**
     * If the image is available as Link, here it goes.
     */
    public String url;

    /**
     * If the data is in a framebuffer, it can be send with this mimetype.
     */
    public String mimeType;

    /**
     * Binary image content
     */
    public byte[] framebuffer;

    /**
     * Dimension of image
     */
    public int width, heigth;
}
