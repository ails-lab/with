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

import java.util.HashMap;
import java.util.Map;

import org.mupop.model.Referenceable;

/**
 * This class supports multilanguage text. It will be listed in the media section of the MuPoP datamodel.
 * Created by stabenau on 11/2/16.
 */
public class Text implements Referenceable {

    /**
     * Text can be in a known language. Multiple texts with different languages should be translations of each other.
     * use key "default" for not languaged text.
     */
    public Map<String, String> textByLanguage = new HashMap<String, String>();
    
    //
    // Convenience methods 
    //
    
    public static Text create() { return new Text(); }
    public Text add( String lang, String val ) {
    	textByLanguage.put( lang, val);
    	return this;
    }
    
}
