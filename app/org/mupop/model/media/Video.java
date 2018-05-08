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
 * Created by stabenau on 11/3/16.
 * A video that can have multiple VideoData sections with different qualities of the same video
 */
public class Video implements Referenceable {
    // different quality versions of the same video
    public VideoData[] videoData;

    // audio might be included in the video, provide the language codes here
    public String[] availableAudio;

    // available (included) subtitles
    public String[] availableSubtitleLanguages;

    // or linked audio
    // ( probably a bitch to sync :-)
    public MultilangAudio linkedAudio;

    // external subtitles go here
    public Subtitle subtitles;
    
    //
    // Convenience Methods
    //
    
    public static Video create( String url ) {
    	VideoData vd = new VideoData();
    	vd.url = url;
    	
    	Video v = new Video();
    	v.videoData = new VideoData[1];
    	v.videoData[0] = vd;
    	return v;
    }
    
}
