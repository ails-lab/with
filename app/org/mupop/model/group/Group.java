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


package org.mupop.model.group;

import java.util.ArrayList;
import java.util.List;

import org.mupop.model.Referenceable;
import org.mupop.model.group.Together;
import org.mupop.model.media.Image;
import org.mupop.model.media.MultilangAudio;
import org.mupop.model.media.Text;
import org.mupop.model.media.Video;

/**
 * Created by stabenau on 11/3/16.
 * We support different forms of grouping. They should all be referenceable. All attributes are opional.
 * 
 */
public class Group implements Referenceable {
    // if this group should be reachable via the Table of Contents
    public boolean TOC;

    // you can suggest the id of a group by giving it here. In a choice object, the event for this group will
    // contain this tag.
    public String tag;

    // An optional title for this group
    public Text title;

    // An optional  descriptive text
    public Text description;

    // An optional image for this group
    public Image image;

    // An optional thumbnail for this group
    public Image thumbnail;

    // first problem, audio should be multilang? Or normal?
    public MultilangAudio audio;

    // If there is background (eg music)
    public MultilangAudio ambienceAudio;

    // if a video is to be shown
    public Video video;

    // if a video is to be shown as background
    public Video ambienceVideo;

    // an ambience or background image might be needed
    public Image ambienceImage;

    // the things that are grouped
    public List<Group> groups = new ArrayList<Group>();
    
    public Cho cho;
    
    
    //
    // Convenience Methods
    //
    
    public static Group create() { return new Group(); }
    public Group addGroup( Group g) {
    	groups.add( g );
    	return this;
    }
    public Group setTitle( Text t ) {
    	this.title = t;
    	return this;
    }
    
    public Group setDescription( Text t) {
    	this.description = t;
    	return this;
    }

    public Group setImage( Image i ) {
    	this.image = i;
    	return this;
    }

    public Group setThumbnail( Image i ) {
    	this.thumbnail = i;
    	return this;
    }
    
    public Group setAudio( MultilangAudio a) {
    	this.audio = a;
    	return this;
    }
    
    public Group setAmbienceAudio( MultilangAudio a) {
    	this.ambienceAudio = a;
    	return this;
    }
    
    public Group setVideo( Video v) {
    	this.video = v;
    	return this;
    }
    
    public Group setAmbienceImage( Image i ) {
    	this.ambienceImage = i;
    	return this;
    }
    
    public Group setCho( Cho c ) {
    	this.cho = c;
    	return this;
    }
    

}
