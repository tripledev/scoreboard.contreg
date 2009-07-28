<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-definition>

	<c:choose>
		    <c:when test="${actionBean.subject!=null}">		    
		    	<c:set var="subjectUrl" value="${actionBean.subject.url}"/>
		    	<c:set var="subjectUri" value="${actionBean.subject.uri}"/>
		    	<div style="margin-top:20px">
		    		<c:choose>
		    			<c:when test="${subjectUrl!=null}">
		    				<p>Resource URL: <a href="${fn:escapeXml(subjectUrl)}"><c:out value="${subjectUrl}"/></a></p>
		    			</c:when>
		    			<c:when test="${subjectUri!=null}">
		    				<div class="advice-msg" title="${fn:escapeXml(subjectUri)}">This is an unresolvable resource!</div>
		    			</c:when>
		    			<c:otherwise>
		    				<div class="advice-msg">This is an anonymous resource!</div>
		    			</c:otherwise>
		    		</c:choose>
		    		<c:if test="${actionBean.subject.predicates!=null && fn:length(actionBean.subject.predicates)>0}">
				    	<table class="datatable" width="100%" cellspacing="0" summary="">
						<col style="width:25%;"/>
						<col style="max-width:3em;"/>
						<col/>
						<col style="width:4em;"/>
				    		<thead>
								<th scope="col" class="scope-col">Property</th>
								<th scope="col" class="scope-col">&nbsp;</th>
								<th scope="col" class="scope-col">Value</th> 
								<th scope="col" class="scope-col">Source</th>
							</thead>
				    		<tbody>
						    	<c:forEach var="predicate" items="${actionBean.subject.predicates}">
						    	
						    		<c:set var="predicateLabelDisplayed" value="${false}"/>
						    		
						    		<c:forEach items="${predicate.value}" var="object" varStatus="objectsStatus">
						    			<c:if test="${not crfn:subjectHasPredicateObject(actionBean.subject, actionBean.subProperties[predicate.key], object.value)}">
						    				<c:if test="${not crfn:isSourceToAny(object.valueHash, predicate.value)}">
									    		<tr>
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
									    			<td>
									    				<c:choose>
														<c:when test="${object.sourceObjectLong==0}">
															<c:out value="${object.value}"/>
														</c:when>
														<c:otherwise>
															<stripes:link class="infolink" href="/factsheet.action"><c:out value="${object.value}"/>
																	<stripes:param name="uriHash" value="${object.sourceObject}"/>
															</stripes:link>	
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
									    	</c:if>
								    	</c:if>
							    	</c:forEach>
						    	</c:forEach>
						    </tbody>
				    	</table>
				    	
				    	<p>
							<stripes:link href="/references.action" event="search">What is referring to this resource?
								<c:choose>
					    			<c:when test="${subjectUrl!=null}">
					    				<stripes:param name="object" value="${subjectUrl}"/>
					    			</c:when>
					    			<c:otherwise>
					    				<stripes:param name="object" value="${actionBean.subject.uriHash}"/>
					    			</c:otherwise>
					    		</c:choose>
							</stripes:link>
						</p>
							
				    </c:if>
			    </div>				    
		    </c:when>
		    <c:otherwise>
		    	<div style="margin-top:20px" class="note-msg"><strong>Unknown resource</strong>
				<p>Nothing is known about ${actionBean.uri}</p>
			</div>
			</c:otherwise>
		</c:choose>
		
</stripes:layout-definition>