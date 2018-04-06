<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 's3Object.label', default: 'S3Object')}" />
        <title><g:message code="default.list.label" args="[entityName]" /></title>
    </head>
    <body>
        <a href="#list-s3Object" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
        <div class="nav" role="navigation">
            <ul>
                <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
                <g:if test="${insertAllowed}">
                    <li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
                </g:if>
            </ul>
        </div>
        <div id="list-s3Object" class="content scaffold-list" role="main">
            <h1><g:message code="default.list.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
                <div class="message" role="status">${flash.message}</div>
            </g:if>

            <div>
                Marker Track:
                <g:each var="mkr" in="${markerTrack?.split(',')}">
                    <g:link style="text-decoration: none;" action="list" params="[marker: mkr, markerTrack: markerTrack, prefix: prefix, maxKeys: maxKeys]">
                        <span class="badge badge-info">${mkr}</span>
                    </g:link>
                </g:each>
            </div>

            <div>
                <form>
                    <input type="text" class="col-xs-2" id="prefix" name="prefix" placeholder="type path prefix here" value="${prefix}"/>
                    <span><g:actionSubmit class="btn-success" value="apply" action="list" onclick="clearMarker()"/></span>

                    <input type="hidden" id="markerTrack" name="markerTrack" value="${markerTrack}"/>
                    <input type="hidden" id="marker" name="marker" value="${marker}"/>
                    <span>Page Size:</span> &nbsp; <g:select name="maxKeys" value="${maxKeys}" from="[10, 20, 50]"/>
                    <span><g:actionSubmit class="btn-primary" value="prev" action="listPreviousPage"/></span>
                    <span><g:actionSubmit class="btn-primary" value="next" action="list"/></span>
                    <span><g:actionSubmit class="btn-primary" value="reset" action="listReset" onclick="clearMarker()"/></span>
                    <span><g:actionSubmit class="btn-info" id="btnCount" value="count" action="#" onclick="onCountClick();return false;">Count</g:actionSubmit></span>
                    &nbsp;&nbsp; <span id="objectCountSpan" class="badge badge-info"></span>
                </form>
            </div>
            <table class="table-bordered">
                <thead>
                <th>key</th>
                <th>size</th>
                <th>eTag</th>
                <th>last modified</th>
                <th>owner</th>
                <th>actions</th>
                </thead>
                <tbody>
                <g:each var="obj" in="${objectList}">
                    <tr>
                        <td><g:link action="show" params="[key: obj.key]">${obj.key}</g:link></td>
                        <td>${obj.size}</td>
                        <td>${obj.eTag}</td>
                        <td>${obj.lastModified}</td>
                        <td>${obj.owner}</td>
                        <td>
                            <g:link action="download" params="[key: obj.key]">Download</g:link>
                            <g:if test="${deleteAllowed}">
                                <g:form method="POST" style="display: inline">
                                    <input type="hidden" name="key" value="${obj.key}"/>
                                    <!-- use previous marker to list current page (not next page) for post-delete redirect -->
                                    <input type="hidden" name="marker" value="${previousMarker}"/>
                                    <input type="hidden" name="maxKeys" value="${maxKeys}"/>
                                    <input type="hidden" name="prefix" value="${prefix}"/>
                                    | <g:actionSubmit action="delete" value="Delete" class="btn-danger" onclick="return confirm('Are you sure to delete?')"/>
                                </g:form>
                            </g:if>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>

    <script>

        function clearMarker() {
            $("#marker").val('');
            $("#markerTrack").val('');
        }

        function onCountClick() {
            $('#objectCountSpan').text(''); // clear possibly pre-existing count
            $("#btnCount").css({opacity: 0.5}).val('counting ...');
            $.get('count', {prefix: $('#prefix').val()}, function(data) {
                $('#objectCountSpan').text(data);
                $("#btnCount").css({opacity: 1}).val('count');
            });
            return false;
        }

    </script>

    </body>
</html>