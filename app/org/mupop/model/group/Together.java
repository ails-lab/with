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

import org.mupop.model.media.Audio;
import org.mupop.model.media.Image;
import org.mupop.model.media.MultilangAudio;
import org.mupop.model.media.Video;

/**
 * Created by stabenau on 11/3/16.
 * If a group is a Together object, the semantics is, that everything in it (attributes and subgroups) are
 * visible at once. 	
 */
public class Together extends Group {
	

	
    public static Together create() { return new Together(); }
}
