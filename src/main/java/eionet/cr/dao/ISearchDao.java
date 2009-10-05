/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Aleksandr Ivanov, Tieto Eesti
 */
package eionet.cr.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import eionet.cr.dto.RawTripleDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.search.SearchException;
import eionet.cr.search.util.SearchExpression;
import eionet.cr.util.PageRequest;
import eionet.cr.util.Pair;
import eionet.cr.util.SortingRequest;

/**
 * Interface to define search related dao methods.
 * 
 * @author Aleksandr Ivanov
 * <a href="mailto:aleksandr.ivanov@tietoenator.com">contact</a>
 */
public interface ISearchDao extends IDao{

	/**
	 * performs spatial sources search.
	 * 
	 * @return
	 * @throws DAOException
	 */
	List<String> performSpatialSourcesSearch() throws DAOException;
	
	/**
	 * Newly rewritten simple search.
	 * 
	 * @param expression - search expression to find
	 * @param pageRequest - page request
	 * @param sortingRequest - sorting request to set
	 * @return
	 * @throws Exception
	 */
	Pair<Integer, List<SubjectDTO>> performSimpleSearch(
				SearchExpression expression,
				PageRequest pageRequest,
				SortingRequest sortingRequest) throws Exception;
	
	/**
	 * Newly rewritten custom search.
	 * 
	 * @param criteria - custom search criteria.
	 * @param literalPredicates - set of literal predicates
	 * @param pageRequest - page request
	 * @param sortingRequest - sorting request
	 * @return
	 * @throws DAOException
	 */
	Pair<Integer, List<SubjectDTO>> performCustomSearch(
			Map<String,String> criteria,
			Set<String> literalPredicates,
			PageRequest pageRequest,
			SortingRequest sortingRequest) throws DAOException;
	
	/**
	 * fetch sample triplets for given source.
	 * 
	 * @param url - source url
	 * @param limit - how many to fetch
	 * @return
	 * @throws DAOException
	 */
	Pair<Integer, List<RawTripleDTO>> getSampleTriples(String url, int limit) throws DAOException;
	
	/**
	 * 
	 * @param predicateUri
	 * @return
	 * @throws SearchException 
	 */
	boolean isAllowLiteralSearch(String predicateUri) throws SearchException;
}