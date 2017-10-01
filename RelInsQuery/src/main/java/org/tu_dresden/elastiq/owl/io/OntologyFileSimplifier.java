package org.tu_dresden.elastiq.owl.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OntologyFileSimplifier {

	private static final Map<String,String> DEFAULT_REGEX = new HashMap<String,String>();

	public OntologyFileSimplifier() {
		DEFAULT_REGEX.put("OntologyID\\(Anonymous-[0-9]*\\)#", "");
		DEFAULT_REGEX.put("http://www.w3.org/2002/07/owl#Thing", "top");
	}
	
	public String simplify(File file){
		return simplify(file, DEFAULT_REGEX);
	}
	
	public String simplify(File file, Map<String, String> regex){
		FileReader fr;
		try {
			fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = br.readLine()) != null){
				for(String r : regex.keySet()){
					line = line.replaceAll(r, regex.get(r));
				}
				sb.append(line + "\n");
			}
			br.close(); fr.close();
			
			FileWriter fw = new FileWriter(file);
			fw.write(sb.toString());
			fw.close();
			
			return sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		return "some error occurred";
	}
}
