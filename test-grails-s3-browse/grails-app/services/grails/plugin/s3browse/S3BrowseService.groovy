package grails.plugin.s3browse

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.transfer.Upload
import grails.core.GrailsApplication
import grails.plugin.awssdk.s3.AmazonS3Service
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean

import java.util.regex.Pattern

@Slf4j
class S3BrowseService implements InitializingBean {
    GrailsApplication grailsApplication
    AmazonS3Service amazonS3Service
    AwsS3Service awsS3Service

    /**
     * this is to filter out illegal chars in filename for both *nix and windows OS systems
     * this is for general english filenames
     */
    static final Pattern ILLEGAL_FILENAME_CHAR_PATTERN = ~/[\u0001-\u001f<>:\"^\/\\|?*\u007f]+/
    /**
     * this is to filter out illegal chars for S3 key string
     * see AWS url: https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html#object-keys
     * AWS suggested allowed special chars: . ! / - '
     */
    static final Pattern ILLEGAL_S3_KEY_CHAR_PATTERN = ~/[^0-9a-zA-Z()\.\/!_\-']/

    String bucketName

    boolean deleteAllowed = false
    boolean insertAllowed = false

    /**
     *
     * @param params map of
     *  - bucketName    optional, bucketName, use injected property value if not given
     *  - prefix        optional
     *  - marker        optional
     *  - maxKeys       optional
     * @return  {@link ObjectListing} instance representing the metadata list of S3 objects
     */
    ObjectListing listObjects(Map params) {
        String bucketName = params.bucketName ?: bucketName
        return awsS3Service.listObjects(bucketName, params.prefix, params.marker, params.maxKeys)
    }

    /**
     * @param params map of
     * - bucketName     optional, if not given, use default bucket
     * - prefix         optional
     * @return  total count of the objects
     */
    Long countObjects(Map params) {
        String bucketName = params.bucketName ?: bucketName
        return awsS3Service.countObjects(bucketName, params.prefix as String)
    }

    /**
     * Get S3 object reference
     * @param s3Key   required
     * @param params  optional, map of
     * - bucketName   if not given, use default bucket name
     * @return  s3 object or null if not found
     */
    S3Object getObject(String s3Key, Map params = [:]) {
        def bucketName = params.bucketName ?: bucketName
        if (s3Key && bucketName) {
            return s3.getObject(bucketName, s3Key)
        }
        return null
    }

    /**
     * @param fileStream
     * @param filename
     * @param params map of
     *   - prefix           optional
     *   - contentType      optional
     *   - contentLength    optional
     *   - s3Key            optional, override the s3 object key
     * @return
     */
    Map uploadFile(InputStream fileStream, String filename, Map params = [:]) {
        def bucketName = params.bucketName ?: bucketName


        String s3Key = cleanseS3Key(params.s3Key ?: resolveS3Key(filename, params.prefix?.toString()))
        if (!s3Key) {
            return [status: 'fail', message: 'filename or S3 key given is not valid']
        }

        //todo: support params.overWrite = true/false switch if key is found by filename
        Upload upload = awsS3Service.upTransferStream(bucketName, s3Key, fileStream, [
                filename: filename,
                sync: true,
                contentLength: params.contentLength,
                contentType: params.contentType
        ])

        return [status: 'success', s3Key: s3Key, uploadState: upload.state?.toString()]
    }

    /**
     * @param key           required
     * @param bucketName    optional, if not given, use default bucket name
     */
    void deleteObject(String key, String bucketName = null) {
        if (!bucketName) {
            bucketName = this.bucketName
        }
        if (key && bucketName) {
            s3.deleteObject(bucketName, key)
        }
    }

    String resolveS3Key(String filename, String prefix = null) {
        if (prefix) {
            prefix = prefix.replaceAll('\\\\', '/')  // '\' => '/'
            return cleanseS3Key(prefix + '/' + cleanseFilename(filename))
        }
        return cleanseS3Key(cleanseFilename(filename))
    }

    protected String cleanseFilename(final String filename) {
        filename?.replaceAll(ILLEGAL_FILENAME_CHAR_PATTERN, "_")
    }

    protected String cleanseS3Key(String s3Key) {
        s3Key?.replaceAll(ILLEGAL_S3_KEY_CHAR_PATTERN, "_")
    }

    protected AmazonS3 getS3() {
        amazonS3Service.client
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (grailsApplication.config.s3Browse?.bucketName) {
            this.bucketName = grailsApplication.config.s3Browse.bucketName
            log.info "set bucketName = ${this.bucketName}"
        } else {
            throw new RuntimeException("bucketName not set, it must be provided")
        }

        if (grailsApplication.config.s3Browse?.deleteAllowed) {
            this.deleteAllowed = Boolean.parseBoolean(grailsApplication.config.s3Browse.deleteAllowed.toString())
        }
        log.info "set deleteAllowed = ${this.deleteAllowed}"

        if (grailsApplication.config.s3Browse?.insertAllowed) {
            this.insertAllowed = Boolean.parseBoolean(grailsApplication.config.s3Browse.insertAllowed.toString())
        }
        log.info "set insertAllowed = ${this.insertAllowed}"


    }


}
