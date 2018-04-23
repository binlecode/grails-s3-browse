<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <title>Show S3 Object Details</title>
    </head>
    <body>
        <a href="#show-s3Object" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
        <div class="nav" role="navigation">
            <ul>
                <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
                <li><g:link class="list" action="index">List S3 Objects</g:link></li>
            </ul>
        </div>
        <div id="show-s3Object" class="content scaffold-show" role="main">
            <g:if test="${flash.message}">
            <div class="message" role="status">${flash.message}</div>
            </g:if>
            <h1>S3 Object: ${object.key}</h1>
            <ul>
                <li>bucketName: ${object.bucketName}</li>
                <li>key: ${object.key}</li>
                <li>redirecLocation: ${object.redirecLocation}</li>
                <g:if test="${object.metadata}">
                    <li>metadata contentDisposition: ${object.metadata.contentDisposition}</li>
                    <li>metadata contentEncoding: ${object.metadata.contentEncoding}</li>
                    <li>metadata contentLength: ${object.metadata.contentLength}</li>
                    <li>metadata contentType: ${object.metadata.contentType}</li>
                    <li>metadata contentMD5: ${object.metadata.contentMD5}</li>
                </g:if>
            </ul>
        </div>
    </body>
</html>
