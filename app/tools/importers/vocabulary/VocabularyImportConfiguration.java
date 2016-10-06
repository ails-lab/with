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


package tools.importers.vocabulary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import db.DB;

public class VocabularyImportConfiguration {

	public static String inPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.srcpath");
	public static String outPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path");
	
	static String newLine = System.getProperty("line.separator");
	
	static Pattern labelPattern = Pattern.compile("^\"(.*?)\"@(.*)$");
	
	String folder;
	
	public VocabularyImportConfiguration(String folder) {
		this.folder = folder;
	}
	
	public File getInputFolder() {
		return new File(inPath + File.separator + folder);
	}
	
	public File getOutputFile() {
		return new File(outPath + File.separator + folder + ".txt");
	}
	
	public Matcher labelMatcher(String label) {
		return labelPattern.matcher(label);
	}
	
	static final int BUFFER = 2048;
	
	public void compress() throws FileNotFoundException, IOException {
		compress(folder);
	}
	
	public void deleteTemp() {
		deleteTemp(folder);
	}
	
	public static void compress(String fileName) throws FileNotFoundException, IOException {
		try (FileOutputStream dest = new FileOutputStream(new File(outPath + File.separator + fileName + ".zip"));
			 ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
			
			byte data[] = new byte[BUFFER];
			
			File f = new File(outPath + File.separator + fileName + ".txt");
			
			try (FileInputStream fi = new FileInputStream(f);
			     BufferedInputStream origin = new BufferedInputStream(fi, BUFFER)) {

				ZipEntry entry = new ZipEntry(f.getName());
				
				out.putNextEntry(entry);
				
				int count;
				while((count = origin.read(data, 0, BUFFER)) != -1) {
				   out.write(data, 0, count);
				}
				
				out.flush();
			}
		}
	}
	
	public static void deleteTemp(String fileName) {
		File f = new File(outPath + File.separator + fileName + ".txt");
		if (f.exists()) {
			f.delete();
		}
	}


}
