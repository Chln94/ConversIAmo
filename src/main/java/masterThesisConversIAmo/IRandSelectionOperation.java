package masterThesisConversIAmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.ibm.watson.assistant.v1.model.Entity;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryOptions.Builder;
import com.ibm.watson.discovery.v1.model.QueryResponse;
import com.ibm.watson.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.natural_language_understanding.v1.model.Features;

public class IRandSelectionOperation {

	
	public static void findRecallPrecisionPositionsWithEntities(Map<String, List<String>> entitiesForCategory, int i, ListIterator<Entity> iterator, List<String> keywords, QueryOptions.Builder queryBuilder, String question, String category, Discovery discovery, List<String> bestPassages, List<Map<String, String>> entitiesList, Features features, NaturalLanguageUnderstanding naturalLanguageUnderstanding, int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, double[] passageRecall, double[] passagePrecision){

		int keywordsCounter=0;
		int maxKeywordsNumber=0;
		HashMap<Integer, List<String>> map = new HashMap<>();

		while(iterator.hasNext()) {
			Entity entity=iterator.next();
			for(int index=0; index<entitiesForCategory.get(category).size(); index++) {
				if(entity.getEntity().matches(entitiesForCategory.get(category).get(index))) {
					for(int j=0;j<entity.getValues().size(); j++) {
						question= question+" "+entity.getValues().get(j).value();
						System.out.println(question);

						queryBuilder.naturalLanguageQuery(question);
						QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute().getResult();

						keywordsCounter=0;
						String passage=queryResponse.getPassages().get(0).getPassageText();
						for(int k=0; k< keywords.size(); k++) {
							if(passage.toLowerCase().contains(entity.getValues().get(j).value().toLowerCase()) && 
									passage.toLowerCase().contains(keywords.get(k))) {
								keywordsCounter++;
							}
						}

						if(keywordsCounter>0 && keywordsCounter>=maxKeywordsNumber-1) {
							updateKeywordsMax(passage, keywordsCounter, maxKeywordsNumber, map);
						}
						keywords.remove(entity.getValues().get(j).value().toLowerCase());
						question= question.substring(0, question.length()-entity.getValues().get(j).value().length()-1);
					}
				}

			}

			//sorting:
			sortCandidatesAccordingToEntitiesList(map, maxKeywordsNumber, entitiesList);

			Analytics.checkIfContainsBestPassages(maxKeywordsNumber, map, bestPassages, positionFrequency, numberOfPassagesPerQuestion, 
					numberOfPassagesPerQuestionFrequency, results, passageRecall, passagePrecision, question, category, i);


		}
	}

	public static void updateKeywordsMax(String passage, int keywordsCounter, int maxKeywordsNumber, HashMap<Integer, List<String>> map) {
		List<String> ausList= new ArrayList<String>();
		String prevPassage;
		ausList.add(passage);

		if(map.get(keywordsCounter)!=null) {
			Iterator<String> passIt= map.get(keywordsCounter).iterator();
			while(passIt.hasNext()) {
				prevPassage=passIt.next();
				if(!prevPassage.contentEquals(passage))
					ausList.add(prevPassage);
			}
		}
		map.put(keywordsCounter, ausList);
		for(int k=0; k<maxKeywordsNumber-1; k++ ) {
			if(map.containsKey(k)) {
				map.remove(k);
			}
		}
	}


	

	public static Map<String, Float> findRecallPrecisionPositionsKeywordsAndSorting(int i, QueryOptions.Builder queryBuilder, String question, String category, Discovery discovery, List<String> bestPassages, List<String> keywords, List<Map<String, String>> entitiesList, int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, double[] passageRecall, double[] passagePrecision, Features features, NaturalLanguageUnderstanding naturalLanguageUnderstanding){

		int keywordsCounter=0;
		Map<String, Float> resultPassages= new HashMap<String, Float>();

		queryBuilder.naturalLanguageQuery(question);
		QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute().getResult();

		for(int j=0; j< queryResponse.getPassages().size(); j++) {
			String passage= queryResponse.getPassages().get(j).getPassageText();
			keywordsCounter=0;
			for(int k=0; k< keywords.size(); k++) {
				if(passage.toLowerCase().contains(keywords.get(k))) {
					keywordsCounter++;
				}
			}
			resultPassages.put(passage, Double.valueOf(queryResponse.getPassages().get(j).getPassageScore()).floatValue()+keywordsCounter);
			if(!entitiesList.isEmpty()) {
				Float score = new Float("0");
				Iterator<Map<String, String>> questEntities= entitiesList.iterator();
				while(questEntities.hasNext()) {
					Map<String, String> nextEnt= questEntities.next();
					if(passage.toLowerCase().contains(nextEnt.values().toString().toLowerCase()
							.substring(1, nextEnt.values().toString().length()-1)))
						score +=1;
				}
				resultPassages.replace(passage, (Float) resultPassages.get(passage)+score);
			}
		}

		//sorting:
		sortCandidatesAccordingToScore(resultPassages, naturalLanguageUnderstanding);
		Analytics.checkIfContainsBestPassagesScore(resultPassages, bestPassages, positionFrequency, numberOfPassagesPerQuestion, 
				numberOfPassagesPerQuestionFrequency, results, passageRecall, passagePrecision, question, category, i);


		return resultPassages;

	}

	public static Map<String, Float> findRecallPrecisionPositionsWithFilterSortingByScore(NaturalLanguageUnderstanding naturalLanguageUnderstanding, List<Map<String, String>> entitiesList, Features features, Map<String, List<String>> entitiesForCategory, int i, List<String> keywords, Builder queryBuilder, String question, String category, Discovery discovery, List<String> bestPassages, int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, double[] passageRecall, double[] passagePrecision) {

		int keywordsCounter=0;
		Map<String, Float> resultPassages= new HashMap<String, Float>();

		queryBuilder.naturalLanguageQuery(question);
		QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute().getResult();

		for(int j=0; j<queryResponse.getPassages().size(); j++) {
			String passage= queryResponse.getPassages().get(j).getPassageText();
			Boolean toBeInserted=false;

			AnalyzeOptions parameters = new AnalyzeOptions.Builder()
					.language("it")
					.text(passage)
					.features(features)
					.build();

			AnalysisResults passageResponse = naturalLanguageUnderstanding
					.analyze(parameters)
					.execute()
					.getResult();

			if(passageResponse.getEntities()!= null) {
				for(int k=0; k<passageResponse.getEntities().size(); k++) {
					for(int index=0; index<entitiesForCategory.get(category).size(); index++) {
						if(passageResponse.getEntities().get(k).getType().matches(entitiesForCategory.get(category).get(index))) {
							toBeInserted=true;
						}
					}
				}
			}

			if(toBeInserted) {
				keywordsCounter=0;
				for(int k=0; k< keywords.size(); k++) {
					if(passage.toLowerCase().contains(keywords.get(k))) {
						keywordsCounter++;
					}
				}
				resultPassages.put(passage, Double.valueOf(queryResponse.getPassages().get(j).getPassageScore()).floatValue()+keywordsCounter);
				if(!entitiesList.isEmpty()) {
					Float score = new Float("0");
					Iterator<Map<String, String>> questEntities= entitiesList.iterator();
					while(questEntities.hasNext()) {
						Map<String, String> nextEnt= questEntities.next();
						if(passage.toLowerCase().contains(nextEnt.values().toString().toLowerCase()
								.substring(1, nextEnt.values().toString().length()-1)))
							score +=1;
					}
					resultPassages.replace(passage, (Float) resultPassages.get(passage)+score);
				}
			}

		}

		//sorting and filter by score>0.3
		sortCandidatesAccordingToScore(resultPassages, naturalLanguageUnderstanding);
		Analytics.checkIfContainsBestPassagesScore(resultPassages, bestPassages, positionFrequency, numberOfPassagesPerQuestion, 
				numberOfPassagesPerQuestionFrequency, results, passageRecall, passagePrecision, question, category, i);

		return resultPassages;

	}

	

	private static void sortCandidatesAccordingToScore(Map<String, Float> resultPassages, NaturalLanguageUnderstanding naturalLanguageUnderstanding) {
		Object[] sorted =  resultPassages.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).toArray();
		Float maxScore= Float.valueOf(sorted[0].toString().substring(sorted[0].toString().lastIndexOf("=")+1));
		Float minScore=  Float.valueOf(sorted[sorted.length-1].toString().substring(sorted[sorted.length-1].toString().lastIndexOf("=")+1));
		resultPassages.clear();
		for(int j=0; j<sorted.length; j++) {
			int delimiter= sorted[j].toString().lastIndexOf("=");
			String key= sorted[j].toString().substring(0, delimiter);
			Float value= Float.valueOf(sorted[j].toString().substring(delimiter+1, sorted[j].toString().length()));
			if((value-minScore)/(maxScore-minScore)>0.3) {
				resultPassages.put(key, value);
			}
		}

	}

	private static void sortCandidatesAccordingToEntitiesList(HashMap<Integer, List<String>> map, int maxKeywordsNumber, List<Map<String, String>> entitiesList) {
		List<HashMap<String, String>> candidates;
		HashMap<String, String> singleCandidate;
		for(int m=0; m<map.size(); m++) {
			candidates = new ArrayList<HashMap<String, String>>();

			for(int n=0; n<map.get(maxKeywordsNumber-m).size(); n++) {
				int score=0;
				singleCandidate= new HashMap<String, String>();

				if(!entitiesList.isEmpty()) {
					Iterator<Map<String, String>> questEntities= entitiesList.iterator();
					while(questEntities.hasNext()) {
						Map<String, String> nextEnt= questEntities.next();
						if(map.get(maxKeywordsNumber-m).get(n).toLowerCase().contains(nextEnt.values().toString().toLowerCase()
								.substring(1, nextEnt.values().toString().length()-1)))
							score +=1;
					}
				}
				map.get(maxKeywordsNumber-m).get(n);
				singleCandidate.put("text",map.get(maxKeywordsNumber-m).get(n));
				singleCandidate.put("score", String.valueOf(score));
				candidates.add(singleCandidate);
			}
			Collections.sort(candidates,new Comparator<HashMap<String, String>>(){
				public int compare(HashMap<String, String> a, HashMap<String, String> b){
					return Integer.compare(Integer.parseInt(a.get("score")),Integer.parseInt(b.get("score")));
				}
			}.reversed());
			List<String> passageList= new ArrayList<String>();
			Iterator<HashMap<String, String>> candidateIt= candidates.iterator();
			while(candidateIt.hasNext()) {
				passageList.add(candidateIt.next().get("text"));
			}
			map.get(maxKeywordsNumber-m).clear();
			map.put((maxKeywordsNumber-m), passageList);
		}

	}

	

	
}