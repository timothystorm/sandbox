<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'asset.label', default: 'Asset')}" />
        <title><g:message code="default.show.label" args="[entityName]" /></title>
    </head>
    <body>
        <a href="#show-asset" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
        <div class="nav" role="navigation">
            <ul>
                <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
                <li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
                <li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
            </ul>
        </div>
        <div id="show-asset" class="content scaffold-show" role="main">
            <h1><g:message code="default.show.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
            <div class="message" role="status">${flash.message}</div>
            </g:if>
            <ol class="property-list asset">
                <li class="fieldcontain">
                    <span class="property-label">EAI</span>
                    <div class="property-value">${asset?.eai}</div>
                </li>
                <li class="fieldcontain">
                    <span class="property-label">Name</span>
                    <div class="property-value">${asset?.name}</div>
                </li>
                <li class="fieldcontain">
                    <span class="property-label">Description</span>
                    <div class="property-value">${asset?.desc}</div>
                </li>
                <li class="fieldcontain">
                    <span class="property-label">Assets</span>
                    <div class="property-value">
                    <g:each in="${asset?.assets}" var="a">
                        <div><g:link action="show" id="${a.id}">${+a.eai+' - '+a.name}</g:link></div>
                    </g:each>
                    </div>
                </li>
                <li class="fieldcontain">
                    <span class="property-label">Asset Of</span>
                    <div class="property-value">
                    <g:each in="${asset?.assetOf}" var="a">
                        <div><g:link action="show" id="${a.id}">${+a.eai+' - '+a.name}</g:link></div>
                    </g:each>
                    </div>
                </li>
            </ol>
            <g:form resource="${this.asset}" method="DELETE">
                <fieldset class="buttons">
                    <g:link class="edit" action="edit" resource="${this.asset}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
                    <input class="delete" type="submit" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
                </fieldset>
            </g:form>
        </div>
    </body>
</html>