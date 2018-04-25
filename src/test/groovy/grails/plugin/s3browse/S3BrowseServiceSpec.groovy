package grails.plugin.s3browse

import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
class S3BrowseServiceSpec extends Specification {

    S3BrowseService service
    def setup() {
        service = new S3BrowseService()
    }

    def cleanup() {
    }

    void "test cleanseFilename"() {
        String of = '1<2/3>e^rt$:-\\x_y-\"s\"[&]*@#%!.pdf'
        when:
        String cf = service.cleanseFilename(of)
        then: 'a new string is returned as cleansed filename, while original filename is not modified'
        cf == '1_2_3_e_rt$_-_x_y-_s_[&]_@#%!.pdf'
        of == '1<2/3>e^rt$:-\\x_y-\"s\"[&]*@#%!.pdf'
    }

    void "test cleanseS3Key"() {
        String key = "!1_2/3-t\$:-\\x\"s\"n[a&b]c^d*e%f's g#.pdf"
        when:
        String cKey = service.cleanseS3Key(key)
        then:
        cKey == "!1_2/3-t__-_x_s_n_a_b_c_d_e_f's_g_.pdf"
    }

    void "test resolveS3Key"() {
        String filename = "this is No#1 - Er's filename_001.xrt"

        when:
        String key = service.resolveS3Key(filename)
        then:
        key == "this_is_No_1_-_Er's_filename_001.xrt"
        filename == "this is No#1 - Er's filename_001.xrt"

        when:
        String prefix = "test/pre1\\pre2"
        key = service.resolveS3Key(filename, prefix)
        then:
        key == "test/pre1/pre2/this_is_No_1_-_Er's_filename_001.xrt"
    }


}