/**
 *
 */
package eionet.cr.dao;

import java.util.List;

import eionet.cr.dto.DocumentationDTO;

/**
 * @author Risto Alt
 *
 */
public interface DocumentationDAO extends DAO {

    /**
     * @param pageId
     * @return DocumentationDTO
     * @throws DAOException
     */
    public DocumentationDTO getDocObject(String pageId) throws DAOException;

    /**
     * Return all object from documentation table where content_type is text/html
     * @return List<DocumentationDTO>
     * @throws DAOException
     */
    public List<DocumentationDTO> getHtmlDocObjects() throws DAOException;

    /**
     * @param pageId
     * @param contentType
     * @param fileName
     * @param title
     * @throws DAOException
     */
    public void insertFile(String pageId, String contentType, String fileName, String title) throws DAOException;

    /**
     * Checks if such page_id already exists in database
     * @param pageId
     * @return boolean
     * @throws DAOException
     */
    public boolean idExists(String pageId) throws DAOException;

}