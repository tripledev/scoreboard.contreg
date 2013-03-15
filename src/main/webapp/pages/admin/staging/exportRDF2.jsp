<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging databases">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        // Ensure a property select box title is updated with the hint of the currently selected property.
                        $("select[id$=propertySelect]").change(function() {
                            $(this).attr("title", $("option:selected",this).attr('title'));
                            return true;
                        });

                        <c:if test="${actionBean.testRun != null && actionBean.testRun.foundMissingConcepts}">
                        // Open the missing concepts popup
                        $("#openMissingConceptsPopup").click(function() {
                            $('#missingConceptsPopup').dialog('open');
                            return false;
                        });

                        // Setup the tables and columns popup
                        $('#missingConceptsPopup').dialog({
                            autoOpen: false,
                            height: 400,
                            width: 800,
                            maxHeight: 800,
                            maxWidth: 800
                        });
                        </c:if>
                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- The page's heading --%>

        <h1>RDF export: step 2</h1>

        <div style="margin-top:20px">
            Your query has been compiled on the database side, and the following selected columns have been detected.<br/>
            For each column, please specify a mapping to the corresponding RDF property.<br/>
            If none of the selected columns is mapped to the "Indicator (code)" property, please also select an indicator from picklist.<br/>
            It is also mandatory to select the dataset where the query's returned objects will go into.<br/>
            Defaults have been selected by the system where possible.<br/>
            Mandatory inputs are marked with <img src="http://www.eionet.europa.eu/styles/eionet2007/mandatory.gif"/>. Conditional inputs are marked with <img src="${pageContext.request.contextPath}/images/conditional.gif"/>.
        </div>

        <%-- The form --%>

        <div style="padding-top:20px">
            <crfn:form id="form1" beanclass="${actionBean.class.name}" method="post">
                <fieldset>
                    <legend style="font-weight:bold">The query:</legend>
                    <pre style="font-size:0.75em;max-height:130px;overflow:auto"><c:out value="${actionBean.queryConf.query}" /></pre>
                </fieldset>
                <fieldset style="margin-top:20px">

                    <legend style="font-weight:bold">The mapping of columns to RDF properties:</legend>
                    <table>
                        <c:forEach items="${actionBean.queryConf.columnMappings}" var="colMapping">
                            <tr>
                                <td style="text-align:right">
                                    <label for="${colMapping.key}.propertySelect" class="required"><c:out value="${colMapping.key}"/>:</label>
                                </td>
                                <td>
                                    <stripes:select name="${colMapping.key}.property" value="${colMapping.value.predicate}" title="${colMapping.value.hint}" id="${colMapping.key}.propertySelect">
                                        <stripes:option value="" label=""/>
                                        <c:forEach items="${actionBean.typeProperties}" var="typeProperty">
                                            <stripes:option value="${typeProperty.predicate}" label="${typeProperty.label}" title="${typeProperty.hint}"/>
                                        </c:forEach>
                                    </stripes:select>
                                </td>
                            </tr>
                        </c:forEach>
                    </table>

                </fieldset>
                <fieldset style="margin-top:20px">
                    <legend style="font-weight:bold">Other settings:</legend>
                    <table>
                        <tr>
                            <td style="text-align:right;vertical-align:top">
                                <label for="selIndicator" title="Indicator of the selected observations." style="padding-right: 12px;background: url(${pageContext.request.contextPath}/images/conditional.gif) center right no-repeat;">Indicator:</label>
                            </td>
                            <td>
                                <stripes:select id="selIndicator" name="queryConf.indicator" value="${actionBean.queryConf.indicator}">
                                    <stripes:option value="" label=""/>
                                    <c:forEach items="${actionBean.indicators}" var="indicatorPair">
                                        <stripes:option value="${indicatorPair.value}" label="${indicatorPair.value}"/>
                                    </c:forEach>
                                </stripes:select>&nbsp;<span style="font-size:0.8em">(must be selected, unless indicator has been mapped to one of the selected columns above)</span>
                            </td>
                        </tr>
                        <tr>
                            <td style="text-align:right">
                                <label for="selDataset" title="The dataset where the selected observations will go into." class="required">Dataset:</label>
                            </td>
                            <td>
                                <stripes:select id="selDataset" name="queryConf.dataset">
                                    <stripes:option value="http://semantic.digital-agenda-data.eu/dataset/scoreboard" label="Unit C4 - Economic and statistical analysis"/>
                                </stripes:select>
                            </td>
                        </tr>
                    </table>
                </fieldset>
                <div style="margin-top:20px">
                    <stripes:submit name="backToStep1" value="< Back"/>&nbsp;
                    <stripes:submit name="test" id="testButton" value="Test"/>&nbsp;
                    <stripes:submit name="run" id="runButton" value="Run"/>&nbsp;
                    <stripes:submit name="cancel" value="Cancel"/>
                </div>
            </crfn:form>

            <c:if test="${actionBean.context.eventName eq 'test' && actionBean.testRun != null && not empty actionBean.testRun.testResults}">

                <div style="width:100%;padding-top:20px">

                    <p>
                        <c:if test="${actionBean.testRun.rowCount > actionBean.testRun.maxTestResults}">
                            <strong>Test results (${actionBean.testRun.rowCount} found, displaying first ${actionBean.testRun.maxTestResults}):</strong>
                        </c:if>
                        <c:if test="${actionBean.testRun.rowCount <= actionBean.testRun.maxTestResults}">
                            <strong>Test results (${actionBean.testRun.rowCount} found):</strong>
                        </c:if>
                        <c:if test="${actionBean.testRun.foundMissingConcepts}">
                            <a href="#" id="openMissingConceptsPopup" class="important-msg" style="float:right">No metadata exists for these found concepts &#187;</a>
                        </c:if>
                    </p>

                    <display:table name="${actionBean.testRun.testResults}" id="testResultRow" class="datatable" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:100%;margin-top:20px">

                        <display:setProperty name="paging.banner.item_name" value="row"/>
                        <display:setProperty name="paging.banner.items_name" value="rows"/>
                        <display:setProperty name="paging.banner.all_items_found" value=""/>
                        <display:setProperty name="paging.banner.one_item_found" value=""/>
                        <display:setProperty name="paging.banner.onepage" value=""/>
                        <display:setProperty name="paging.banner.some_items_found" value='<span class="pagebanner">Rows {2} to {3}.</span>'/>

                        <c:forEach items="${testResultRow}" var="testResultRowEntry">
                            <display:column property="${testResultRowEntry.key}" title="${testResultRowEntry.key}"/>
                        </c:forEach>

                    </display:table>

                    <c:if test="${actionBean.testRun.foundMissingConcepts}">
                        <div id="missingConceptsPopup" title="Concepts with no metadta in the system yet">
                            <table class="datatable" style="width:100%">
                                <tr>
                                    <c:if test="${not empty actionBean.testRun.missingIndicators}">
                                        <th>Indicators</th>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingBreakdowns}">
                                        <th>Breakdowns</th>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingUnits}">
                                        <th>Units</th>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingRefAreas}">
                                        <th>Ref. areas</th>
                                    </c:if>
                                </tr>
                                <tr>
                                    <c:if test="${not empty actionBean.testRun.missingIndicators}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingIndicators}" var="missingIndicator">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingIndicator}"/></li>
                                                </ul>
                                             </c:forEach>
                                        </td>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingBreakdowns}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingBreakdowns}" var="missingBreakdown">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingBreakdown}"/></li>
                                                </ul>
                                            </c:forEach>
                                        </td>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingUnits}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingUnits}" var="missingUnit">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingUnit}"/></li>
                                                </ul>
                                            </c:forEach>
                                        </td>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingRefAreas}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingRefAreas}" var="missingRefArea">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingRefArea}"/></li>
                                                </ul>
                                            </c:forEach>
                                        </td>
                                    </c:if>
                                </tr>
                            </table>
                        </div>
                    </c:if>

                </div>

            </c:if>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
