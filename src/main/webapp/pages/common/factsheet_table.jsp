<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-definition>

<c:if test="${actionBean.subject.predicates!=null && fn:length(actionBean.subject.predicates)>0}">
	
	<c:set var="isRaw" value="${param.raw!=null}"/>
	
   	<table class="datatable" width="100%" cellspacing="0" summary="">
   	<c:if test="${displayCheckboxes}">
   		<col/>
   	</c:if>
	<col style="width:25%;"/>
	<col style="width:2em; max-width:3em;"/>
	<col/>
	<col style="width:4em;"/>
   		<thead>
   			<c:if test="${displayCheckboxes}">
   				<th scope="col" class="scope-col">&nbsp;</th>
   			</c:if>
			<th scope="col" class="scope-col">Property</th>
			<th scope="col" class="scope-col">&nbsp;</th>
			<th scope="col" class="scope-col">Value</th> 
			<th scope="col" class="scope-col">Source</th>
		</thead>
   		<tbody>
	    	<c:forEach var="predicate" items="${actionBean.subject.predicates}">
	    	
	    		<c:set var="predicateLabelDisplayed" value="${false}"/>
	    		
	    		<c:forEach items="${predicate.value}" var="object" varStatus="objectsStatus">
	    		
	    			<c:if test="${isRaw || not crfn:subjectHasPredicateObject(actionBean.subject, actionBean.subProperties[predicate.key], object.value)}">
	    			
	    				<c:if test="${isRaw || not crfn:isSourceToAny(object.hash, predicate.value)}">
	    				
    						<c:set var="thisDisplayedRow" value="${crfn:spoHash(predicate.key)}_${object.language}_${object.value}"/>
    						
    						<c:if test="${isRaw || (empty previousDisplayedRow || previousDisplayedRow!=thisDisplayedRow)}">
    						
					    		<tr>
									<c:if test="${displayCheckboxes}">
										<c:choose>
											<c:when test="${sessionScope.crUser.registrationsUri==object.sourceSmart}">
												<th>
													<input type="checkbox" name="rowId" value="${crfn:spoHash(predicate.key)}_${object.id}"/>
												</th>
											</c:when>
											<c:otherwise><th>&nbsp;</th></c:otherwise>
										</c:choose>
									</c:if>
					    			<th scope="row" class="scope-row" title="${predicate.key}" style="white-space:nowrap">
					    				<c:choose>
					    					<c:when test="${not predicateLabelDisplayed}">
					    						<c:out value="${crfn:getPredicateLabel(actionBean.predicateLabels, predicate.key)}"/>
					    						<c:set var="predicateLabelDisplayed" value="${true}"/>
													<c:if test='${sessionScope.crUser!=null && crfn:hasPermission(sessionScope.crUser.userName, "/", "u")}'>
														<stripes:link  href="/factsheet.action">
															<stripes:param name="uri" value="${predicate.key}"/>
															<img src="${pageContext.request.contextPath}/images/view2.gif" alt="Definition"/>
														</stripes:link>	
													</c:if>
					    					</c:when>
					    					<c:otherwise>&nbsp;</c:otherwise>
					    				</c:choose>
					    			</th>
					    			<td>
					    				<c:choose>
					    					<c:when test="${not empty object.language}">
					    						<span class="langcode"><c:out value="${object.language}"/></span>
					    					</c:when>
					    					<c:otherwise>&nbsp;</c:otherwise>
					    				</c:choose>
					    			</td>
					    			<td title="${crfn:rawModeTitle(object, predicate.value)}">
					    				<c:choose>
					    					<c:when test="${!object.literal}">		    								
		    									<c:choose>
		    										<c:when test="${!object.anonymous}">
		    											<stripes:link class="infolink" href="/factsheet.action"><c:out value="${object.value}"/>
		    												<stripes:param name="uri" value="${object.value}"/>
		    											</stripes:link>
		    										</c:when>
		    										<c:otherwise>
		    											<stripes:link class="infolink" href="/factsheet.action">Anonymous resource
		    												<stripes:param name="uriHash" value="${object.hash}"/>
		    											</stripes:link>
		    										</c:otherwise>
		    									</c:choose>						
					    					</c:when>
					    					<c:when test="${object.literal && object.sourceObjectHash!=0}">
					    						<stripes:link class="infolink" href="/factsheet.action"><c:out value="${object.value}"/>
	   												<stripes:param name="uriHash" value="${object.sourceObjectHash}"/>
		    									</stripes:link>
					    					</c:when>
											<c:otherwise>
												<c:out value="${object.value}"/>
											</c:otherwise>
										</c:choose>
					    			</td>
					    			<td class="center">
					    				<c:choose>
						    				<c:when test="${object.sourceSmart!=null}">
									    		<stripes:link href="/factsheet.action">
									    			<img src="${pageContext.request.contextPath}/images/harvest_source.png" title="${fn:escapeXml(object.sourceSmart)}" alt="${fn:escapeXml(object.sourceSmart)}"/>
									    			<stripes:param name="uri" value="${object.sourceSmart}"/>
												</stripes:link>
											</c:when>
											<c:otherwise>&nbsp;</c:otherwise>
										</c:choose>
									</td>
					    		</tr>							    		
					    		<c:set var="previousDisplayedRow" value="${thisDisplayedRow}"/>
					    	</c:if>
				    	</c:if>
			    	</c:if>
				    
		    	</c:forEach>
	    	</c:forEach>
	    </tbody>
   	</table>
   	
</c:if>
</stripes:layout-definition>
