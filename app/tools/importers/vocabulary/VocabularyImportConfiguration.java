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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class VocabularyImportConfiguration {

//	public static String inPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.srcpath");
//	public static String outPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path");
	
	
	static String newLine = System.getProperty("line.separator");
	
	static Pattern labelPattern = Pattern.compile("^\"(.*?)\"@(.*)$");
	
	protected String folder;
	
	public static String srcdir;
	public static String outdir;
	
	public static String tmpdir;
	
	public VocabularyImportConfiguration(String folder) {
		this.folder = folder;
		
//		File dir  = new File(outdir);
//		if (!dir.exists()) {
//			dir.mkdir();
//		}

	}

	public static File getTempFolder() {
		File f = new File(tmpdir);
		if (!f.exists()) {
			f.mkdir();
		} 
		
		File ff = new File(tmpdir + File.separator + "voc-" + (int)Math.floor(Math.random()*10000000));
		while (ff.exists()) {
			ff = new File(tmpdir + File.separator + "voc-" + (int)Math.floor(Math.random()*10000000));
		}

		ff.mkdir();
		
		return ff; 
	}

	public File getInputFolder() {
		return new File(srcdir + File.separator + folder);
	}


//	public String getOutputFileName() {
//		return folder + ".txt";
//	}
	
	public Matcher labelMatcher(String label) {
		return labelPattern.matcher(label);
	}
	
	static final int BUFFER = 2048;
	
//	public void compress() throws FileNotFoundException, IOException {
//		compress(folder);
//	}
	
//	public void deleteTemp() {
//		deleteTemp(folder);
//	}
	
	public static File compress(File path, String fileName) throws FileNotFoundException, IOException {
		File f = new File(path + File.separator + fileName + ".zip");
		
		try (FileOutputStream dest = new FileOutputStream(f);
			 ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
			
			byte data[] = new byte[BUFFER];
			
			File ff = new File(path + File.separator + fileName + ".txt");
			
			try (FileInputStream fi = new FileInputStream(ff);
			     BufferedInputStream origin = new BufferedInputStream(fi, BUFFER)) {

				ZipEntry entry = new ZipEntry(ff.getName());
				
				out.putNextEntry(entry);
				
				int count;
				while((count = origin.read(data, 0, BUFFER)) != -1) {
				   out.write(data, 0, count);
				}
				
				out.flush();
			}
		}
		
		return f;
	}
	
	public static List<File> uncompress(File path, File f) throws FileNotFoundException, IOException {
		List<File> res = new ArrayList<>();
		
		try (FileInputStream fis = new FileInputStream(f);
		     ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {
	    	  
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " + entry);
		        int count;
		        byte data[] = new byte[BUFFER];
		        
		        File ff = new File(path + File.separator + entry.getName());
		        try (FileOutputStream fos = new FileOutputStream(ff);
 		             BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER)) {
		        
			        while ((count = zis.read(data, 0, BUFFER)) != -1) {
			        	dest.write(data, 0, count);
			        }
		        
			        dest.flush();
		        }
		        
		        res.add(ff);
			}
		}
		
		return res;
	}
	
	public void deleteTemp(String fileName) {
		File f = new File(outdir + File.separator + fileName + ".txt");
		if (f.exists()) {
			f.delete();
		}
	}


}
