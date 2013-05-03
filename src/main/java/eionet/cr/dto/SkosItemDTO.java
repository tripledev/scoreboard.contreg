package eionet.cr.dto;

import org.apache.commons.lang.StringUtils;

/**
 * A generic DTO for representing resources that can have a skos:notation, skos:prefLabel, etc.
 * This could be an object whose rdf:type is skos:Concept or skos:ConceptScheme, for exmaple.
 *
 * @author jaanus
 */
public class SkosItemDTO {

    /** */
    private String uri;
    private String skosNotation;
    private String skosPrefLabel;

    /**
     * @param uri
     */
    public SkosItemDTO(String uri) {

        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("The URI must not be blank!");
        }
        this.uri = uri;
    }

    /**
     * @return the skosNotation
     */
    public String getSkosNotation() {
        return skosNotation;
    }

    /**
     * @param skosNotation the skosNotation to set
     */
    public void setSkosNotation(String skosNotation) {
        this.skosNotation = skosNotation;
    }

    /**
     * @return the skosPrefLabel
     */
    public String getSkosPrefLabel() {
        return skosPrefLabel;
    }

    /**
     * @param skosPrefLabel the skosPrefLabel to set
     */
    public void setSkosPrefLabel(String skosPrefLabel) {
        this.skosPrefLabel = skosPrefLabel;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }
}