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
 * Created by stabenau on 11/3/16.
 * Videodata with some quality related metadata
 */
public class VideoData {
    public String mimeType;

    // optionally contains the data ... for video that should be rare
    public byte[] data;

    // where to get the video from
    public String url;

    // format of video
    public int width, height;

    // bitrate in kilobit / sec
    // this should be a good measure for the quality
    public int kbitSec;
}
