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


package sources.core;



import java.util.StringTokenizer;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class FashionSparqlEndpoint {
	
	String sparqlEndpoint = "http://localhost:3030/thesauri/sparql";
//	String sparqlEndpoint = "http://panic.image.ntua.gr:3030/fashionSem/sparql";
	String prefixes = "PREFIX edm:<http://www.europeana.eu/schemas/edm/> \n"+
					  "PREFIX ore:<http://www.openarchives.org/ore/terms/> \n"+
					  "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n"+
					  "PREFIX dc:<http://purl.org/dc/elements/1.1/> \n"+
					  "PREFIX dcterms:<http://purl.org/dc/terms/> \n\n";
	
	String searchTerm;
	
	public static void main(String args[]){
		FashionSparqlEndpoint rep = new FashionSparqlEndpoint();
		//This is the term to search for
		String termToSearch = "Advert";
		
		//Set the search term
		rep.setSearchTerm(termToSearch);
		
		//First get the number of results for the given term
		int res = rep.getResultsSize();
//		System.out.println(res);
		
		//Get the results for WITH
		//Have a look at this method to get the values you need for the JSON Response
		rep.getResults(5,10);
		
	}
	
	public int getResultsSize(){
		
		String select = "select (count(distinct ?s) as ?count) \n";
		String where = "where { \n"+
					   "    ?s a edm:ProvidedCHO. \n"+
					   "    { OPTIONAL { ?s  dc:title ?title.  Filter regex(?title, \""+getSearchTerm()+"\")} } UNION \n"+
					   "    { OPTIONAL { ?s  dc:description ?description.  Filter regex(?description, \""+getSearchTerm()+"\")} } UNION \n"+
					   "    { OPTIONAL { ?s  dc:creator ?creator. Filter regex(?creator, \""+getSearchTerm()+"\") } } UNION \n"+
					   "    { OPTIONAL { ?s  dc:identifier ?identifier. Filter regex(?identifier, \""+getSearchTerm()+"\") } } \n"+
					   "}";		
		
		int resultsSize = -1;
		
		Query query = QueryFactory.create(prefixes+select+where);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
		ResultSet results = qexec.execSelect();
		
		while(results.hasNext()){
			QuerySolution solution = results.nextSolution();
			RDFNode result = solution.get("?count");
			resultsSize = result.asLiteral().getInt();
		}
		qexec.close() ;
		
		return resultsSize;
		
	}

	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}
	
	public void getResults(int offset, int size){
		String select = "select distinct ?s ?title ?creator ?description ?dataProvider ?thumb ?rights \n";
		String where = "where { \n"+
				"    ?s a edm:ProvidedCHO. \n"+
				"    ?aggr edm:aggregatedCHO ?s. \n"+
				"    ?aggr edm:rights ?rights. \n"+
				"    ?aggr edm:dataProvider ?dataProvider. \n"+
				"    ?aggr edm:isShownBy ?thumb. \n"+
				"    { OPTIONAL { ?s  dc:title ?title.  Filter regex(?title, \""+getSearchTerm()+"\")} }  \n"+
				"    { OPTIONAL { ?s  dc:description ?description.  Filter regex(?description, \""+getSearchTerm()+"\")} }  \n"+
				"    { OPTIONAL { ?s  dc:creator ?creator. Filter regex(?creator, \""+getSearchTerm()+"\") } }  \n"+
				"    { OPTIONAL { ?s  dc:identifier ?identifier. Filter regex(?identifier, \""+getSearchTerm()+"\") } } \n"+
				"}";		
		String restrictions = "\n OFFSET "+offset+" LIMIT "+size;
		Query query = QueryFactory.create(prefixes+select+where+restrictions);
		
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
		ResultSet results = qexec.execSelect();
		
		while(results.hasNext()){
			QuerySolution solution = results.nextSolution();
			String s = initFromResource(solution,"?s");  
			String title = initFromLiteral(solution,"?title");
			String creator = initFromLiteral(solution,"?creator");
			String description = initFromLiteral(solution,"?description");
			String dataProvider = initFromResource(solution,"?dataProvider"); 
			String thumb = initFromResource(solution,"?thumb");
			String rigths = initFromResource(solution,"?rights");   
		}
		qexec.close() ;

	}

	private String getDataProvider(String dataProvider) {
		System.out.println(dataProvider);
		String cleanDP = dataProvider.replace("http://mint-projects.image.ntua.gr/europeana-fashion/", "");
		StringTokenizer st = new StringTokenizer(cleanDP,"_");
		for(int i=0; i< 5; i++)
			st.nextToken();
		
		return st.nextToken();
	}

	private String getURL(String uri) {
		return "http://www.europeanafashion.eu/record/a/"+getIdentifier(uri);
	}

	private String getIdentifier(String uri) {
		String cleanUri = uri.replace("http://mint-projects.image.ntua.gr/europeana-fashion/", "");
		StringTokenizer st = new StringTokenizer(cleanUri,"_");
		st.nextToken();
		return st.nextToken();
	}

	private String initFromResource(QuerySolution solution, String var) {
		
		if(solution.get(var) != null)
			return solution.get(var).asResource().getURI();
		return "Unknown";
	}

	private String initFromLiteral(QuerySolution solution, String var) {
		if(solution.get(var) != null)
			return solution.get(var).asLiteral().getString();
		return "Unknown";
	}
	
}
