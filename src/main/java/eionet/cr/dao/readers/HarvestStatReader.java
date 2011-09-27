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
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Juhan Voolaid
 */

package eionet.cr.dao.readers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import eionet.cr.dto.HarvestStatDTO;
import eionet.cr.util.sql.SQLResultSetBaseReader;

/**
 * Harvest statistics result set reader.
 *
 * @author Juhan Voolaid
 */
public class HarvestStatReader extends SQLResultSetBaseReader<HarvestStatDTO> {

    @Override
    public void readRow(ResultSet rs) throws SQLException, ResultSetReaderException {
        Integer harvestId = rs.getInt("harvest_id");

        HarvestStatDTO harvestStat = new HarvestStatDTO();
        resultList.add(harvestStat);

        harvestStat.setHarvestId(harvestId);
        harvestStat.setSourceUrl(rs.getString("url"));
        harvestStat.setTotalStatements(new Integer(rs.getInt("tot_statements")));

        Timestamp started = rs.getTimestamp("started");
        Timestamp finished = rs.getTimestamp("finished");
        harvestStat.setDatetimeStarted(started);
        if (started != null && finished != null) {
            long duration = finished.getTime() - started.getTime();
            harvestStat.setDuration(duration);
        }

        if (harvestStat.getTotalStatements() > 0) {
            Long statementDuration = harvestStat.getDuration() / harvestStat.getTotalStatements();
            harvestStat.setStatementDuration(statementDuration);
        }

    }

}