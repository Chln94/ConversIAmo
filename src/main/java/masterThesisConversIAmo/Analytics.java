package masterThesisConversIAmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.watson.assistant.v1.Assistant;
import com.ibm.watson.assistant.v1.model.MessageInput;
import com.ibm.watson.assistant.v1.model.MessageOptions;
import com.ibm.watson.assistant.v1.model.MessageResponse;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryPassages;
import com.ibm.watson.discovery.v1.model.QueryResponse;

public class Analytics {
	
	static double std (List<Double> list,  int n) { 
		double sum = 0; 
		for (int i = 0; i < n; i++) 
			sum += list.get(i); 
		double mean = (double)sum /  (double)n; 
		double sqDiff = 0; 
		for (int i = 0; i < n; i++)  
			sqDiff += (list.get(i) - mean) * (list.get(i) - mean); 
		double std= Math.sqrt( (double) sqDiff / n );
		return  std;
	} 
	
	public static void checkClassification(Assistant service, String workspaceId, List<List<String>> QAList) {
		String question="";
		String category="";
		int count=0;
		int irrelevant=0;
		int descrCount=0;
		List<String> bestPassages= new ArrayList<String>();
		MessageInput input = new MessageInput();
		MessageResponse response = null;
		String intentName= "";
		for(int i=0; i<QAList.size(); i++) {
			question=QAList.get(i).get(1);
			category=QAList.get(i).get(2);
			bestPassages.add(QAList.get(i).get(3));
			if(!QAList.get(i).get(4).isEmpty()) {
				bestPassages.add(QAList.get(i).get(4));
			}

			input.setText(question);
			MessageOptions op = new MessageOptions.Builder(workspaceId)
					.input(input)
					.build();	
			response = service.message(op).execute().getResult();

			if(!response.getIntents().isEmpty()) {
				intentName= response.getIntents().get(0).getIntent();
				if(!category.matches(intentName)) {
					System.out.println(intentName);
					System.out.println("sbagliato");
					System.out.println(question+" "+category);
					if(category.matches("DESCRIPTION")) {
						descrCount++;	
					}
					count++;
				}
			}
			else {
				System.out.println(question);
				System.out.println("non classificato");
				irrelevant++;
			}
		}
		System.out.println(count+" classificazioni sbagliate su 110 di cui "+descrCount+" classificazioni di DESCRIPTION sbagliate");
		System.out.println(irrelevant+" classificazioni non avvenute su 110");

	}

	
	public static void findRecallPrecisionPositions(int i, QueryOptions.Builder queryBuilder, String question, String category, Discovery discovery, List<String> bestPassages, int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, double[] passageRecall, double[] passagePrecision){
		double[] recall= {0,0};
		double precision=0;
		double correctOnes=0;
		int count=0;
		int[] position= {-1,-1};

		queryBuilder.naturalLanguageQuery(question);
		QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute().getResult();
		List<String> ausList= new ArrayList<String>();
		List<String> firstThreePassages= new ArrayList<String>();

		Iterator<QueryPassages> it= queryResponse.getPassages().iterator();
		while(it.hasNext()) {
			String passage= it.next().getPassageText();
			if(firstThreePassages.size()<3) firstThreePassages.add(passage);
			if(bestPassages.size()>1) {
				if(recall[0]!=0.5 && passage.contains(bestPassages.get(0).substring(1, bestPassages.get(0).length()-1))) {
					recall[0]=0.5;
					position[0]=count;
					positionFrequency[count] = positionFrequency[count]+1;
					correctOnes++;
					System.out.println("ci sono due possibili passaggi giusti");
					System.out.println("passaggio giusto trovato in posizione:"+position[0]);
				}
				if(recall[1]!=0.5 && passage.contains(bestPassages.get(1).substring(1, bestPassages.get(1).length()-1))) {
					recall[1]=0.5;
					position[1]=count;
					positionFrequency[count] = positionFrequency[count]+1;
					correctOnes++;
					System.out.println("ci sono due possibili passaggi giusti");
					System.out.println("passaggio giusto trovato in posizione:"+position[1]);
				}
				count++;
			}
			else {
				if(recall[0]!= 1.0 && passage.contains(bestPassages.get(0).substring(1, bestPassages.get(0).length()-1))) {
					recall[0]=1.0 ;
					position[0]=count;
					positionFrequency[count] = positionFrequency[count]+1;
					correctOnes++;
					System.out.println("passaggio giusto trovato in posizione:"+position[0]);
				}
				count++;
			}


		}

		System.out.println("totale passaggi trovati:"+count);
		System.out.println("recall della domanda "+(i+1)+": "+(recall[0]+recall[1]));	
		precision=(correctOnes/count);
		System.out.println("precision della domanda "+(i+1)+": "+precision);	
		ausList.add(question);
		ausList.add(category);
		ausList.add(String.valueOf((recall[0]+recall[1])));
		ausList.add(String.valueOf(precision));
		results.add(ausList);
		passageRecall[i]= (recall[0]+recall[1]);
		passagePrecision[i]= precision;
		numberOfPassagesPerQuestion[i]=count;
		numberOfPassagesPerQuestionFrequency[count] +=1;
		count=0;
		precision=0;
		correctOnes=0;
		recall[0] = recall[1] =0;
		position[0]= position[1]=-1;

	}
	
	
	static void checkIfContainsBestPassages(int maxKeywordsNumber, HashMap<Integer, List<String>> map, List<String> bestPassages, int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, double[] passageRecall, double[] passagePrecision, String question, String category, int i ) {
		double[] recall= {0,0};
		double precision=0;
		double correctOnes=0;
		int count=0;
		int[] position= {-1,-1};

		List<String> ausList= new ArrayList<String>();

		for(int m=0; m<map.size(); m++) {

			Iterator<String> it= map.get(maxKeywordsNumber-m).iterator();
			while(it.hasNext()) {
				String passage= it.next();
				if(bestPassages.size()>1) {
					if(recall[0]!=0.5 && passage.contains(bestPassages.get(0).substring(1, bestPassages.get(0).length()-1))) {
						recall[0]=0.5;
						position[0]=count;
						positionFrequency[count] = positionFrequency[count]+1;
						correctOnes++;
						System.out.println("ci sono due possibili passaggi giusti");
						System.out.println("passaggio giusto trovato in posizione:"+position[0]);
						//System.out.println(passage);
					}
					if(recall[1]!=0.5 && passage.contains(bestPassages.get(1).substring(1, bestPassages.get(1).length()-1))) {
						recall[1]=0.5;
						position[1]=count;
						positionFrequency[count] = positionFrequency[count]+1;
						correctOnes++;
						System.out.println("ci sono due possibili passaggi giusti");
						System.out.println("passaggio giusto trovato in posizione:"+position[1]);
					}
					count++;
				}
				else {
					if(recall[0]!= 1.0 && passage.contains(bestPassages.get(0).substring(1, bestPassages.get(0).length()-1))) {
						recall[0]=1.0 ;
						position[0]=count;
						positionFrequency[count] = positionFrequency[count]+1;
						correctOnes++;
						System.out.println("passaggio giusto trovato in posizione:"+position[0]);
						//bestContained=true;
					}
					count++;
				}

			}
		}

		System.out.println("totale passaggi trovati:"+count);
		System.out.println("recall della domanda "+(i+1)+": "+(recall[0]+recall[1]));	
		precision=(correctOnes/count);
		System.out.println("precision della domanda "+(i+1)+": "+precision);	

		ausList.add(question);
		ausList.add(category);
		ausList.add(String.valueOf((recall[0]+recall[1])));
		ausList.add(String.valueOf(precision));
		results.add(ausList);
		passageRecall[i]= (recall[0]+recall[1]);
		passagePrecision[i]= precision;
		numberOfPassagesPerQuestion[i]=count;
		numberOfPassagesPerQuestionFrequency[count] +=1;
		count=0;
		precision=0;
		correctOnes=0;
		recall[0] = recall[1] =0;
		position[0]= position[1]=-1;
	}
	
	static void checkIfContainsBestPassagesScore(Map<String, Float> resultPassages, List<String> bestPassages, int[] positionFrequency, double[] numberOfPassagesPerQuestion, int[] numberOfPassagesPerQuestionFrequency, List<List<String>> results, double[] passageRecall, double[] passagePrecision, String question, String category, int i) {

		double[] recall= {0,0};
		double precision=0;
		double correctOnes=0;
		int count=0;
		int[] position= {-1,-1};

		List<String> ausList= new ArrayList<String>();

		Iterator<String> it= resultPassages.keySet().iterator();
		while(it.hasNext()) {
			String passage= it.next();
			if(bestPassages.size()>1) {
				if(recall[0]!=0.5 && passage.contains(bestPassages.get(0).substring(1, bestPassages.get(0).length()-1))) {
					recall[0]=0.5;
					position[0]=count;
					positionFrequency[count] = positionFrequency[count]+1;
					correctOnes++;
					System.out.println("ci sono due possibili passaggi giusti");
					System.out.println("passaggio giusto trovato in posizione:"+position[0]);
				}
				if(recall[1]!=0.5 && passage.contains(bestPassages.get(1).substring(1, bestPassages.get(1).length()-1))) {
					recall[1]=0.5;
					position[1]=count;
					positionFrequency[count] = positionFrequency[count]+1;
					correctOnes++;
					System.out.println("ci sono due possibili passaggi giusti");
					System.out.println("passaggio giusto trovato in posizione:"+position[1]);
				}
				count++;

			}
			else {
				if(recall[0]!= 1.0 && passage.contains(bestPassages.get(0).substring(1, bestPassages.get(0).length()-1))) {
					recall[0]=1.0 ;
					position[0]=count;
					positionFrequency[count] = positionFrequency[count]+1;
					correctOnes++;
					System.out.println("passaggio giusto trovato in posizione:"+position[0]);
					//bestContained=true;
				}
				count++;
			}

		}

		System.out.println("totale passaggi trovati:"+count);
		System.out.println("recall della domanda "+(i+1)+": "+(recall[0]+recall[1]));	
		precision=(correctOnes/count);
		System.out.println("precision della domanda "+(i+1)+": "+precision);	

		ausList.add(question);
		ausList.add(category);
		ausList.add(String.valueOf((recall[0]+recall[1])));
		ausList.add(String.valueOf(precision));
		results.add(ausList);
		passageRecall[i]= (recall[0]+recall[1]);
		passagePrecision[i]= precision;
		numberOfPassagesPerQuestion[i]=count;
		numberOfPassagesPerQuestionFrequency[count] +=1;
		count=0;
		precision=0;
		correctOnes=0;
		recall[0] = recall[1] =0;
		position[0]= position[1]=-1;

	}
}
