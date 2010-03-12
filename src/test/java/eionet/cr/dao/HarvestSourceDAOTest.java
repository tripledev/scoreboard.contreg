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
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.dao;

import java.util.Collections;
import java.util.List;

import org.dbunit.DBTestCase;
import org.dbunit.DatabaseTestCase;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Test;

import eionet.cr.config.GeneralConfig;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.test.helpers.CRDatabaseTestCase;
import eionet.cr.test.helpers.DbHelper;
import eionet.cr.test.helpers.dbunit.DbUnitDatabaseConnection;
import eionet.cr.util.Pair;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sql.DbConnectionProvider;

/**
 * JUnit test tests HarvestSourceDAO functionality.
 * 
 * @author altnyris
 *
 */
public class HarvestSourceDAOTest extends CRDatabaseTestCase {

	/*
	 * (non-Javadoc)
	 * @see org.dbunit.DatabaseTestCase#getDataSet()
	 */
	protected IDataSet getDataSet() throws Exception {
		return getXmlDataSet("sources-harvests-messages.xml");
	}
	
	@Test
	public void testAddSource() throws Exception {
		
		HarvestSourceDTO harvestSource = new HarvestSourceDTO();
		harvestSource = new HarvestSourceDTO();
		harvestSource.setUrl("http://rod.eionet.europa.eu/testObligations");
		harvestSource.setEmails("bob@europe.eu");
		harvestSource.setIntervalMinutes(new Integer(0));
		
		HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
		Integer harvestSourceID = dao.addSource(harvestSource, "bobsmith");
		assertNotNull(harvestSourceID);
		
		harvestSource = dao.getHarvestSourceById(harvestSourceID);
		assertEquals("bob@europe.eu", harvestSource.getEmails());
		assertEquals("http://rod.eionet.europa.eu/testObligations", harvestSource.getUrl());
		assertEquals("bob@europe.eu", harvestSource.getEmails());
	}
	
	@Test
	public void testGetHarvestSourceByUrl() throws Exception{
		
		HarvestSourceDTO dto =
			DAOFactory.get().getDao(HarvestSourceDAO.class).getHarvestSourceByUrl(
					"http://www.eionet.europa.eu/seris/rdf");
		assertNotNull(dto);
	}
	
	@Test
	public void testGetHarvestSources() throws Exception {
		
		
		Pair<Integer,List<HarvestSourceDTO>> result = DAOFactory.get().getDao(HarvestSourceDAO.class).getHarvestSources("", PagingRequest.create(1,100), null);
		assertNotNull(result);
		assertNotNull(result.getRight());
		assertEquals(42, result.getRight().size());
	}
	
	@Test
	public void testEditSource() throws Exception {
		
		// get the source by URL
		HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
		HarvestSourceDTO harvestSource = dao.getHarvestSourceByUrl(
				"http://www.eionet.europa.eu/seris/rdf");
		assertNotNull(harvestSource);
		
		// change the URL of the source
		harvestSource.setUrl("http://www.eionet.europa.eu/seris/rdf-dummy");
		dao.editSource(harvestSource);
		
		// get the source by previous URL again- now it must be null
		assertNull(dao.getHarvestSourceByUrl("http://www.eionet.europa.eu/seris/rdf"));
		
		// get the source by new URL, it must not be null
		assertNotNull(dao.getHarvestSourceByUrl("http://www.eionet.europa.eu/seris/rdf-dummy"));
	}
	
	@Test
	public void testDeleteSource() throws Exception {

		// get the source's ID
		HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
		HarvestSourceDTO dto = dao.getHarvestSourceByUrl("http://localhost:8080/cr/pages/test.xml");
		assertNotNull(dto);
		assertNotNull(dto.getSourceId());

		// delete the source
		dao.deleteSourceByUrl("http://localhost:8080/cr/pages/test.xml");
		
		// now get the source by ID- it must be null now
		dto = dao.getHarvestSourceById(dto.getSourceId());
		assertNull(dto);
	}
}
