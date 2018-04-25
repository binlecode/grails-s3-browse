package grails.plugin.s3browse

import grails.test.mixin.integration.Integration
import grails.transaction.*

import spock.lang.*
import geb.spock.*

/**
 * See http://www.gebish.org/manual/current/ for more instructions
 */
@Integration
@Rollback
class S3BrowseServiceIntegrationSpec extends GebSpec {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        when:"The home page is visited"
            go '/'

        then:"The title is correct"
            //todo: add some integration tests in additional to those unit tests
        	true
    }
}
