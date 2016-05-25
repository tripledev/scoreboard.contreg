package eionet.cr.migration;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.config.MigratableCR;
import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.service.DatasetMigrationsService;
import eionet.cr.service.ServiceException;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 * Thread that runs a given dataset migration.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationRunner extends Thread {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationRunner.class);

    /** */
    private static final int TTLP_MASK = 1 + 2 + 4 + 8 + 16 + 32 + 64 + 128;

    /** */
    private int migrationId;

    /** */
    private DatasetMigrationsService migrationsService;

    /** */
    private DatasetMigrationDTO migrationDTO;

    /** */
    private Connection sqlConn;

    /**
     * @param migrationId
     * @param migrationsService
     * @throws ServiceException
     */
    public DatasetMigrationRunner(int migrationId, DatasetMigrationsService migrationsService) throws ServiceException {

        super();

        this.migrationDTO = DatasetMigrationsService.newInstance().findById(migrationId);
        if (this.migrationDTO == null) {
            throw new IllegalArgumentException("Failed to find a dataset migration object by this id: " + migrationId);
        }
        this.migrationId = migrationId;

        this.migrationsService = migrationsService;
        if (this.migrationsService == null) {
            this.migrationsService = DatasetMigrationsService.newInstance();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        LOGGER.debug("STARTED dataset migration by this id: " + migrationId);

        Exception exception = null;
        try {
            sqlConn = SesameUtil.getSQLConnection();
            runInternal();
            LOGGER.debug("SUCCESS when running dataset migration by this id: " + migrationId);
        } catch (Exception e) {
            exception = e;
            LOGGER.error("FAILURE when running dataset migration by this id: " + migrationId, e);
        } finally {
            setMigrationFinished(exception);
            SQLUtil.close(sqlConn);
        }
    }

    /**
     *
     * @param exception
     */
    private void setMigrationFinished(Exception exception) {
        try {
            String messages = exception == null ? null : Util.getStackTrace(exception);
            migrationsService.setMigrationFinished(migrationId, new Date(), exception != null, messages);
        } catch (Exception e) {
            LOGGER.error("Failed to set migration finished", e);
        }
    }

    /**
     * @throws InterruptedException
     * @throws SQLException
     * @throws DatasetMigrationException
     * @throws IOException
     *
     */
    private void runInternal() throws InterruptedException, SQLException, DatasetMigrationException, IOException {

        File packageDir = getPackageDirectory();
        if (!packageDir.exists() || !packageDir.isDirectory()) {
            throw new DatasetMigrationException("Found no such package directory: " + packageDir);
        }

        String metadataGraphUri = migrationDTO.getTargetDatasetUri();
        String dataGraphUri = metadataGraphUri.replace("/dataset/", "/data/");

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.execute("log_enable(2,1)");

            // Import data.
            importData(dataGraphUri, packageDir, migrationDTO.isPrePurge());

            // Import metadata.
            importMetadata(metadataGraphUri, packageDir, migrationDTO.isPrePurge());
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @return
     * @throws DatasetMigrationException
     */
    private File getPackageDirectory() throws DatasetMigrationException {
        File packageDir;
        String sourceCrUrl = migrationDTO.getSourceCrUrl();
        MigratableCR migratableCR = migrationsService.getMigratableCRByUrl(sourceCrUrl);
        if (migratableCR == null) {
            throw new DatasetMigrationException("Found no CR configuration by this URL: " + sourceCrUrl);
        }

        String packagesDir = migratableCR.getMigrationPackagesDir();
        String packageIdentifier = migrationDTO.getSourcePackageIdentifier();
        packageDir = new File(packagesDir, packageIdentifier);
        return packageDir;
    }

    /**
     *
     * @param dataGraphUri
     * @param packageDir
     * @param prePurge
     * @throws SQLException
     * @throws IOException
     */
    private void importData(String dataGraphUri, File packageDir, boolean prePurge) throws SQLException, IOException {

        File[] dataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(DatasetMigrationPackageFiller.DATA_FILE_SUFFIX);
            }
        });

        if (dataFiles == null || dataFiles.length == 0) {
            LOGGER.warn("Found no data files in " + packageDir);
        }

        if (prePurge) {
            purgeGraphs(dataGraphUri);
        }

        importFiles(dataFiles, dataGraphUri);
    }

    /**
     *
     * @param metadataGraphUri
     * @param packageDir
     * @param prePurge
     * @throws SQLException
     * @throws IOException
     */
    private void importMetadata(String metadataGraphUri, File packageDir, boolean prePurge) throws SQLException, IOException {

        File[] metadataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(DatasetMigrationPackageFiller.METADATA_FILE_SUFFIX);
            }
        });

        if (metadataFiles == null || metadataFiles.length == 0) {
            LOGGER.warn("Found no metadata files in " + packageDir);
        }

        if (prePurge) {
            purgeGraphs(metadataGraphUri);
        }

        importFiles(metadataFiles, metadataGraphUri);
    }

    /**
     *
     * @param files
     * @param targetGraphUri
     * @throws SQLException
     * @throws IOException
     */
    private void importFiles(File[] files, String targetGraphUri) throws SQLException, IOException {

        if (StringUtils.isBlank(targetGraphUri)) {
            throw new IllegalArgumentException("Target graph URI must not be blank!");
        }

        // Gunzip all gzipped files.
        ArrayList<File> unzippedFiles = new ArrayList<File>();
        ArrayList<File> finalFiles = new ArrayList<File>();
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".gz")) {
                File gunzippedFile = gunzipFile(file);
                unzippedFiles.add(gunzippedFile);
                finalFiles.add(gunzippedFile);
            } else {
                finalFiles.add(file);
            }
        }

        PreparedStatement pstmt = null;
        try {
            for (File file : finalFiles) {

                LOGGER.debug(String.format("Importing [%s] into [%s]", file, targetGraphUri));

                // String sql = "DB.DBA.TTLP(file_to_string_output(?), '', ?, ?)";
                String sql = String.format("DB.DBA.TTLP(file_to_string_output('%s'), '', '%s', %d)", file.getAbsolutePath().replace('\\', '/'),
                        targetGraphUri, TTLP_MASK);

                LOGGER.debug("Executing SQL: " + sql);
                pstmt = sqlConn.prepareStatement(sql);



//                pstmt.setString(1, file.getAbsolutePath().replace('\\', '/'));
//                pstmt.setString(2, targetGraphUri);
//                pstmt.setInt(3, TTLP_MASK);
                pstmt.execute();
                LOGGER.debug("Closing pstmt...");
                SQLUtil.close(pstmt);
            }
        } finally {
            LOGGER.debug("Closing pstmt finally ...");
            SQLUtil.close(pstmt);
        }

        // Remove unzipped files.
        for (File unzippedFile : unzippedFiles) {
            LOGGER.debug("Quietly deleting " + unzippedFile);
            FileUtils.deleteQuietly(unzippedFile);
        }
    }

    /**
     *
     * @param graphUris
     * @throws SQLException
     */
    private void purgeGraphs(String... graphUris) throws SQLException {

        if (ArrayUtils.isEmpty(graphUris)) {
            return;
        }

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            for (String graphUri : graphUris) {
                LOGGER.debug("Purging graph: " + graphUri);
                stmt.execute("sparql clear graph <" + graphUri + ">");
            }
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param file
     * @return
     * @throws IOException
     */
    private File gunzipFile(File file) throws IOException {

        String fileName = file.getName();
        int suffixIndex = fileName.toLowerCase().indexOf(".gz");

        String outFileName = suffixIndex > 0 ? fileName.substring(0, suffixIndex) : fileName;
        File outFile = new File(file.getParentFile(), outFileName);

        LOGGER.debug("Unzipping " + file);

        GZIPInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new GZIPInputStream(new FileInputStream(file));
            outputStream = new FileOutputStream(outFile);
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        return outFile;
    }
}