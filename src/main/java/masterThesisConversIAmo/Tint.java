package masterThesisConversIAmo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.pipeline.Annotation;
import eu.fbk.dh.tint.runner.TintPipeline;
import eu.fbk.dh.tint.runner.TintRunner;

public class Tint {
	
	public static TintPipeline tintSetup(){
		// Initialize the Tint pipeline
		TintPipeline pipeline = new TintPipeline();
		try {
			pipeline.loadPropertiesFromFile(new File("default-config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		pipeline.setProperty("nthread", "10");
		pipeline.setProperty("customAnnotatorClass.keyphrase", "eu.fbk.dh.kd.annotator.DigiKDAnnotator");
		pipeline.setProperty("keyphrase.numberOfConcepts", "20");
		pipeline.setProperty("keyphrase.local_frequency_threshold", "2");
		pipeline.setProperty("keyphrase.language", "ITALIAN");

		pipeline.load();
		return pipeline;
	}

	public static List<String> keywordsExtractionWithTint(String question) {
		List<String> keywords= new ArrayList<String>();
		TintPipeline pipeline = tintSetup();
		InputStream stream = new ByteArrayInputStream(question.getBytes(StandardCharsets.UTF_8));
		OutputStream out = null;
		Annotation qAnnotation=null;
		try{
			out = new ByteArrayOutputStream();
			qAnnotation = pipeline.run(stream,out, TintRunner.OutputFormat.JSON);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		JsonParser parser2 = new JsonParser();
		JsonObject qAnalyzed = (JsonObject) parser2.parse(out.toString());
		JsonArray questionAnalyzed= qAnalyzed.getAsJsonArray("sentences");

		Map<String,String> tokenPos= new HashMap<String,String>();
		JsonArray tokens= questionAnalyzed.get(0).getAsJsonObject().get("tokens").getAsJsonArray();
		for(int k=0; k< tokens.size(); k++) {
			String POS = tokens.get(k).getAsJsonObject().get("pos").toString();
			POS = POS.substring(1, POS.length()-1);
			if(POS.matches("V") || 	POS.matches("S") || POS.matches("SP")|| POS.matches("SW") || POS.matches("A")){
				String word=tokens.get(k).getAsJsonObject().get("word").toString();
				tokenPos.put(word.substring(1, word.length()-1).toLowerCase(), POS );
			}
		}

		JsonArray dep= questionAnalyzed.get(0).getAsJsonObject().get("basic-dependencies").getAsJsonArray();
		for(int k=0; k< dep.size(); k++) {
			Boolean toBeInserted=false;
			String governor=dep.get(k).getAsJsonObject().get("governorGloss").toString()
					.substring(1,dep.get(k).getAsJsonObject().get("governorGloss").toString().length()-1);
			String dependent=dep.get(k).getAsJsonObject().get("dependentGloss").toString()
					.substring(1,dep.get(k).getAsJsonObject().get("dependentGloss").toString().length()-1);

			if(tokenPos.get(governor)!=null && tokenPos.get(governor).contains("S") 
					&& tokenPos.get(dependent)!= null) {
				toBeInserted=true;
				for(int z=0; z<keywords.size(); z++) {
					if(keywords.get(z).toString().contains(governor)) {
						keywords.get(z).replace(keywords.get(z), keywords.get(z).concat(" "+dependent));
						toBeInserted=false;
					}
				}
				if(toBeInserted)
					keywords.add(governor+" "+dependent);
			}
		}
		Iterator<String> it=tokenPos.keySet().iterator();
		while(it.hasNext()) {
			String key= it.next();
			if(!keywords.toString().contains(key))
				keywords.add(key);
		}
		System.out.println("keywords found:"+keywords);
		return keywords;
	}


}
