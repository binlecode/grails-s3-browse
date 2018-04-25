package test.grails.s3browse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class AccessAuditInterceptor {
    int order = HIGHEST_PRECEDENCE + 50 // make sure this is the first filter to hit

    public AccessAuditInterceptor() {
        match controller: '*', action: '*'
    }

    boolean before() {
        log.trace "HTTP request - method: ${request.method}, controller: ${controllerName}, action: ${actionName}, params: ${params}"
        true   // pass over to target controller
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
