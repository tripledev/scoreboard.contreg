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
package eionet.cr.web.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.SpoBinaryDAO;
import eionet.cr.dao.util.UriLabelPair;
import eionet.cr.dao.virtuoso.PredicateObjectsReader;
import eionet.cr.dataset.CurrentLoadedDatasets;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.dto.TripleDTO;
import eionet.cr.harvest.CurrentHarvests;
import eionet.cr.harvest.HarvestException;
import eionet.cr.harvest.OnDemandHarvester;
import eionet.cr.harvest.scheduled.UrgentHarvestQueue;
import eionet.cr.util.Pair;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
import eionet.cr.web.util.ApplicationCache;
import eionet.cr.web.util.tabs.FactsheetTabMenuHelper;
import eionet.cr.web.util.tabs.TabElement;

/**
 * Factsheet.
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
@UrlBinding("/factsheet.action")
public class FactsheetActionBean extends AbstractActionBean {

    /**  */
    public static final String PAGE_PARAM_PREFIX = "page";

    /** */
    private static final String ADDIBLE_PROPERTIES_SESSION_ATTR = FactsheetActionBean.class.getName() + ".addibleProperties";

    /** URI by which the factsheet has been requested. */
    private String uri;

    /** URI hash by which the factsheet has been requested. Ignored when factsheet requested by URI. */
    private long uriHash;

    /** The subject data object found by the requestd URI or URI hash. */
    private SubjectDTO subject;

    /** Used in factsheet edit mode only, where it indicates if the subject is anonymous. */
    private boolean anonymous;

    /** */
    private String propertyUri;
    /** */
    private String propertyValue;

    /** List of identifiers of property-value rows submitted from factsheet edit form. */
    private List<String> rowId;

    /** True if the session bears a user and it happens to be an administrator. Otherwise false. */
    private boolean adminLoggedIn;

    /** True if the found subject is a bookmark of the logged-in user. In all other cases false. */
    private Boolean subjectIsUserBookmark;

    /** True if the found subject has downloadable content in filestore. */
    private Boolean subjectDownloadable;

    /** True, if URI is harvest source. */
    private boolean uriIsHarvestSource;

    /** */
    private String bookmarkLabel;

    /** */
    private Map<String, Integer> predicatePageNumbers;
    private Map<String, Integer> predicatePageCounts;

    /** */
    private List<TabElement> tabs;

    /** */
    private Boolean subjectIsType = null;

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails
     */
    @DefaultHandler
    public Resolution view() throws DAOException {

        if (isNoCriteria()) {
            addCautionMessage("No request criteria specified!");
        } else {
            HelperDAO helperDAO = DAOFactory.get().getDao(HelperDAO.class);

            setAdminLoggedIn(getUser() != null && getUser().isAdministrator());

            subject = helperDAO.getFactsheet(uri, null, getPredicatePageNumbers());

            FactsheetTabMenuHelper helper = new FactsheetTabMenuHelper(uri, subject, factory.getDao(HarvestSourceDAO.class));

            tabs = helper.getTabs(FactsheetTabMenuHelper.TabTitle.RESOURCE_PROPERTIES);
            uriIsHarvestSource = helper.isUriIsHarvestSource();
        }

        return new ForwardResolution("/pages/factsheet.jsp");
    }

    /**
     * Handle for ajax harvesting.
     *
     * @return Resolution
     */
    public Resolution harvestAjax() {
        String message;
        try {
            message = harvestNow().getRight();
        } catch (Exception ignored) {
            logger.error("error while scheduling ajax harvest", ignored);
            message = "Error occured, more info can be obtained in application logs";
        }
        return new StreamingResolution("text/html", message);
    }

    /**
     * Schedules a harvest for resource.
     *
     * @return view resolution
     * @throws HarvestException
     *             if harvesting fails
     * @throws DAOException
     *             if query fails
     */
    public Resolution harvest() throws HarvestException, DAOException {

        Pair<Boolean, String> message = harvestNow();
        if (message.getLeft()) {
            addWarningMessage(message.getRight());
        } else {
            addSystemMessage(message.getRight());
        }

        return new RedirectResolution(this.getClass(), "view").addParameter("uri", uri);
    }

    /**
     * helper method to eliminate code duplication.
     *
     * @return Pair<Boolean, String> feedback messages
     * @throws HarvestException
     *             if harvesting fails
     * @throws DAOException
     *             if query fails
     */
    private Pair<Boolean, String> harvestNow() throws HarvestException, DAOException {

        String message = null;
        if (isUserLoggedIn()) {
            if (!StringUtils.isBlank(uri) && URLUtil.isURL(uri)) {

                /* add this url into HARVEST_SOURCE table */

                HarvestSourceDAO dao = factory.getDao(HarvestSourceDAO.class);
                HarvestSourceDTO dto = new HarvestSourceDTO();
                dto.setUrl(StringUtils.substringBefore(uri, "#"));
                dto.setEmails("");
                dto.setIntervalMinutes(Integer.valueOf(GeneralConfig.getProperty(GeneralConfig.HARVESTER_REFERRALS_INTERVAL,
                        String.valueOf(HarvestSourceDTO.DEFAULT_REFERRALS_INTERVAL))));
                dto.setPrioritySource(false);
                dto.setOwner(null);
                dao.addSourceIgnoreDuplicate(dto);

                /* issue an instant harvest of this url */

                OnDemandHarvester.Resolution resolution = OnDemandHarvester.harvest(dto.getUrl(), getUserName());

                /* give feedback to the user */

                if (resolution.equals(OnDemandHarvester.Resolution.ALREADY_HARVESTING))
                    message = "The resource is currently being harvested by another user or background harvester!";
                else if (resolution.equals(OnDemandHarvester.Resolution.UNCOMPLETE))
                    message = "The harvest hasn't finished yet, but continues in the background!";
                else if (resolution.equals(OnDemandHarvester.Resolution.COMPLETE))
                    message = "The harvest has been completed!";
                else if (resolution.equals(OnDemandHarvester.Resolution.SOURCE_UNAVAILABLE))
                    message = "The resource was not available!";
                else if (resolution.equals(OnDemandHarvester.Resolution.NO_STRUCTURED_DATA))
                    message = "The resource contained no RDF data!";
                // else if (resolution.equals(InstantHarvester.Resolution.RECENTLY_HARVESTED))
                // message = "Source redirects to another source that has recently been harvested! Will not harvest.";
                else
                    message = "No feedback given from harvest!";
            }
            return new Pair<Boolean, String>(false, message);
        } else {
            return new Pair<Boolean, String>(true, getBundle().getString("not.logged.in"));
        }
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails if query fails
     */
    public Resolution edit() throws DAOException {

        return view();
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails if query fails
     */
    public Resolution addbookmark() throws DAOException {
        if (isUserLoggedIn()) {
            DAOFactory.get().getDao(HelperDAO.class).addUserBookmark(getUser(), getUrl(), bookmarkLabel);
            addSystemMessage("Succesfully bookmarked this source.");
        } else {
            addSystemMessage("Only logged in users can bookmark sources.");
        }
        return view();
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails
     */
    public Resolution removebookmark() throws DAOException {
        if (isUserLoggedIn()) {
            DAOFactory.get().getDao(HelperDAO.class).deleteUserBookmark(getUser(), getUrl());
            addSystemMessage("Succesfully removed this source from bookmarks.");
        } else {
            addSystemMessage("Only logged in users can remove bookmarks.");
        }
        return view();
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails if query fails
     */
    public Resolution save() throws DAOException {

        SubjectDTO subjectDTO = new SubjectDTO(uri, anonymous);

        if (propertyUri.equals(Predicates.CR_TAG)) {
            List<String> tags = Util.splitStringBySpacesExpectBetweenQuotes(propertyValue);

            for (String tag : tags) {
                ObjectDTO objectDTO = new ObjectDTO(tag, true);
                objectDTO.setSourceUri(getUser().getRegistrationsUri());
                subjectDTO.addObject(propertyUri, objectDTO);
            }
        } else {
            // other properties
            ObjectDTO objectDTO = new ObjectDTO(propertyValue, true);
            objectDTO.setSourceUri(getUser().getRegistrationsUri());
            subjectDTO.addObject(propertyUri, objectDTO);
        }

        HelperDAO helperDao = factory.getDao(HelperDAO.class);
        helperDao.addTriples(subjectDTO);
        helperDao.updateUserHistory(getUser(), uri);

        // since user registrations URI was used as triple source, add it to HARVEST_SOURCE too
        // (but set interval minutes to 0, to avoid it being background-harvested)
        DAOFactory
        .get()
        .getDao(HarvestSourceDAO.class)
        .addSourceIgnoreDuplicate(
                HarvestSourceDTO.create(getUser().getRegistrationsUri(), true, 0, getUser().getUserName()));

        return new RedirectResolution(this.getClass(), "edit").addParameter("uri", uri);
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails
     */
    public Resolution delete() throws DAOException {

        if (rowId != null && !rowId.isEmpty()) {

            ArrayList<TripleDTO> triples = new ArrayList<TripleDTO>();

            for (String row : rowId) {
                int i = row.indexOf("_");
                if (i <= 0 || i == (row.length() - 1)) {
                    throw new IllegalArgumentException("Illegal rowId: " + row);
                }

                String predicateHash = row.substring(0, i);
                String predicate = getContext().getRequestParameter("pred_".concat(predicateHash));

                String objectHash = row.substring(i + 1);
                String objectValue = getContext().getRequest().getParameter("obj_".concat(objectHash));
                String sourceUri = getContext().getRequest().getParameter("source_".concat(objectHash));

                TripleDTO triple = new TripleDTO(uri, predicate, objectValue);
                // FIXME - find a better way to determine if the object is literal or not, URIs may be literals also
                triple.setLiteralObject(!URLUtil.isURL(objectValue));
                triple.setSourceUri(sourceUri);

                triples.add(triple);
            }

            HelperDAO helperDao = factory.getDao(HelperDAO.class);
            helperDao.deleteTriples(triples);
            helperDao.updateUserHistory(getUser(), uri);
        }

        return new RedirectResolution(this.getClass(), "edit").addParameter("uri", uri);
    }

    /**
     * Validates if user is logged on and if event property is not empty.
     */
    @ValidationMethod(on = {"save", "delete", "edit", "harvest"})
    public void validateUserKnown() {

        if (getUser() == null) {
            addWarningMessage("Operation not allowed for anonymous users");
        } else if (getContext().getEventName().equals("save") && StringUtils.isBlank(propertyValue)) {
            addGlobalValidationError(new SimpleError("Property value must not be blank"));
        }
    }

    /**
     * @return the resourceUri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param resourceUri
     *            the resourceUri to set
     */
    public void setUri(final String resourceUri) {
        this.uri = resourceUri;
    }

    /**
     * @return the resource
     */
    public SubjectDTO getSubject() {
        return subject;
    }

    /**
     * @return the addibleProperties
     * @throws DAOException
     *             if query fails
     */
    public Collection<UriLabelPair> getAddibleProperties() throws DAOException {

        // get the addible properties from session

        HttpSession session = getContext().getRequest().getSession();
        @SuppressWarnings("unchecked")
        ArrayList<UriLabelPair> result = (ArrayList<UriLabelPair>) session.getAttribute(ADDIBLE_PROPERTIES_SESSION_ATTR);

        // if not in session, create them and add to session
        if (result == null || result.isEmpty()) {

            // get addible properties from database

            HelperDAO helperDAO = factory.getDao(HelperDAO.class);
            HashMap<String, String> props = helperDAO.getAddibleProperties(uri);

            // add some hard-coded properties, HashMap assures there won't be duplicates
            props.put(Predicates.RDFS_LABEL, "Title");
            props.put(Predicates.CR_TAG, "Tag");
            props.put(Predicates.RDFS_COMMENT, "Other comments"); // Don't use
            props.put(Predicates.DC_DESCRIPTION, "Description");
            props.put(Predicates.CR_HAS_SOURCE, "hasSource");
            props.put(Predicates.ROD_PRODUCT_OF, "productOf");

            // create the result object from the found and hard-coded properties, sort it

            result = new ArrayList<UriLabelPair>();
            if (props != null && !props.isEmpty()) {

                for (String uri : props.keySet()) {
                    result.add(UriLabelPair.create(uri, props.get(uri)));
                }
                Collections.sort(result);
            }

            // put into session
            session.setAttribute(ADDIBLE_PROPERTIES_SESSION_ATTR, result);
        }

        return result;
    }

    /**
     * @param anonymous
     *            the anonymous to set
     */
    public void setAnonymous(final boolean anonymous) {
        this.anonymous = anonymous;
    }

    /**
     * @param subject
     *            the subject to set
     */
    public void setSubject(final SubjectDTO subject) {
        this.subject = subject;
    }

    /**
     * @param propertyUri
     *            the propertyUri to set
     */
    public void setPropertyUri(final String propertyUri) {
        this.propertyUri = propertyUri;
    }

    /**
     * @param propertyValue
     *            the propertyValue to set
     */
    public void setPropertyValue(final String propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * @param rowId
     *            the rowId to set
     */
    public void setRowId(final List<String> rowId) {
        this.rowId = rowId;
    }

    /**
     * @return the noCriteria
     */
    public boolean isNoCriteria() {
        return StringUtils.isBlank(uri);
    }

    /**
     * @return the uriHash
     */
    public long getUriHash() {
        return uriHash;
    }

    /**
     * @param uriHash
     *            the uriHash to set
     */
    public void setUriHash(final long uriHash) {
        this.uriHash = uriHash;
    }

    /**
     *
     * @return String
     */
    public String getUrl() {
        return uri != null && URLUtil.isURL(uri) ? uri : null;
    }

    /**
     * True if admin is logged in.
     *
     * @return boolean
     */
    public boolean isAdminLoggedIn() {
        return adminLoggedIn;
    }

    /**
     * Setter of admin logged in property.
     *
     * @param adminLoggedIn
     *            boolean
     */
    public void setAdminLoggedIn(final boolean adminLoggedIn) {
        this.adminLoggedIn = adminLoggedIn;
    }

    /**
     *
     * @return boolean
     * @throws DAOException
     *             if query fails if query fails
     */
    public boolean getSubjectIsUserBookmark() throws DAOException {

        if (!isUserLoggedIn()) {
            return false;
        }

        if (subjectIsUserBookmark == null) {
            subjectIsUserBookmark = Boolean.valueOf(factory.getDao(HelperDAO.class).isSubjectUserBookmark(getUser(), uri));
        }

        return subjectIsUserBookmark.booleanValue();
    }

    /**
     * @return the subjectDownloadable
     * @throws DAOException
     */
    public boolean isSubjectDownloadable() throws DAOException {

        if (subjectDownloadable == null) {
            subjectDownloadable = Boolean.valueOf(DAOFactory.get().getDao(SpoBinaryDAO.class).exists(uri));
        }
        return subjectDownloadable.booleanValue();
    }

    /**
     *
     * @return boolean
     */
    public boolean isCurrentlyHarvested() {

        return uri == null ? false
                : (CurrentHarvests.contains(uri) || UrgentHarvestQueue.isInQueue(uri) || CurrentLoadedDatasets.contains(uri));
    }

    /**
     *
     * @return boolean
     */
    public boolean isCompiledDataset() {

        boolean ret = false;

        if (subject.getObject(Predicates.RDF_TYPE) != null) {
            ret = Subjects.CR_COMPILED_DATASET.equals(subject.getObject(Predicates.RDF_TYPE).getValue());
        }

        return ret;
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     */
    public Resolution showOnMap() throws DAOException {
        HelperDAO helperDAO = DAOFactory.get().getDao(HelperDAO.class);
        subject = helperDAO.getFactsheet(uri, null, null);

        FactsheetTabMenuHelper helper = new FactsheetTabMenuHelper(uri, subject, factory.getDao(HarvestSourceDAO.class));
        tabs = helper.getTabs(FactsheetTabMenuHelper.TabTitle.SHOW_ON_MAP);
        return new ForwardResolution("/pages/map.jsp");
    }

    public boolean isUriIsHarvestSource() {
        return uriIsHarvestSource;
    }

    /**
     *
     * @return
     */
    public String getBookmarkLabel() {
        return bookmarkLabel;
    }

    /**
     *
     * @param bookmarkLabel
     */
    public void setBookmarkLabel(String bookmarkLabel) {
        this.bookmarkLabel = bookmarkLabel;
    }

    /**
     * @return the predicatePages
     */
    public Map<String, Integer> getPredicatePageNumbers() {

        if (predicatePageNumbers == null) {

            predicatePageNumbers = new HashMap<String, Integer>();
            HttpServletRequest request = getContext().getRequest();
            Map<String, String[]> paramsMap = request.getParameterMap();

            if (paramsMap != null && !paramsMap.isEmpty()) {

                for (Map.Entry<String, String[]> entry : paramsMap.entrySet()) {

                    String paramName = entry.getKey();
                    if (isPredicatePageParam(paramName)) {

                        int pageNumber = NumberUtils.toInt(paramName.substring(PAGE_PARAM_PREFIX.length()));
                        if (pageNumber > 0) {

                            String[] predicateUris = entry.getValue();
                            if (predicateUris != null) {
                                for (String predicateUri : predicateUris) {
                                    predicatePageNumbers.put(predicateUri, pageNumber);
                                }
                            }
                        }
                    }
                }
            }
        }

        return predicatePageNumbers;
    }

    /**
     *
     * @param paramName
     * @return
     */
    public boolean isPredicatePageParam(String paramName) {

        if (paramName.startsWith(PAGE_PARAM_PREFIX) && paramName.length() > PAGE_PARAM_PREFIX.length()) {
            return StringUtils.isNumeric(paramName.substring(PAGE_PARAM_PREFIX.length()));
        } else {
            return false;
        }
    }

    /**
     *
     * @return
     */
    public int getPredicatePageSize() {

        return PredicateObjectsReader.PREDICATE_PAGE_SIZE;
    }

    /**
     *
     * @return
     */
    public List<TabElement> getTabs() {
        return tabs;
    }

    /**
     *
     * @return
     */
    public boolean getSubjectIsType(){

        if (subjectIsType==null){

            List<String> typeUris = ApplicationCache.getTypeUris();
            subjectIsType = Boolean.valueOf(typeUris.contains(this.uri));
        }

        return subjectIsType;
    }
}
