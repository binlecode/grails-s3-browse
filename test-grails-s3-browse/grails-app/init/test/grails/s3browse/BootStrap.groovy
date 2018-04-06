package test.grails.s3browse

import groovy.util.logging.Slf4j

@Slf4j
class BootStrap {

    def init = { servletContext ->

        log.info "bootstrap running"

    }

    def destroy = {
    }
}
