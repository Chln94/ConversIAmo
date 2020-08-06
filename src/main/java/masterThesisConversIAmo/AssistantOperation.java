package masterThesisConversIAmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.cloud.sdk.core.service.exception.BadRequestException;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.assistant.v1.Assistant;
import com.ibm.watson.assistant.v1.model.CreateDialogNodeOptions;
import com.ibm.watson.assistant.v1.model.CreateEntity;
import com.ibm.watson.assistant.v1.model.CreateEntityOptions;
import com.ibm.watson.assistant.v1.model.CreateValue;
import com.ibm.watson.assistant.v1.model.DialogNode;
import com.ibm.watson.assistant.v1.model.DialogNodeCollection;
import com.ibm.watson.assistant.v1.model.DialogNodeOutput;
import com.ibm.watson.assistant.v1.model.DialogNodeOutputGeneric;
import com.ibm.watson.assistant.v1.model.DialogNodeOutputTextValuesElement;
import com.ibm.watson.assistant.v1.model.Entity;
import com.ibm.watson.assistant.v1.model.EntityCollection;
import com.ibm.watson.assistant.v1.model.GetDialogNodeOptions;
import com.ibm.watson.assistant.v1.model.ListDialogNodesOptions;
import com.ibm.watson.assistant.v1.model.UpdateDialogNodeOptions;
import com.ibm.watson.assistant.v1.model.UpdateEntityOptions;
import com.ibm.watson.assistant.v1.model.UpdateWorkspaceOptions;
import com.ibm.watson.assistant.v1.model.Value;
import com.ibm.watson.assistant.v1.model.Workspace;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.ListCollectionsOptions;
import com.ibm.watson.discovery.v1.model.ListCollectionsResponse;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryResponse;

import edu.stanford.nlp.util.StringUtils;

public class AssistantOperation {

	public static void updateAssistantEntities(Assistant service,String workspaceId) {
		IamOptions options = new IamOptions.Builder()
				.apiKey("QTB1SHK0vgfpa9EL6UfN9sZlke_3tXr31qp3d6K2sGCy")
				.build();
		Discovery discovery = new Discovery("2019-02-08", options);
		discovery.setEndPoint("https://gateway.watsonplatform.net/discovery/api");

		String environmentId = "aae3312f-6177-477d-bf7d-2eabd19a4551";
		ListCollectionsOptions listOptions = new ListCollectionsOptions.Builder(environmentId).build();
		ListCollectionsResponse collListResponse = discovery.listCollections(listOptions).execute().getResult();
		String collectionId= collListResponse.getCollections().get(0).getCollectionId();
		QueryOptions.Builder queryBuilder = new QueryOptions.Builder(environmentId, collectionId);
		queryBuilder.naturalLanguageQuery("");
		queryBuilder.count(1000);
		QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute().getResult();

		JsonParser parser;
		JsonObject json, entity;
		JsonElement entities, concepts;
		List<String> people = new ArrayList<String>();
		List<String> locations= new ArrayList<String>();
		List<String> organizations= new ArrayList<String>();
		List<String> companies= new ArrayList<String>();
		List<String> healthConditions= new ArrayList<String>();
		List<String> sports= new ArrayList<String>();
		List<String> media= new ArrayList<String>();
		List<String> naturalEvent= new ArrayList<String>();
		List<String> facilities= new ArrayList<String>();
		List<String> movies= new ArrayList<String>();


		for(int i=0; i<queryResponse.getMatchingResults(); i++) {
			parser = new JsonParser();
			json = (JsonObject) parser.parse(queryResponse.getResults().get(i).toString());
			entities = json.get("enriched_text").getAsJsonObject().get("entities");
			for (int j=0; j< entities.getAsJsonArray().size(); j++) {
				entity=entities.getAsJsonArray().get(j).getAsJsonObject();
				String value=entity.get("text").toString();
				String type=entity.get("type").toString();
				switch(type) {
				case "Person": {people.add(value); break;}
				case "Location": {locations.add(value); break;}
				case "GeographicFeature": {locations.add(value); break;}
				case "Organization": {organizations.add(value); break;}
				case "Company": {companies.add(value); break;}
				case "HealthCondition": {healthConditions.add(value); break;}
				case "Sport": {sports.add(value); break;}
				case "PrintMedia": {media.add(value); break;}
				case "Broadcaster": {media.add(value); break;}
				case "TelevisionShow": {media.add(value); break;}
				case "NaturalEvent": {naturalEvent.add(value); break;}
				case "Facility": {facilities.add(value); break;}
				case "Movie": {movies.add(value); break;}
				}
			}
		}

		List<String> conceptslist= new ArrayList<String>();
		String concept;
		for(int i=0; i<queryResponse.getMatchingResults(); i++) {
			parser = new JsonParser();
			json = (JsonObject) parser.parse(queryResponse.getResults().get(i).toString());
			concepts = json.get("enriched_text").getAsJsonObject().get("concepts");
			for (int j=0; j< concepts.getAsJsonArray().size(); j++) {
				concept= concepts.getAsJsonArray().get(j).getAsJsonObject().get("text").toString();
				conceptslist.add(concept.substring(1, concept.length()-1));
			}
		}

		List<CreateEntity> ents= new ArrayList<CreateEntity>();
		List<CreateValue> peopleEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,peopleEntityValues, people, "Person");
		List<CreateValue> locationsEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,locationsEntityValues, locations, "Location");
		List<CreateValue> oranizationsEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,oranizationsEntityValues, organizations, "Organization");
		List<CreateValue> companiesEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,companiesEntityValues, companies, "Company");
		List<CreateValue> healthConditionsEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,healthConditionsEntityValues, healthConditions, "HealthCondition");
		List<CreateValue> sportsEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,sportsEntityValues, sports, "Sport");
		List<CreateValue> mediaEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,mediaEntityValues, media, "Media");
		List<CreateValue> naturalEventsEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,naturalEventsEntityValues, naturalEvent, "NaturalEvent");
		List<CreateValue> facilitiesEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,facilitiesEntityValues, facilities, "Facility");
		List<CreateValue> moviesEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,moviesEntityValues, movies, "Movie");

		List<CreateValue> conceptsEntityValues = new ArrayList<CreateValue>();
		ents= createNewEntity(ents,conceptsEntityValues, conceptslist, "Concept");

		UpdateWorkspaceOptions newoptions = new UpdateWorkspaceOptions.Builder(workspaceId)
				.entities(ents)
				.build();

		Workspace workspaceResponse = service.updateWorkspace(newoptions).execute().getResult();
		System.out.println(workspaceResponse);

	}

	public static List<CreateEntity> createNewEntity(List<CreateEntity> ents, List<CreateValue> listEntityValues, 
			List<String> list, String entityName) {

		Iterator<CreateValue> iter;
		CreateValue alreadyInList;
		String newValue;
		Boolean inserted;
		List<String> syn;

		Set set=new TreeSet(String.CASE_INSENSITIVE_ORDER);
		set.addAll(list);
		list= new ArrayList(set);
		for(int i=0; i<list.size(); i++) {
			newValue=list.get(i);
			iter= listEntityValues.listIterator();
			inserted=false;

			while(iter.hasNext()) {
				syn= new ArrayList<String>();
				alreadyInList=iter.next();
				if(!alreadyInList.value().matches(newValue) && 
						(alreadyInList.value().equalsIgnoreCase(newValue) || alreadyInList.value().contains(newValue) 
								|| newValue.contains(alreadyInList.value())) ){
					if(!alreadyInList.synonyms().isEmpty())
						syn = alreadyInList.synonyms();
					syn.add(alreadyInList.value());
					iter.remove();
					listEntityValues.add(new CreateValue.Builder().value(newValue).synonyms(syn).build());
					inserted=true;
					iter= listEntityValues.listIterator();
				}
			}

			if(!inserted) 
				listEntityValues.add(new CreateValue.Builder().value(newValue).synonyms(new ArrayList<String>()).build());

		}

		List<String> ausSyn;
		List<CreateValue> finalVersion= new ArrayList<CreateValue>();
		for(int i=0; i<listEntityValues.size(); i++) {
			ausSyn = new ArrayList<String>();
			ausSyn.addAll(listEntityValues.get(i).synonyms());
			for(int j=i+1; j<listEntityValues.size(); j++) {
				if(!ausSyn.contains(listEntityValues.get(j).synonyms()) && 
						listEntityValues.get(i).value().equalsIgnoreCase(listEntityValues.get(j).value())) {
					ausSyn.addAll(listEntityValues.get(j).synonyms());
				}
			}

			set=new TreeSet(String.CASE_INSENSITIVE_ORDER);
			set.addAll(ausSyn);
			ausSyn= new ArrayList(set);
			finalVersion.add(new CreateValue.Builder().value(listEntityValues.get(i).value()).synonyms(ausSyn).build());

		}

		CreateEntity entityToBeAdded= new CreateEntity.Builder()
				.entity(entityName)
				.fuzzyMatch(true)
				.values(finalVersion)
				.build();

		ents.add(entityToBeAdded);
		return ents;
	}

	
	public static void createDialogNode(String workspaceId, Assistant service, String intentName, List<Map<String,String>> entitiesList, List<String> keywords, 
			String answer) {
		//looks for the correct parent node
		String parentDialogNode="";
		ListDialogNodesOptions listnodeopt = new ListDialogNodesOptions.Builder(workspaceId).build();
		DialogNodeCollection listresp = service
				.listDialogNodes(listnodeopt)
				.execute().getResult();
		Iterator<DialogNode> dialogNodesIt = listresp
				.getDialogNodes().iterator();
		while(dialogNodesIt.hasNext()) {
			DialogNode node= dialogNodesIt.next();
			String title=node.title();
			if(title!=null && title.matches(intentName))
				parentDialogNode = node.dialogNode();
		}
		//setting the new node condition
		String conditions = "#"+intentName;
		Iterator<Map<String, String>> entities= entitiesList
				.iterator();
		while(entities.hasNext()) {
			Map<String,String> entityMap= entities.next();
			conditions= conditions+" and @"+entityMap.keySet()
			.toString().substring(1,entityMap
					.keySet().toString().length()-1)+":("
					+entityMap.values().toString().
					substring(1,entityMap.values().
							toString().length()-1)+")";
		}
		//joins the keywords as new dialog node name
		String dialogNode = StringUtils.join(keywords, " ");
		//setting of the output
		DialogNodeOutput output = new DialogNodeOutput();
		List<DialogNodeOutputGeneric> generic = new ArrayList<DialogNodeOutputGeneric>();
		DialogNodeOutputGeneric dialogNodeOutputGeneric= new DialogNodeOutputGeneric();
		dialogNodeOutputGeneric.setResponseType("text");
		dialogNodeOutputGeneric.setSelectionPolicy("sequential");
		DialogNodeOutputTextValuesElement dialogNodeOutputTextValuesElement=new DialogNodeOutputTextValuesElement();
		dialogNodeOutputTextValuesElement.setText(answer);
		//only one type of response 
		List<DialogNodeOutputTextValuesElement> values= new ArrayList<DialogNodeOutputTextValuesElement>();
		values.add(dialogNodeOutputTextValuesElement);
		dialogNodeOutputGeneric.setValues(values);
		generic.add(dialogNodeOutputGeneric);
		output.setGeneric(generic);
		//settings of the context variable $answerFound
		Map<String, Object> context =new 
				HashMap<String, Object>();
		context.put("answerFound", "true");
		try{
			//dialog node creation
			CreateDialogNodeOptions nodeOptions = new CreateDialogNodeOptions.Builder(workspaceId, dialogNode)
					.conditions(conditions)
					.title(dialogNode)
					.parent(parentDialogNode)
					.output(output)
					.build();
			DialogNode nodeResponse = service.createDialogNode(nodeOptions).execute().getResult();
			System.out.println("Risposta correttamente inserita nel sistema. Grazie.");
		}catch(BadRequestException e) {
			//dialog node update
			GetDialogNodeOptions options = new GetDialogNodeOptions.Builder(workspaceId, dialogNode).build();
			if(service.getDialogNode(options).execute().getResult().dialogNode().matches(dialogNode)) {
				List<DialogNodeOutputTextValuesElement> newValues = service.getDialogNode(options).execute().getResult().output().getGeneric().get(0).getValues();
				if(!newValues.toString().contains(answer)) {
					newValues.addAll(values);
					output.clear();
					dialogNodeOutputGeneric.setValues(newValues);
					output.setGeneric(generic);
					UpdateDialogNodeOptions updateOptions = new UpdateDialogNodeOptions.Builder(workspaceId, dialogNode)
							.newOutput(output)
							.build();
					DialogNode nodeResponse = service.updateDialogNode(updateOptions).execute().getResult();
					System.out.println("Risposta correttamente inserita nel sistema. Grazie.");
				}
			}
		}

	}



	public static void updateAssistantEntitiesFromNLU(List<Map<String, String>> entitiesList, 
			EntityCollection entitiesResponse, Assistant service, String workspaceId) {
		//iterates on question entities
		Iterator<Map<String, String>> entitiesListIt= entitiesList.iterator();
		ListIterator<Entity> entitiesResponseIt;
		while(entitiesListIt.hasNext()) {
			Map<String, String> entity= entitiesListIt.next();
			String entityType = entity.keySet().toString()
					.substring(1,entity.keySet().toString()
							.length()-1);
			String entityValue = entity.values().toString()
					.toLowerCase().substring(1,entity.values()
							.toString().length()-1);
			//checks if entity type is already present in Assistant ones
			if(entitiesResponse.getEntities().toString()
					.contains(entityType)) {
				//iterates on the existing Assistant entities
				entitiesResponseIt= entitiesResponse
						.getEntities().listIterator();
				while(entitiesResponseIt.hasNext()) {
					Entity current= entitiesResponseIt.next();
					//checks if the entity already has this value 
					if( (current.getEntity().matches(entityType) 
							&& !current.getValues().toString()
							.toLowerCase().contains(entityValue)) 
							|| (current.getEntity().matches("Location") 
									&& entityType.matches("GeographicFeature")) 
							|| (current.getEntity().matches("Media") && 
									entityType.matches("PrintMedia") || 
									entityType.matches("Broadcaster") || 
									entityType.matches("TelevisionShow"))) {
						//the value has to be added
						List<Value> oldValues= current.getValues();
						CreateValue createVal;
						List<CreateValue> newValues = new ArrayList<CreateValue>();
						for(int y=0; y< oldValues.size(); y++){
							createVal = new CreateValue.Builder()
									.value(oldValues.get(y).value())
									.build();
							newValues.add(createVal);
						}
						CreateValue newValue= new CreateValue.Builder().value(entity.values().toString()
								.substring(1,entity.values().toString().length()-1)).build();
						newValues.add(newValue);
						UpdateEntityOptions updateEntityOptions = new UpdateEntityOptions
								.Builder(workspaceId, current.getEntity())
								.newValues(newValues)
								.build();
						Entity updateRes = service
								.updateEntity(updateEntityOptions)
								.execute().getResult();
					}
				}
			}
			else { 
				//if entity type is not present in Assistant yet
				List<CreateValue> entityValues = new ArrayList<CreateValue>();
				entityValues.add(new CreateValue.Builder(entity
						.values().toString().substring(1,entity
								.values().toString().length()-1))
						.build());
				String newEntityName= entity.keySet().toString()
						.substring(1,entity.keySet().toString()
								.length()-1);
				CreateEntityOptions newEntityOptions = new CreateEntityOptions.Builder(workspaceId, newEntityName).values(entityValues).build();

				Entity newEntityRes = service
						.createEntity(newEntityOptions).execute()
						.getResult();
			}
		}	
	}
	
}
