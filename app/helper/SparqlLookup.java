/*Copyright (c) 2019 "hbz"

This file is part of etikett.

etikett is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package helper;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * @author Jan Schnasse
 *
 */
public class SparqlLookup {

    public static String lookup(String rdfAddress, String uri, String labelPredicate, String language, RDFFormat format,
            String accept) {
        String label = lookupLabelInCorrectLanguage(rdfAddress, uri, labelPredicate, language, format, accept);
        if (label != null) {
            return label;
        }
        label = lookupLabelInAnyLanguage(rdfAddress, uri, labelPredicate, format, accept);
        if (label != null) {
            return label;
        }
        return rdfAddress;
    }

    private static String lookupLabelInAnyLanguage(String rdfAddress, String uri, String labelPredicate,
            RDFFormat format, String accept) {
        String queryString = String.format("SELECT ?s ?o {%s <%s> ?o . }", uri, labelPredicate);
        return SparqlLookup.sparqlLabelLookup(rdfAddress, format, accept, queryString);
    }

    private static String lookupLabelInCorrectLanguage(String rdfAddress, String uri, String labelPredicate,
            String language, RDFFormat format, String accept) {
        if (language == null) {
            return null;
        }
        String queryString = String.format("SELECT ?s ?o {%s <%s> ?o . FILTER(LANGMATCHES(lang(?o),'%s'))}", uri,
                labelPredicate, language);
        return SparqlLookup.sparqlLabelLookup(rdfAddress, format, accept, queryString);
    }

    private static String sparqlLabelLookup(String rdfAddress, RDFFormat format, String accept, String queryString) {
        Map<String, String> args = new HashMap<>();
        args.put("accept", accept);
        try (RepositoryConnection con = RdfUtils
                .readRdfInputStreamToRepository(URLUtil.urlToInputStream(new URL(rdfAddress), args), format);) {
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            try (TupleQueryResult qresult = tupleQuery.evaluate()) {
                while (qresult.hasNext()) {
                    BindingSet bindingSet = qresult.next();
                    Value object = bindingSet.getValue("o");
                    if (object instanceof Literal) {
                        return RdfUtils.normalizeLiteral((Literal) object).stringValue();
                    }
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            play.Logger.debug("", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
