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


package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

public class CodeController extends Controller {
	
	public static final ALogger log = Logger.of(CodeController.class);
	public static final String LIBDIR =  "target/universal/stage/lib";
	
	
	/**
	 * Make a list of all the jars and serve it here, so actors can download them and use them.
	 * @return
	 */
	public static Result jarList() {
		try {
			// String strClassPath = System.getProperty("java.class.path");
			File libDir = new File( Play.application().path(), LIBDIR );
			List<String> jarUrls = fileChecksum( libDir );	
			String baseUrl = "http://"+request().host()+request().uri();
			JsonNode res = buildResultJson(jarUrls, baseUrl);
			return ok( res );
		} catch( Exception e ) {
			log.error( "No Application description json",e );
			return badRequest( "Cant serve Application description json" );
		}
	}
	
	/**
	 * Just serving files from the classpath ... and only certain ones, JRE internal ones.
	 * @param filename
	 * @return
	 */
	public static Result downloadJar( String filename ) {
		File libDir = new File( Play.application().path(), LIBDIR );
		File toServe = new File( libDir, filename );
		if( toServe.exists() && toServe.canRead()) return ok( toServe );
		else return badRequest( "Can't serve " + filename );
	}
	
	private static JsonNode buildResultJson( List<String> jars, String baseUrl ) {
		ObjectMapper om = new ObjectMapper();
		ObjectNode root = om.createObjectNode();
		ArrayNode jarNodes = om.createArrayNode();
		for( String s: jars ) {
			String[] cols = s.split( "\\t" );
			ObjectNode singleJar = om.createObjectNode()
					.put( "path", cols[0])
					.put( "md5", cols[1])
					.put( "url", baseUrl+"/"+cols[0] );
			
			jarNodes.add(singleJar);
		}
		root.set( "resources", jarNodes );
		
		// add relevant settings
		ObjectNode settings = root.objectNode();
		root.set( "settings", settings);
		
		// only config.file and config.resource is relevant
		// and only config.resource is needed on the receiving end
		String configfile = System.getProperty("config.file");
		if( configfile != null ) {
			// if its in the conf dir, extract the filename 
			configfile.replaceAll(".*conf/(.*)$" ,"$1" );
		} else {
			configfile = System.getProperty("config.resource" );
		}
		// no config in the setting, its the standard one, still needs to be configured,
		// since there is no play running
		if(( configfile == null) || ( configfile.length() == 0 )) 
			configfile = "application.conf";
		settings.put( "config.resource", configfile );
		return root;
	}
	
	/**
	 * Create a list of files and their checksums in given directory. 
	 * file \t md5hex
	 * @param directory
	 * @return
	 */
	private static List<String> fileChecksum( File directory ) {
		ArrayList<String> nameChecksum = new ArrayList<String>();
		try {
			if( directory.exists() && directory.canRead() && directory.isDirectory()) {
				String[] contents = directory.list();
				for( String s: contents ) {
					File f = new File( directory, s );
					if( f.isFile()) {
						FileInputStream fis = new FileInputStream( f );
						String md5 = DigestUtils.md5Hex(fis);
						fis.close();
						nameChecksum.add( s+"\t"+md5);
					}
				}
			}
		} catch( Exception e ) {
			log.error( "", e );
		}
		return nameChecksum;		
	}
	
}
