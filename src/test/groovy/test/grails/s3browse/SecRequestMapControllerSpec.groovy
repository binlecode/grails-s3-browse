package test.grails.s3browse

import grails.test.mixin.*
import org.springframework.http.HttpMethod
import spock.lang.*

@TestFor(SecRequestMapController)
@Mock(SecRequestMap)
class SecRequestMapControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null

        // TODO: Populate valid properties like...
        // params["name"] = 'someValidName'
        // Provide a populateValidParams() implementation for this generated test suite
        params.configAttribute = 'TEST_ROLE'
        params.httpMethod = HttpMethod.GET
        params.url = '/test/url/**'
    }

    void "Test the index action returns the correct model"() {

        when:"The index action is executed"
            controller.index()

        then:"The model is correct"
            !model.secRequestMapList
            model.secRequestMapCount == 0
    }

    void "Test the create action returns the correct model"() {
        when:"The create action is executed"
            controller.create()

        then:"The model is correctly created"
            model.secRequestMap!= null
    }

    void "Test the save action correctly persists an instance"() {

        when:"The save action is executed with an invalid instance"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'POST'
            def secRequestMap = new SecRequestMap()
            secRequestMap.validate()
            controller.save(secRequestMap)

        then:"The create view is rendered again with the correct model"
            model.secRequestMap!= null
            view == 'create'

//        when:"The save action is executed with a valid instance"
//            response.reset()
//            populateValidParams(params)
//            secRequestMap = new SecRequestMap(params)
//
//            controller.save(secRequestMap)
//
//        then:"A redirect is issued to the show action"
//            response.redirectedUrl == '/secRequestMap/show/1'
//            controller.flash.message != null
//            SecRequestMap.count() == 1
    }

    void "Test that the show action returns the correct model"() {
        when:"The show action is executed with a null domain"
            controller.show(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the show action"
            populateValidParams(params)
            def secRequestMap = new SecRequestMap(params)
            controller.show(secRequestMap)

        then:"A model is populated containing the domain instance"
            model.secRequestMap == secRequestMap
    }

    void "Test that the edit action returns the correct model"() {
        when:"The edit action is executed with a null domain"
            controller.edit(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the edit action"
            populateValidParams(params)
            def secRequestMap = new SecRequestMap(params)
            controller.edit(secRequestMap)

        then:"A model is populated containing the domain instance"
            model.secRequestMap == secRequestMap
    }

    void "Test the update action performs an update on a valid domain instance"() {
        when:"Update is called for a domain instance that doesn't exist"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'PUT'
            controller.update(null)

        then:"A 404 error is returned"
            response.redirectedUrl == '/secRequestMap/index'
            flash.message != null

        when:"An invalid domain instance is passed to the update action"
            response.reset()
            def secRequestMap = new SecRequestMap()
            secRequestMap.validate()
            controller.update(secRequestMap)

        then:"The edit view is rendered again with the invalid instance"
            view == 'edit'
            model.secRequestMap == secRequestMap

        when:"A valid domain instance is passed to the update action"
            response.reset()
            populateValidParams(params)

            secRequestMap = GroovySpy(SecRequestMap)
            secRequestMap.save(_) >> secRequestMap
            secRequestMap.getId() >> 'TEST-ID'
            secRequestMap.hasErrors() >> false

            controller.update(secRequestMap)

        then:"A redirect is issued to the show action"
            secRequestMap != null
            response.status == 302  // a redirect
            response.redirectedUrl.split('/')[-1] == secRequestMap.id
            flash.message != null
    }

    void "Test that the delete action deletes an instance if it does not exist"() {
        when:"The delete action is called for a null instance"
            request.contentType = FORM_CONTENT_TYPE
            request.method = 'DELETE'
            controller.delete(null)

        then:"A 404 is returned"
            response.redirectedUrl == '/secRequestMap/index'
            flash.message != null
    }

    void "test delete instance"() {
        when:
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'DELETE'
        def secRequestMap = Stub(SecRequestMap)
        secRequestMap.delete(_) >> null

        controller.delete(secRequestMap)

        then:"The instance is deleted"
        response.redirectedUrl == '/secRequestMap/index'
        flash.message != null
    }

}
