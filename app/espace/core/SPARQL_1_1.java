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


package espace.core;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

public class SPARQL_1_1 {
	
	protected final static Logger log = Logger.getLogger(Repository.class);
	private String sparqlEndpoint;
	
	
	/**
	 * Sets the sparql endpoint to which queries and updates are performed.
	 * @param sparqlEndpoint
	 */
	public SPARQL_1_1(String sparqlEndpoint) {
		this.sparqlEndpoint = sparqlEndpoint;
	}

	/**
	 * Performs an update SPAQRL query to a remote endpoint
	 * @param repository - the update endpoint
	 * @param query - the update SPARQL query
	 */
	public static void update(String repository,String query){
		UpdateRequest queryObj = UpdateFactory.create(query); 
		UpdateProcessor qexec = UpdateExecutionFactory.createRemote(queryObj, repository); 
		qexec.execute(); 
	}
	/**
	 * Query a SPARQL repository and get the results in a ResultSet
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a ResultSet containing the values for all the variables set in the SPARQL query
	 */
	public static ResultSet query(String repository, String query) {
		QueryExecution e = QueryExecutionFactory.sparqlService(repository, query);
		ResultSet results = e.execSelect();
		e.close();		
		return results;
	}
	
	
	/**
	 * Query a SPARQL repository and get the results in a ResultSet in JSON format.
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a ResultSet containing the values for all the variables set in the SPARQL query in JSON Format
	 */
	public static ResultSet queryJSON(String repository, String query) {
		QueryExecution e = QueryExecutionFactory.sparqlService(repository, query);
		ResultSet results = e.execSelect();
		ResultSetFormatter.outputAsJSON(results);
		e.close();		
		return results;
	}
	/**
	 * Query a SPARQL repository and get a list of the results' first bound variable as string
	 * @param repository - the SPARQL endpoint
	 * @param query - the SPARQL query
	 * @return a list containing the results for the first variable set in the SPAQRL query
	 */
	public static List<String> queryList(String repository, String query) {
		ArrayList<String> values = new ArrayList<String>();
		QueryExecution e = QueryExecutionFactory.sparqlService(repository, query);
		ResultSet results = e.execSelect();
		String variable = results.getResultVars().get(0);
		
		while(results.hasNext()) {
			QuerySolution solution = results.nextSolution();
			RDFNode result = solution.get(variable);
			if(result != null) values.add(result.toString());
		}
		
		e.close();
		
		return values;		
	}
	
	/**
	 * Query a SPARQL repository and get the first result as map with variables as keys 
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a Map with the first result with variables as keys
	 */
	public static Map<String, String> queryMap(String repository, String query) {
		HashMap<String, String> map = new HashMap<String, String>();
		QueryExecution e = QueryExecutionFactory.sparqlService(repository, query);
		ResultSet results = e.execSelect();
		
		for(String variable : results.getResultVars()) {
			if(results.hasNext()) {
				QuerySolution solution = results.nextSolution();
				RDFNode result = solution.get(variable);
				if(result != null) map.put(variable, result.toString());
			}
		}
		
		e.close();

		return map;
	}

	/**
	 * Query a SPARQL repository and get the first result's first bound variable as string 
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a String with the first result's first bound variable 
	 */
	public static String queryFirst(String repository, String query) {
		String value = null;
		
		QueryExecution e = QueryExecutionFactory.sparqlService(repository, query);
		ResultSet results = e.execSelect();
		
		String variable = results.getResultVars().get(0);
		
		if(results.hasNext()) {
			QuerySolution solution = results.nextSolution();
			RDFNode result = solution.get(variable);
			if(result != null) value = result.toString();
		}
		
		e.close();

		return value;
	}


	/**
	 * Query a SPARQL repository and get the results as a list of maps with bound variables as keys. 
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a List of Maps with bound variables as keys
	 */
	public static List<Map<String, String>> queryMapList(String repository, String query) {
		log.debug(query);
		ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

		QueryExecution e = QueryExecutionFactory.sparqlService(repository, query);
		ResultSet results = e.execSelect();
		
		while(results.hasNext()) {
			QuerySolution solution = results.nextSolution();
			HashMap<String, String> map = new HashMap<String, String>();

			for(String variable : results.getResultVars()) {
				RDFNode result = solution.get(variable);
				if(result != null) map.put(variable, result.toString());
			}

			list.add(map);
		}
		
		return list;
	}
	
	/**
	 * Query a SPARQL repository and get the results in a ResultSet
	 * @param query - the SPARQL query
	 * @return a ResultSet with the results
	 */
	public ResultSet query(String query) {
		return Repository.query(this.sparqlEndpoint, query);
	}

	/**
	 * Query a SPARQL repository and get the first result's first bound variable as string. 
	 * @param query - the SPARQL query
	 * @return a String with the first result's first bound variable 
	 */
	public String queryFirst(String query) {
		return Repository.queryFirst(this.sparqlEndpoint, query);
	}

	/**
	 * Query a SPARQL repository and get a list of the results' first bound variable as string. 
	 * @param query - the SPARQL query
	 * @return a list containing the results for the first variable set in the SPAQRL query
	 */
	public List<String> queryList(String query) {
		return Repository.queryList(this.sparqlEndpoint, query);
	}

	/**
	 * Query a SPARQL repository and get the first result as map with variables as keys. 
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a Map with bound variables as keys
	 */
	public Map<String, String> queryMap(String query) {
		return Repository.queryMap(this.sparqlEndpoint, query);
	}
	
	/**
	 * Query a SPARQL repository and get the results as a list of maps with bound variables as keys. 
	 * @param repository - the SPARQL endpoint 
	 * @param query - the SPARQL query
	 * @return a List of Maps with bound variables as keys
	 */
	public List<Map<String, String>> queryMapList(String query) {
		return Repository.queryMapList(this.sparqlEndpoint, query);
	}
	
		
	
	public static void main(String args[]){
		SPARQL_1_1 endpoint = new SPARQL_1_1("http://panic.image.ece.ntua.gr:3030/fashionSem/sparql");
		
		String query = "PREFIX edm:<http://www.europeana.eu/schemas/edm/> \n"+
					   "PREFIX ore:<http://www.openarchives.org/ore/terms/> \n"+
					   "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n"+
					   "PREFIX dc:<http://purl.org/dc/elements/1.1/> \n"+
					   "PREFIX dcterms:<http://purl.org/dc/terms/> \n"+

					   "select distinct ?s \n"+
					   "where { \n"+
					   "    ?s a edm:ProvidedCHO. \n"+
					   "    { OPTIONAL { ?s  dc:title ?title.  Filter regex(?title, \"Advert\")} } UNION \n"+
					   "    { OPTIONAL { ?s  dc:description ?description.  Filter regex(?description, \"Advert\")} } UNION \n"+
					   "    { OPTIONAL { ?s  dc:creator ?creator. Filter regex(?description, \"Advert\") } } UNION \n"+
					   "    { OPTIONAL { ?s  dc:identifier ?identifier. Filter regex(?description, \"Advert\") } } \n"+
					   "} ";
	}

}
