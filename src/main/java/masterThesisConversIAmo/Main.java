package masterThesisConversIAmo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.assistant.v1.Assistant;
import com.ibm.watson.assistant.v1.model.EntityCollection;
import com.ibm.watson.assistant.v1.model.ListEntitiesOptions;
import com.ibm.watson.assistant.v1.model.ListWorkspacesOptions;
import com.ibm.watson.assistant.v1.model.MessageInput;
import com.ibm.watson.assistant.v1.model.MessageOptions;
import com.ibm.watson.assistant.v1.model.MessageResponse;
import com.ibm.watson.assistant.v1.model.Workspace;
import com.ibm.watson.assistant.v1.model.WorkspaceCollection;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.ListCollectionsOptions;
import com.ibm.watson.discovery.v1.model.ListCollectionsResponse;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryResponse;
import com.ibm.watson.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.natural_language_understanding.v1.model.ConceptsOptions;
import com.ibm.watson.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.natural_language_understanding.v1.model.Features;
import com.ibm.watson.natural_language_understanding.v1.model.KeywordsOptions;
import com.opencsv.CSVReader;

import edu.stanford.nlp.util.StringUtils;



public class Main {
	
	public static List<List<String>> readCsvFile(String path) throws IOException {
		List<List<String>> records = new ArrayList<List<String>>();
		try (CSVReader csvReader = new CSVReader(new FileReader(path));) {
			String[] values = null;
			while ((values = csvReader.readNext()) != null) {
				records.add(Arrays.asList(values));
			}
		}
		return records;

	}

	public static void main(String[] args) throws IOException {

		//Watson Assistant connection via API
		IamOptions iamOptions = new IamOptions.Builder().apiKey("BWbY7TAHQH4p_jNwx-TLEKBmPpCpujje7qQu-k-DJ7BP").build();
		Assistant service = new Assistant("2019-02-08", iamOptions);
		service.setEndPoint("https://gateway.watsonplatform.net/assistant/api");
		ListWorkspacesOptions options = new ListWorkspacesOptions.Builder().build();
		WorkspaceCollection workspaces = service.listWorkspaces(options).execute().getResult();
		String workspaceId=workspaces.getWorkspaces().get(0).getWorkspaceId();
		//Setup.updateAssistantEntities(service, workspaceId);

		//import dataset
		List<List<String>> QAList= readCsvFile("D:\\Desktop\\TESI\\DOMANDERISPOSTE110.csv");
		//Setup.checkClassification(service, workspaceId, QAList);

		//collect entities in Watson Assistant
		//ListEntitiesOptions entitiesOptions = new ListEntitiesOptions.Builder(workspaceId).export(true).build();
		//EntityCollection entitiesResponse = service.listEntities(entitiesOptions).execute().getResult();

		//Watson Discovery connection via API
		IamOptions DiscoveryOptions = new IamOptions.Builder()
				.apiKey("QTB1SHK0vgfpa9EL6UfN9sZlke_3tXr31qp3d6K2sGCy")
				.build();
		Discovery discovery = new Discovery("2019-02-08", DiscoveryOptions);
		discovery.setEndPoint("https://gateway.watsonplatform.net/discovery/api");
		String environmentId = "aae3312f-6177-477d-bf7d-2eabd19a4551";
		ListCollectionsOptions listOptions = new ListCollectionsOptions.Builder(environmentId).build();
		ListCollectionsResponse collListResponse = discovery.listCollections(listOptions).execute().getResult();
		String collectionId= collListResponse.getCollections().get(0).getCollectionId();


		//Watson NLU connection via API
		IamOptions iamOptionsNLU = new IamOptions.Builder()
				.apiKey("HGtAyUOyFqP5BptcId2RV73Mf8z-NeBrn0DYsanupAYz")
				.build();

		NaturalLanguageUnderstanding naturalLanguageUnderstanding = 
				new NaturalLanguageUnderstanding("2018-11-16", iamOptionsNLU);
		naturalLanguageUnderstanding.setEndPoint("https://gateway-lon.watsonplatform.net/natural-language-understanding/api");


		int maxNumberOfPassages=50;
		int numberOfQuestions= QAList.size();
		String question="";
		String category="";
		String intentClassification="";
		List<String> bestPassages= new ArrayList<String>();
		String intentName= "";

		double[] passageRecall = new double[numberOfQuestions];
		double[] passagePrecision = new double[numberOfQuestions];
		int[] positionFrequency = new int[maxNumberOfPassages];
		double[] numberOfPassagesPerQuestion = new double[numberOfQuestions];
		int[] numberOfPassagesPerQuestionFrequency = new int[maxNumberOfPassages+1];
		List<List<String>> results= new ArrayList<List<String>>();
		List<Double> ausRecall= new ArrayList<Double>();
		List<Double> ausPrecision= new ArrayList<Double>();
		List<Double> ausNumberOfPassages= new ArrayList<Double>();


		//mapping question type - entity type required 
		Map<String, List<String>> entitiesForCategory = new HashMap<String, List<String>>();
		List<String> entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Location");
		entitiesToBeAdded.add("Facility");
		entitiesToBeAdded.add("GeographicFeature");
		entitiesForCategory.put("LOCATION", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Organization");
		entitiesToBeAdded.add("Company");
		entitiesForCategory.put("HUMAN_group", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Person");
		entitiesForCategory.put("HUMAN_individual", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Number");
		entitiesToBeAdded.add("Measure");
		entitiesToBeAdded.add("Ordinal");
		entitiesForCategory.put("NUMERIC_count", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Date");
		entitiesForCategory.put("NUMERIC_date", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Money");
		entitiesForCategory.put("NUMERIC_money", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Percent");
		entitiesToBeAdded.add("Measure");
		entitiesForCategory.put("NUMERIC_percent", entitiesToBeAdded);
		entitiesToBeAdded= new ArrayList<String>();
		entitiesToBeAdded.add("Duration");
		entitiesForCategory.put("NUMERIC_period", entitiesToBeAdded);

		int passageCounterToConsiderInAvg=0;
		Map<String, Float> resultPassages= new HashMap<String, Float>();

		//user input from console:
		/*System.out.println("ConversIAmo cerca risposte alle tue curiosità su intelligenza artificiale "
				+ "e argomenti correlati all'interno di un corpus di articoli disponibili online in italiano. "
				+ " Comincia subito con la prima domanda.");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        question = reader.readLine();*/

		//input from test set
		for(int i=0; i<QAList.size(); i++) { 
			question=QAList.get(i).get(1);
			System.out.println(question);
			//category=QAList.get(i).get(2);
			bestPassages.add(QAList.get(i).get(3));
			if(!QAList.get(i).get(4).isEmpty()) {
				bestPassages.add(QAList.get(i).get(4));
			}
			Boolean answerFound=false;
			
			//Discovery results only with stop word removal
			//stop word removal:
		/*	question=removeStopWords(question);
			QueryOptions.Builder queryBuilder = new QueryOptions.Builder(environmentId, collectionId);
			queryBuilder.passagesCount(maxNumberOfPassages);
			queryBuilder.passagesCharacters(500);
			queryBuilder.naturalLanguageQuery(question);
			QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute().getResult();

			IRandSelectionOperation.findRecallPrecisionPositions(i, queryBuilder, question, category, discovery, 
						bestPassages, positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency,
						results, passageRecall, passagePrecision);
			passageCounterToConsiderInAvg++;
			ausRecall.add(passageRecall[i]);
			ausPrecision.add(passagePrecision[i]);
			ausNumberOfPassages.add(numberOfPassagesPerQuestion[i]);
		 */

			List<Map<String,String>> entitiesList = new 
					ArrayList<Map<String,String>>();
			List<String> keywords= new ArrayList<String>();

			MessageInput userInput = new MessageInput();
			userInput.setText(question);
			MessageOptions op = new MessageOptions.Builder(workspaceId)
					.input(userInput)
					.build();
			MessageResponse assistantResponse = service.message(op).execute().getResult();
			if (!assistantResponse.getIntents().isEmpty())
				intentClassification=assistantResponse.getIntents().get(0).getIntent();
			else
				intentClassification="Irrelevant";

			//if an answer is present, print it
			if(assistantResponse.getContext().get("answerFound").toString().contains("true")) {
				System.out.println(assistantResponse.getOutput().get("text")
						.toString().substring(1, assistantResponse.getOutput().get("text").toString().length()-1));
				System.out.println("Questo risponde alla tua domanda? (sì/no): ");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String in = reader.readLine();
				if(in.equalsIgnoreCase("no")) {
					//start all our process to find the most suitable passages in corpus
					resultPassages= askDiscovery(question, naturalLanguageUnderstanding, discovery, environmentId, collectionId, maxNumberOfPassages,
							assistantResponse, intentClassification, bestPassages, i, entitiesList, keywords, 
							positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency, 
							results, passageRecall, passagePrecision, entitiesForCategory,  
							ausRecall, ausPrecision, ausNumberOfPassages);
					passageCounterToConsiderInAvg++;
				}
				else answerFound=true;
			}
			else {
				//start all our process to find the most suitable passages in corpus
				resultPassages= askDiscovery(question, naturalLanguageUnderstanding, discovery, environmentId, collectionId, maxNumberOfPassages,
						assistantResponse, intentClassification, bestPassages, i, entitiesList, keywords, 
						positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency, 
						results, passageRecall, passagePrecision, entitiesForCategory,  
						ausRecall, ausPrecision, ausNumberOfPassages);
				passageCounterToConsiderInAvg++;
			}

			while(!answerFound) {
				//display the final results extracted by the process
				Iterator<String> finalResults= resultPassages.keySet().stream().iterator();
				int position=1;
				System.out.println("La nostra ricerca ha estratto i seguenti risultati: ");
				while(finalResults.hasNext()) {
					System.out.println(position+". "+finalResults.next());
					position++;
				}
				//asks if there is an answer considered as correct
				System.out.println("La risposta corretta è tra queste? (sì/no): ");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String in = reader.readLine();

				if(!in.equalsIgnoreCase("no") && !entitiesList.isEmpty() && !intentName.matches("Irrelevant")) {
					answerFound=true;
					System.out.println("Indica il numero corrispondente: ");
					reader=new BufferedReader(new InputStreamReader(System.in));
					String number = reader.readLine();
					String answer=resultPassages.keySet().toArray()[Integer.valueOf(number)-1].toString();
					//update new entities/concepts found in the question by NLU in Assistant
					ListEntitiesOptions entitiesOptions = new ListEntitiesOptions.Builder(workspaceId).export(true).build();
					EntityCollection entitiesResponse = service.listEntities(entitiesOptions).execute().getResult();
					AssistantOperation.updateAssistantEntitiesFromNLU(entitiesList,entitiesResponse,service, workspaceId);
					//new dialog node with this answer
					AssistantOperation.createDialogNode(workspaceId, service, intentClassification, entitiesList, keywords, answer);
				}
				else {
					//if no answer has been found, it asks for reformulation
					System.out.println("Prova a riformulare la domanda, potresti essere un po' più preciso?");
					reader = new BufferedReader(new InputStreamReader(System.in));
					question = reader.readLine();
					//back to the Assistant inputMessage
					//start all our process to find the most suitable passages in corpus
					resultPassages= askDiscovery(question, naturalLanguageUnderstanding, discovery, environmentId, collectionId, maxNumberOfPassages,
							assistantResponse, intentClassification, bestPassages, i, entitiesList, keywords, 
							positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency, 
							results, passageRecall, passagePrecision, entitiesForCategory,  
							ausRecall, ausPrecision, ausNumberOfPassages);
				}			
			}
			bestPassages.clear();
		}

		//our statistics:
		System.out.println(Arrays.asList(ArrayUtils.toObject(positionFrequency)).toString());
		for(int k=0; k<maxNumberOfPassages/10; k++) {
			System.out.println("le posizioni da "+k+"0 a "+k+"9 contengono: "+Arrays.stream(ArrayUtils.subarray(positionFrequency, k*10, k*10+10)).sum()+" risposte");
		}
		System.out.println("media numero di passaggi trovati per domanda: "+Arrays.stream(numberOfPassagesPerQuestion).sum()/passageCounterToConsiderInAvg);
		System.out.println("std numero di passaggi trovati per domanda: "+Analytics.std(ausNumberOfPassages,passageCounterToConsiderInAvg));
		System.out.println("media recall: "+Arrays.stream(passageRecall).sum()/passageCounterToConsiderInAvg);
		System.out.println("std recall: "+Analytics.std(ausRecall,passageCounterToConsiderInAvg));
		System.out.println("media precision: "+Arrays.stream(passagePrecision).sum()/passageCounterToConsiderInAvg);
		System.out.println("std precision: "+Analytics.std(ausPrecision,passageCounterToConsiderInAvg));

	}
	
	
	public static String removeStopWords(String originalQuestion) throws IOException {
		Set<String> stopWordsSet = new HashSet<String>();
		FileReader fr=new FileReader("D:\\Desktop\\TESI\\stopwords_it.txt");
		BufferedReader br= new BufferedReader(fr);
		String sCurrentLine = br.readLine();
		while (sCurrentLine != null){
			stopWordsSet.add(sCurrentLine);
			sCurrentLine = br.readLine();
		}
		List<String> listOfStrings =new ArrayList<String>(Arrays.asList(originalQuestion.toLowerCase().split("\\s+|,\\s*|\\.\\s|’|'|\\?"))); 
		listOfStrings.removeAll(stopWordsSet);
		return StringUtils.join(listOfStrings, " ");
	}

	private static Map<String, Float> askDiscovery(String question, NaturalLanguageUnderstanding naturalLanguageUnderstanding, Discovery discovery,
			String environmentId, String collectionId, int maxNumberOfPassages,MessageResponse assistantResponse, String intentClassification, 
			List<String> bestPassages, int i, List<Map<String,String>> entitiesList, List<String> keywords, 
			int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, 
			double[] passageRecall, double[] passagePrecision, Map<String, List<String>> entitiesForCategory, 
			List<Double> ausRecall, List<Double> ausPrecision, List<Double> ausNumberOfPassages) throws IOException {
		Map<String, Float> resultPassages= new HashMap<String, Float>();

		//NLU setup
		ConceptsOptions concepts= new ConceptsOptions.Builder()
				.limit(10)
				.build();
		EntitiesOptions entities= new EntitiesOptions.Builder()
				.sentiment(false)
				.limit(100)
				.build();
		KeywordsOptions keywordsOption= new KeywordsOptions.Builder()
				.limit(10)
				.build();
		Features features = new Features.Builder()
				.concepts(concepts)
				.entities(entities)
				.keywords(keywordsOption)
				.build();
		//entities and concepts found by NLU			
		AnalyzeOptions parameters = new AnalyzeOptions.Builder()
				.language("it")
				.text(question)
				.features(features)
				.build();
		AnalysisResults questionAnalysisResponse = naturalLanguageUnderstanding
				.analyze(parameters)
				.execute()
				.getResult();
		if(questionAnalysisResponse.getEntities()!= null) {
			for(int k=0; k<questionAnalysisResponse.getEntities()
					.size(); k++) {
				if(!entitiesList.toString()
						.contains(questionAnalysisResponse.getEntities()
								.get(k).getText())){
					//value not already contained in entitiesList
					Map<String, String> entityMap = new 
							HashMap<String,String>();
					entityMap.put(questionAnalysisResponse
							.getEntities().get(k).getType(),
							questionAnalysisResponse
							.getEntities().get(k).getText());
					entitiesList.add(entityMap);
				}
			}
		}

		//same check for concepts, plus the relevance treshold
		if(questionAnalysisResponse.getConcepts()!=null) {
			for(int k=0; k<questionAnalysisResponse.getConcepts()
					.size(); k++) {
				Map<String, String> conceptMap = new 
						HashMap<String,String>();
				if(questionAnalysisResponse.getConcepts().get(k)
						.getRelevance()>0.9 && !entitiesList.toString()
						.contains(questionAnalysisResponse.getConcepts()
								.get(k).getText())) {
					conceptMap.put("Concept", questionAnalysisResponse
							.getConcepts().get(k).getText());
					entitiesList.add(conceptMap);
				}	
			}
		}
		//check on entities and concepts from Assistant (if present) which NLU could have not extracted
		if (!assistantResponse.getEntities().isEmpty()) {
			for(int e=0; e<assistantResponse.getEntities()
					.size(); e++) {
				if(!entitiesList.toString().toLowerCase()
						.contains(assistantResponse.getEntities().get(e)
								.getValue().toLowerCase())) {
					Map<String, String> ausMap = new 
							HashMap<String,String>();
					ausMap.put(assistantResponse.getEntities().get(e)
							.getEntity(), 
							assistantResponse.getEntities().get(e).getValue());
					entitiesList.add(ausMap);
				}
			}
		}
		//delete possible sub-strings left, going backwards
		List<Map<String, String>> ausEntityList= new ArrayList<Map<String, String>>();
		for(int x=entitiesList.size()-1; x>=0; x--) {
			if(!ausEntityList.toString().toLowerCase().contains(entitiesList.get(x).values().toString().toLowerCase().substring(1,entitiesList.get(x).values().toString().length()-1)))
				ausEntityList.add(entitiesList.get(x));
		}
		entitiesList.clear();
		entitiesList.addAll(ausEntityList);
		System.out.println("entities found:"+entitiesList);

		//keywords found by our Tint-based method
		//keywords= Tint.keywordsExtractionWithTint(question);

		//or keywords found by NLU
		for(int k=0; k<questionAnalysisResponse.getKeywords().size(); k++) {
			if(!keywords.toString().toLowerCase().contains(questionAnalysisResponse.getKeywords().get(k).getText().toLowerCase())){
				keywords.add(questionAnalysisResponse.getKeywords().get(k).getText().toLowerCase());
			}
		} 
		System.out.println("keywords:"+keywords);

		//stop word removal:
		question=removeStopWords(question);

		//Discovery query setup
		QueryOptions.Builder queryBuilder = new QueryOptions.Builder(environmentId, collectionId);
		queryBuilder.passagesCount(maxNumberOfPassages);
		queryBuilder.passagesCharacters(500);


		//matching keywords e ordinamento con entities/concepts per DESCRIPTION:
		if(intentClassification.contains("DESCRIPTION") ||intentClassification.contains("ABBREVIATION")
				||intentClassification.contains("Irrelevant")) {
			resultPassages= IRandSelectionOperation.findRecallPrecisionPositionsKeywordsAndSorting(i, queryBuilder, question, intentClassification, discovery, 
					bestPassages, keywords, entitiesList, positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency,
					results, passageRecall, passagePrecision, features, naturalLanguageUnderstanding);
		}
		else {
			/*if(intentClassification.contains("HUMAN")||intentClassification.contains("LOCATION")) {
			//query expansion with "ground truth" entities
			IRandSelectionOperation.findRecallPrecisionPositionsWithEntities(entitiesForCategory, i, iterator, keywords, queryBuilder, question, intentClassification, discovery, 
					bestPassages, entitiesList, features, naturalLanguageUnderstanding, positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency,
					results, passageRecall, passagePrecision);
			}*/

			//risultati domande normali con filtraggio entity type, matching keywords e ordinamento per score
			resultPassages= IRandSelectionOperation.findRecallPrecisionPositionsWithFilterSortingByScore(naturalLanguageUnderstanding, entitiesList, features,
					entitiesForCategory, i, keywords, queryBuilder, question, intentClassification, discovery, 
					bestPassages, positionFrequency, numberOfPassagesPerQuestion, numberOfPassagesPerQuestionFrequency,
					results, passageRecall, passagePrecision);

		}

		ausRecall.add(passageRecall[i]);
		ausPrecision.add(passagePrecision[i]);
		ausNumberOfPassages.add(numberOfPassagesPerQuestion[i]);

		return resultPassages;
	}

}






