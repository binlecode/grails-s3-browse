package grails.plugin.s3browse

import com.amazonaws.AmazonClientException
import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.internal.Constants
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.GetObjectMetadataRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.transfer.Download
import com.amazonaws.services.s3.transfer.MultipleFileDownload
import com.amazonaws.services.s3.transfer.MultipleFileUpload
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.transfer.Upload
import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import grails.plugin.awssdk.s3.AmazonS3Service
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Provide common AWS S3 utilities in addition to AWS-SDK-S3 Grails plugin and AWS Java SDK.
 * This service class compliments the {@link AmazonS3Service}.
 * It is recommended to check that class first for intend
 *
 * @see {@link AmazonS3} for underlying s3 client level API
 * @see {@link TransferManager} for underlying s3 concurrent and async transfer API
 */
@Slf4j
class AwsS3Service implements GrailsConfigurationAware, InitializingBean {
    static transactional = false
    AmazonS3Service amazonS3Service

    static final Long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = (16 * Constants.MB)
    static final Long DEFAULT_MIN_UPLOAD_PART_SIZE = (5 * Constants.MB)
    static final Integer DEFAULT_MAX_UPLOAD_TRANSFER_THREADS = 50
    static final CannedAccessControlList DEFAULT_ACL = CannedAccessControlList.Private
    static final int DEFAULT_STREAM_BUFFER_SIZE = 4096

    long multipartUploadThreshold
    long minimumUploadPartSize
    int maximumUploadTransferThreads

    /**
     * TransferManager creates its own connection pool, and should be reused, and it is thread-safe
     * For default configuration @see {@link com.amazonaws.services.s3.transfer.TransferManagerConfiguration}
     */
    TransferManager transferManager


    @Override
    void setConfiguration(Config co) {
        multipartUploadThreshold = co.getProperty('s3f.s3.multipartUploadThreshold', long, DEFAULT_MULTIPART_UPLOAD_THRESHOLD)
        log.info "set multipartUploadThreshold = $multipartUploadThreshold"
        minimumUploadPartSize = co.getProperty('s3f.s3.minimumUploadPartSize', long, DEFAULT_MIN_UPLOAD_PART_SIZE)
        log.info "set minimumUploadPartSize = $minimumUploadPartSize"
        maximumUploadTransferThreads = co.getProperty('s3f.s3.maximumUploadTransferThread', int, DEFAULT_MAX_UPLOAD_TRANSFER_THREADS)
        log.info "set maximumUploadTransferThreads = $maximumUploadTransferThreads"
    }

    @Override
    void afterPropertiesSet() throws Exception {
        transferManager = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .withMultipartUploadThreshold(multipartUploadThreshold)
                .withMinimumUploadPartSize(minimumUploadPartSize)
                .build()
    }

    AmazonS3 getS3Client() {
        amazonS3Service.client
    }

    /**
     * @param params map of
     * - bucketName     optional, if not given, use default bucket
     * - prefix         optional
     * @return  total count of the objects
     */
    long countObjects(String bucketName, String prefix) {
        def listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
        if (prefix) {
            listObjectsRequest.setPrefix(prefix)
        }
        def objListing = s3Client.listObjects(listObjectsRequest)
        long objCount = objListing.objectSummaries.size()
        while (objListing.isTruncated()) {
            listObjectsRequest.setMarker(objListing.nextMarker)
            objListing = s3Client.listObjects(listObjectsRequest)
            objCount += objListing.objectSummaries.size()
        }
        return objCount
    }

    /**
     * Lists S3 objects with pagination support
     * @param bucketName
     * @param prefix
     * @param marker
     * @param maxKeys
     */
    ObjectListing listObjects(String bucketName, String prefix = null, String marker = null, Integer maxKeys = null) {
        def listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
        if (prefix) {
            listObjectsRequest.setPrefix(prefix)
        }
        if (marker) {
            listObjectsRequest.setMarker(marker)
        }
        if (maxKeys) {
            listObjectsRequest.setMaxKeys(maxKeys)
        }
        return s3Client.listObjects(listObjectsRequest)
    }

    /**
     * Uploads byte array with optional custom {@link TransferManager}
     *
     * @param bucketName    required, name of the s3 bucket
     * @param s3Key         required, the path of the file as it will appear on s3
     * @param byteArray     required, file data to be uploaded to S3
     * @param filename      required, name of the file to be saved as object metadata
     * @param params        optional, map of @see {@link #upTransferStream(String, String, InputStream, ObjectMetadata, Map)}
     */
    Upload upTransferBytes(String bucketName, String s3Key, byte[] byteArray, String filename, Map params = [:]) {
        ObjectMetadata omd = new ObjectMetadata()
        omd.setContentLength(byteArray.length)
        omd.setHeader("filename", filename)

        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray)
        upTransferStream(bucketName, s3Key, bis, omd, params)
    }

    /**
     * Upload file stream with AWS SDK {@link TransferManager}
     *
     * @param bucketName    required, name of S3 bucket
     * @param s3Key         required, the s3 object key for the upload file
     * @param inputStream   required, {@link java.io.InputStream} object to be uploaded
     * @param params        optional, map of values to config {@link ObjectMetadata} object, includes:
     * - contentLength  optional
     * - contentType    optional
     * - expirationTime optional
     * - filename       optional
     * - s3Acl          optional {@link CannedAccessControlList} enum value for S3 object access control
     * - sync           optional boolean, if true this is a blocking call that won't return until upload is done
     *
     * @return {@link com.amazonaws.services.s3.transfer.Upload} object
     */
    Upload upTransferStream(String bucketName, String s3Key, InputStream inputStream, Map params = [:]) {

        ObjectMetadata omd = new ObjectMetadata()
        // according to ObjectMetadata doc, if content-length is not given,
        // the stream content has to be buffered to calculate it
        if (params.contentLength) {
            omd.setContentLength(params.contentLength)
        }
        if (params.contentType) {
            omd.setContentType(params.contentType)
        }
        if (params.filename) {
            omd.setHeader("filename", params.filename)
        }
        if (params.expirationTime) {
            omd.setExpirationTime(params.expirationTime)
        }

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Key, inputStream, omd)
                .withCannedAcl((params.s3Acl as CannedAccessControlList) ?: DEFAULT_ACL)

        Upload upload = getTransferManager().upload(putObjectRequest)
        if (params.sync) {
            upload.waitForCompletion()
        }
        upload
    }

    /**
     * Uploads input stream with optional custom AWS SDK {@link TransferManager}
     * @param bucketName
     * @param s3Key
     * @param inputStream stream data to be uploaded to S3
     * @param omd S3 {@link ObjectMetadata} object required to upload an input stream
     * @param params optional map of
     * - s3Acl {@link CannedAccessControlList} enum value
     * - sync  boolean, if true this is a blocking call that won't return until upload is done
     */
    Upload upTransferStream(String bucketName, String s3Key, InputStream inputStream, ObjectMetadata omd, Map params = [:]) {
        PutObjectRequest putObject = new PutObjectRequest(bucketName, s3Key, inputStream, omd)
                .withCannedAcl(params.s3Acl ?: DEFAULT_ACL)

        //todo: annotate this method with LogTime annotation for time tracking
        Upload upload = getTransferManager().upload(putObject)
        if (params.sync) {
            upload.waitForCompletion()
        }
        upload
    }

    /**
     * Uploads file with AWS SDK {@link TransferManager}
     * @param bucketName    required
     * @param s3Path        required
     * @param file          required
     * @param params        optional map of
     * - s3Acl {@link CannedAccessControlList} enum value
     * - sync  boolean, if true this is a blocking call that won't return until upload is done
     */
    Upload upTransferFile(String bucketName, String s3Path, File file, Map params = [:]) {
        PutObjectRequest putObject = new PutObjectRequest(bucketName, s3Path, file)
                .withCannedAcl(params.s3Acl ?: DEFAULT_ACL)
        //todo: annotate this method with LogTime annotation for time tracking
        Upload upload = getTransferManager().upload(putObject)
        if (params.sync) {
            upload.waitForCompletion()
        }
        upload
    }

    /**
     * @param bucketName Name of the bucket to upload to
     * @param virtualDirectoryKeyPrefix top level sub directory of the bucket you want to upload to
     * @param directory the local directory to be uploaded
     * @param params optional map of
     * - excludeSubDir boolean, whether or not to include all sub directories in the upload.  default is true
     * - sync boolean, if true this is a blocking call that won't return until upload is done
     */
    MultipleFileUpload upTransferDirectory(String bucketName, String virtualDirectoryKeyPrefix, File directory, Map params = [:]) {
        boolean includeSub = true
        if (Boolean.parseBoolean(params.excludeSubDir?.toString())) {
            includeSub = false
        }
        MultipleFileUpload multipleFileUpload = getTransferManager().uploadDirectory(bucketName, virtualDirectoryKeyPrefix, directory, includeSub)
        if (params.sync) {
            multipleFileUpload.waitForCompletion()
        }
        multipleFileUpload
    }

    /**
     * Downloads S3 object to local file using transfer manager.
     *
     * @see {@link #downTransferFile(String, String, File)}
     * @param bucketName
     * @param s3Key
     * @param filePathName
     * @param params map of
     * - sync boolean, if true this is a blocking call that won't return until upload is done
     */
    Download downTransferFile(String bucketName, String s3Key, String filePathName, Map params = [:]) {
        downTransferFile(bucketName, s3Key, new File(filePathName), params)
    }

    /**
     * @param bucketName
     * @param s3Key
     * @param file
     * @param params map of
     * - sync boolean, if true this is a blocking call that won't return until upload is done
     */
    Download downTransferFile(String bucketName, String s3Key, File file, Map params = [:]) {
        Download download = getTransferManager().download(new GetObjectRequest(bucketName, s3Key), file)
        if (params.sync) {
            download.waitForCompletion()
        }
        download
    }

    /**
     * @param bucketName
     * @param prefix
     * @param destDirPathName
     * @param params map of
     * - sync boolean, if true this is a blocking call that won't return until upload is done
     */
    MultipleFileDownload downTransferDirectory(String bucketName, String prefix, String destDirPathName, Map params = [:]) {
        MultipleFileDownload mfd = getTransferManager().downloadDirectory(bucketName, prefix, new File(destDirPathName))
        if (params.sync) {
            mfd.waitForCompletion()
        }
        mfd
    }

    /**
     * Tries to fetch s3 object metadata by bucket name and key.
     * This is essentially a try-catch wrapper on {@link AmazonS3#getObjectMetadata(java.lang.String, java.lang.String)}
     *
     * @param bucketName
     * @param s3Key
     * @return {@link ObjectMetadata} object or null if object doesn't exist
     */
    ObjectMetadata getObjectMetadata(String bucketName, String s3Key) {
        ObjectMetadata om
        try {
            om = s3Client.getObjectMetadata(new GetObjectMetadataRequest(bucketName, s3Key, null))
        } catch (AmazonS3Exception exception) {
            log.warn 'An amazon S3 exception was caught while checking if file exists', exception
        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was caught while checking if file exists', exception
        }
        return om
    }

    /**
     * Generates presigned url for target s3 object. This method complements existing API from
     * {@link AmazonS3#generatePresignedUrl(java.lang.String, java.lang.String, java.util.Date)}
     * and {@link AmazonS3#generatePresignedUrl(java.lang.String, java.lang.String, java.util.Date, com.amazonaws.HttpMethod)}
     *
     * @param bucketName Name of the bucket the file resides in
     * @param s3Key Name of the file on S3
     * @param timeWindowMilliseconds Time window the url will remain good for in milliseconds
     * @param httpMethod type of request the link should be used with.  Default is GET
     * @return Self Signed Url, eg.
     *      https://s3.amazonaws.com/dda_df_dps/pns/dev/3tset.568.TIOGM/savetest15.jpg?
     *      AWSAccessKeyId=ACCESSKEYID&Expires=1456499066&Signature=Z8mgYHT7j5M9yFPvOP8U2LEeQB0%3D
     */
    URL generatePresignedUrl(String bucketName, String s3Key, Long timeWindowMilliseconds, HttpMethod httpMethod = HttpMethod.GET) {
        Date expiration = new Date(new Date().getTime() + timeWindowMilliseconds)
        s3Client.generatePresignedUrl(bucketName, s3Key, expiration, httpMethod)
    }


    // ** io streams **

    /**
     * Gets the s3 object and pipe to an output stream.
     * Note this is a blocking call keeping the underlying http connection open until the stream is drained.
     * The caller should wire the output stream before calling (non-blocking), and take care of output stream
     * flush and closing after calling.
     *
     * @see {@link AmazonS3#getObject(java.lang.String, java.lang.String)}
     *
     * @param oStream the target output stream
     * @param bucketName name of S3 bucket
     * @param s3Key the path to the file on S3
     * @param options map of
     * - bufferSize optional integer size of buffer used in data streaming
     */
    void streamObject(OutputStream oStream, String bucketName, String s3Key, Map options = [:]) {
        s3Client.getObject(bucketName, s3Key).withCloseable { S3Object s3Object ->
            int bufSize = options.bufferSize ?: DEFAULT_STREAM_BUFFER_SIZE
            int bytesIn
            byte[] readBuffer = new byte[bufSize]
            InputStream is = s3Object.objectContent
            while ((bytesIn = is.read(readBuffer)) != -1) {
                oStream.write(readBuffer, 0, bytesIn)
            }
        }
    }

    // ** zip packaging **

    /**
     * Gets the s3 objects and pipe to an output stream.
     * Note this is a blocking call until all the objects are streamed out.
     * The caller should wire the output stream before calling (non-blocking), and take care of the stream flush
     * and closing after calling.
     *
     * @param oStream
     * @param bucketName
     * @param s3Keys
     * @param options map of
     * - rootFolderName string
     * - bufferSize optional integer
     */
    void streamZip(OutputStream oStream, String bucketName, List<String> s3Keys, Map options = [:]) {
        // use withCloseable to auto close stream
        new ZipOutputStream(oStream).withCloseable { ZipOutputStream zos ->
            // if package root folder name is given, add it first
            String rfName = ''
            if (options.rootFolderName) {
                rfName = "${options.rootFolderName}/"  // use '/' to specify folder
                zos.putNextEntry(new ZipEntry(rfName))
            }

            s3Keys.each { String s3Key ->
                // error control at each file level, so if one file fails, the zipping won't abort
                try {



                    String filename = convertFilenameFromS3Key(s3Key) //todo: need a more practical filename resolving
                    ZipEntry zipEntry = new ZipEntry(rfName + filename)
                    zos.putNextEntry(zipEntry)

                    streamObject(zos, bucketName, s3Key, options)
                    log.debug "Finish streaming s3 object to zip entry, filename: ${filename}"
                } catch (ex) {
                    log.error ex.message, ex
                    // do nothing, skip it by returning current closure run
                } finally {
                    zos.flush()
                    zos.closeEntry()  // MUST use closeEntry() to close each file entry
                }
            }
        }
    }

    /**
     * Downloads S3 objects with given key prefix and pipe to given output stream
     * @param oStream       required output stream
     * @param bucketName    required
     * @param prefix        required
     * @param options       optional map of
     * -
     */
    void streamZip(OutputStream oStream, String bucketName, String prefix, Map options = [:]) {
        //todo: get object list from prefix

        //todo: if root folder name is not given, the default should be converted from prefix

        //todo: the inteneded folder structure should be s3key trimmed off by the prefix
        //todo:   to build relative folder path


    }


    /**
     * Download S3 objects in stream and save to disk file.
     * @param zipFilePathName
     * @param bucketName
     * @param s3Keys
     * @param options @see {@link #streamZip(java.io.OutputStream, java.lang.String, java.util.List, java.util.Map)}
     */
    void streamZipFile(String zipFilePathName, String bucketName, List<String> s3Keys, Map options = [:]) {
        // extract folder name from pathname for zip package extracted root folder name
        if (!options.rootFolderName) {
            options.rootFolderName = zipFilePathName.split('\\.')[-2].split('/')[-1]
        }
        streamZip(new FileOutputStream(zipFilePathName), bucketName, s3Keys, options)
    }

    /**
     * Convert s3 path to file name
     * - replace all '/' and '\' with '-'
     */
    protected String convertFilenameFromS3Key(String s3Key) {
        s3Key.replaceAll(/\/+/, '-')
    }

    protected TransferManager getTransferManager() {
        amazonS3Service.transferManager ?: this.transferManager
    }


}

