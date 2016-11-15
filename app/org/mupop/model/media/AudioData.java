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
 * Basic audio artefact, with some quality metadata
 * Created by stabenau on 11/2/16.
 */
public class AudioData {
    /**
     * Embedded audio data comes like this
     */

    // some metadata about quality
    public int kbitPerSec, bitsSampled;
    public String mimetype;
    public byte[] data;

    /**
     * If the sound can be played from a URL. This field should be filled, when talking to a browser.
     */
    public String url;
}
