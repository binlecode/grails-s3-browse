<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="main" />
        <title>Upload S3 Object</title>
    </head>
    <body>
        <a href="#create-s3File" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
        <div class="nav" role="navigation">
            <ul>
                <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
                <li><g:link class="list" action="index">List S3 Objects</g:link></li>
            </ul>
        </div>
        <div id="create-s3File" class="content scaffold-create" role="main">
            <h1>Upload file to S3</h1>
            <g:if test="${flash.message}">
                <div class="message" role="status">${flash.message}</div>
            </g:if>

            <g:uploadForm action="upload" controller="s3Browse">
                <fieldset class="form">
                    <input type="file" name="s3File" />
                    <input type="text" name="prefix" placeholder="S3 key prefix (optional)" />
                </fieldset>
                <fieldset class="buttons">
                    <g:submitButton name="upload" class="save" value="Upload" />
                </fieldset>
            </g:uploadForm>
        </div>
    </body>
</html>
