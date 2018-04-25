package grails.plugin.s3browse

import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3Object
import grails.config.Config
import grails.converters.JSON
import grails.core.GrailsApplication
import grails.core.support.GrailsConfigurationAware
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile

class S3BrowseController implements GrailsConfigurationAware {
    S3BrowseService s3BrowseService
    GrailsApplication grailsApplication

    boolean deleteAllowed = false
    boolean insertAllowed = false

    def index() {
        list()
    }

    /**
     * @param params map of
     *  - maxKeys
     *  - prefix
     *  - marker
     */
    def list() {
        params.maxKeys = params.int('maxKeys') ?: 10  // maxKeys of type Integer
        params.prefix = params.prefix ?: ''
        ObjectListing objListing = s3BrowseService.listObjects(params)

        // build view model
        def vm = [maxKeys: params.maxKeys, prefix: params.prefix]
        vm.objectList = objListing.getObjectSummaries().collect { os ->
            [key: os.key, eTag: os.eTag, size: os.size, lastModified: os.lastModified, owner: os.owner]
        }
        if (objListing.isTruncated()) {
            vm.marker = objListing.getNextMarker()
        }

        // build marker track (breadcrumb)
        List<String> markerTrack = params.markerTrack?.split(',') ?: []
        if (vm.marker) {
            def findAt = markerTrack.findIndexOf { it == vm.marker }
            if (findAt >= 0) {
                markerTrack = markerTrack.take(findAt + 1)
            } else {
                markerTrack.add(vm.marker)
            }
        }

        withFormat {
            json { render vm as JSON }
            '*'  { render view: 'list',
                   model: [
                           objectList: vm.objectList,
                           prefix: vm.prefix,
                           maxKeys: vm.maxKeys,
                           marker: vm.marker,    // marker for next page
                           markerTrack: markerTrack.join(','),  // put list back to CSV string
                           deleteAllowed: isDeleteAllowed(),
                           insertAllowed: isInsertAllowed()
                   ] }
        }
    }

    def listPreviousPage() {
        List<String> markerTrack = params.markerTrack?.split(',') ?: []
        if (markerTrack.size() > 2) {
            params.marker = markerTrack[-3]
        } else {
            params.marker = null
        }
        list()
    }

    def listReset() {
        params.remove('marker')
        list()
    }

    def count() {
        def cnt = s3BrowseService.countObjects(params)
        withFormat {
            json { render ([prefix: params.prefix ?: '', count: cnt] as JSON) }
            '*'  { render text: "prefix: ${params.prefix ?: ''}, count: $cnt" }
        }
    }

    def show(String key) {
        def obj = [:]
        s3BrowseService.getObject(key, params).withCloseable { S3Object object ->
            obj.putAll([
                    bucketName: object.bucketName,
                    key: object.key,
                    redirecLocation: object.redirectLocation
            ])
            if (object.objectMetadata) {
                obj.metadata = [
                        contentDisposition: object.objectMetadata.contentDisposition,
                        contentEncoding: object.objectMetadata.contentEncoding,
                        contentLength: object.objectMetadata.contentLength,
                        contentType: object.objectMetadata.contentType,
                        contentMD5: object.objectMetadata.contentMD5
                ]
            }
        }

        withFormat {
            json { render obj as JSON }
            '*'  { render view: 'show', model: [object: obj] }
        }
    }

    def create() {
    }

    def upload() {
        if (!isInsertAllowed()) {
            flash.message = "object insert is not allowed"
            withFormat {
                json { render status: HttpStatus.BAD_REQUEST }
                '*'  { redirect action: 'list',
                        params: [
                                bucketName: params.bucketName,
                                maxKeys: params.maxKeys,
                                prefix: params.prefix,
                                marker: params.marker
                        ]
                }
            }
            return
        }

        MultipartFile f = request.getFile('s3File')
        if (f.empty) {
            flash.message = 'file cannot be empty'
            withFormat {
                json { render status: HttpStatus.BAD_REQUEST, text: ([message: 'file is empty'] as JSON) }
                '*'  { redirect action: 'create',
                        params: [
                                bucketName: params.bucketName,
                                maxKeys: params.maxKeys,
                                prefix: params.prefix,
                                marker: params.marker
                        ]
                }
            }
            return
        }

        Map result = s3BrowseService.uploadFile(
                f.inputStream,
                f.originalFilename,
                [contentType: f.contentType, prefix: params.prefix]
        )

        withFormat {
            json { render status: HttpStatus.CREATED, text: (result as JSON) }
            '*'  { redirect action: 'show', params: [key: result.s3Key] }
        }
    }

    def download(String key) {
         s3BrowseService.getObject(key).withCloseable { S3Object obj ->
             if (!obj) {
                 render status: HttpStatus.NOT_FOUND
                 return
             }

             def fileName = obj.key.replaceAll('/', '__').replaceAll(':', '')

             response.setContentType('application/octet-stream')
             response.setHeader('Content-Disposition', "attachment;filename=\"${fileName}\"")
             // stream s3 object inputStream to response outputStream directly to save memory, default buffer is 4KB
             IOUtils.copy(obj.objectContent, response.outputStream)
             response.flushBuffer()
         }
    }

    def delete(String key) {
        if (request.method != 'POST') {
            render status: HttpStatus.METHOD_NOT_ALLOWED
            return
        }

        if (!isDeleteAllowed()) {
            flash.message = "object deletion is not allowed"
            withFormat {
                json { render status: HttpStatus.METHOD_NOT_ALLOWED }
                '*'  { redirect action: 'list',
                        params: [
                                bucketName: params.bucketName,
                                maxKeys: params.maxKeys,
                                prefix: params.prefix,
                                marker: params.marker
                        ]
                }
            }
            return
        }

        def result = s3BrowseService.deleteObject(key)
        flash.message = "object deleted with key: ${key}"

        withFormat {
            json { render result as JSON }
            '*'  { redirect action: 'list',
                            params: [
                                    bucketName: params.bucketName,
                                    maxKeys: params.maxKeys,
                                    prefix: params.prefix,
                                    marker: params.marker
                            ]
            }
        }
    }

    @Override
    void setConfiguration(Config co) {
        this.deleteAllowed = co.getProperty('s3Browse.deleteAllowed', Boolean, false)
        log.info "set deleteAllowed = ${this.deleteAllowed}"

        this.insertAllowed = co.getProperty('s3Browse.insertAllowed', String)
        log.info "set insertAllowed = ${this.insertAllowed}"
    }

}
