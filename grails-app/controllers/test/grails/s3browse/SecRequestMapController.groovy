package test.grails.s3browse

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class SecRequestMapController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond SecRequestMap.list(params), model:[secRequestMapCount: SecRequestMap.count()]
    }

    def show(SecRequestMap secRequestMap) {
        respond secRequestMap
    }

    def create() {
        respond new SecRequestMap(params)
    }

    @Transactional
    def save(SecRequestMap secRequestMap) {
        if (secRequestMap == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (secRequestMap.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond secRequestMap.errors, view:'create'
            return
        }

        secRequestMap.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'secRequestMap.label', default: 'SecRequestMap'), secRequestMap.id])
                redirect secRequestMap
            }
            '*' { respond secRequestMap, [status: CREATED] }
        }
    }

    def edit(SecRequestMap secRequestMap) {
        respond secRequestMap
    }

    @Transactional
    def update(SecRequestMap secRequestMap) {
        if (secRequestMap == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (secRequestMap.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond secRequestMap.errors, view:'edit'
            return
        }

        secRequestMap.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'secRequestMap.label', default: 'SecRequestMap'), secRequestMap.id])
                redirect secRequestMap
            }
            '*'{ respond secRequestMap, [status: OK] }
        }
    }

    @Transactional
    def delete(SecRequestMap secRequestMap) {

        if (secRequestMap == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        secRequestMap.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'secRequestMap.label', default: 'SecRequestMap'), secRequestMap.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'secRequestMap.label', default: 'SecRequestMap'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
