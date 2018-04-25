package test.grails.s3browse

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.http.HttpMethod

@Slf4j
class BootStrap {
    GrailsApplication grailsApplication

    def init = { servletContext ->

        log.info "config.server.port=${grailsApplication.config.server.port}"
        log.info "config.server.contextPath=${grailsApplication.config.server.contextPath}"
        log.info "config.server.max-http-post-size=${grailsApplication.config.server.'max-http-post-size'}"
        log.info "config.grails.controllers.upload.maxFileSize=${grailsApplication.config.grails.controllers.upload.maxFileSize}"
        log.info "config.grails.controllers.upload.maxRequestSize=${grailsApplication.config.grails.controllers.upload.maxRequestSize}"
        log.info "config.spring.http.multipart.max-file-size=${grailsApplication.config.spring.http.multipart.'max-file-size'}"

        log.info "grailsApplication.metadata.applicationVersion=${grailsApplication.metadata.getApplicationVersion()}"
        log.info "grailsApplication.metadata.grailsVersion=${grailsApplication.metadata.getGrailsVersion()}"
        log.info "grailsApplication.metadata.environment=${grailsApplication.metadata.getEnvironment()}"
        log.info "actuator config.endpoints.enabled=${grailsApplication.config.endpoints.enabled}"
        log.info "webconsole config.grails.plugin.console.enabled=${grailsApplication.config.grails.plugin.console.enabled}"

        log.info "config.dataSource.url=${grailsApplication.config.dataSource.url}"
        log.info "config.dataSource.username=${grailsApplication.config.dataSource.username}"
        log.info "config.dataSource.dbCreate=${grailsApplication.config.dataSource.dbCreate}"

        log.info "config.grails.mongodb.url=${grailsApplication.config.grails.mongodb.url}"

        seedSecRoleData()
        seedSecRequestMap(true)
        seedSecUser()
    }

    def destroy = {
    }

    private seedSecRoleData() {
        log.info 'seeding SecRole data ...'
        SecRole.withTransaction {
            ['ROLE_ADMIN', 'ROLE_WRITE', 'ROLE_READ'].each { String role ->

                SecRole sra = SecRole.findByAuthority(role)
                if (!sra) {
                    sra = new SecRole(authority: role).insert(failOnError: true)
                    log.info "seeded role: $sra"
                }
            }
        }
    }

    private seedSecRequestMap(boolean purge = false) {
        log.info 'seeding SecRequestMap data ...'

        if (purge) {
            log.warn "purging existing SecRequestMap data"
            SecRequestMap.where {}.deleteAll()
        }

        SecRequestMap.withTransaction {
            for (String url in [
                    '/',
                    '/error',
                    '/index',
                    '/index.gsp',
                    '/shutdown',
                    '/**/favicon.ico',
                    '/**/js/**',
                    '/**/css/**',
                    '/**/images/**',
                    '/login', '/login.*', '/login/*',
                    '/logout', '/logout.*', '/logout/*',
            ]) {
                SecRequestMap srm = SecRequestMap.findByUrl(url)
                if (!srm) {
                    srm = new SecRequestMap(url: url, configAttribute: 'permitAll').save(failOnError: true)
                    log.info "seeded secRequestMap: $srm"
                }
            }
        }

        // admin access
        SecRequestMap.withTransaction {
            // enhanced with fullyAuthenticated
            // a “fully authenticated” authentication (i.e. an explicit login was performed without using remember-me)
            for (String url in [
                    '/console/**',
                    '/actuator/**',
            ]) {
                SecRequestMap srm = SecRequestMap.findByUrl(url)
                if (!srm) {
//                    srm = new SecRequestMap(url: url, configAttribute: 'ROLE_ADMIN,isFullyAuthenticated()').save(failOnError: true)
                    srm = new SecRequestMap(url: url, configAttribute: 'ROLE_ADMIN').save(failOnError: true)
                    log.info "seeded secRequestMap: $srm"
                }
            }

            for (String url in [
                    '/secRole/**',
                    '/secUser/**',
                    '/secUserSecRole/**',
                    '/secRequestMap/**',
//                    '/**/save', '/**/save/**',
//                    '/**/update', '/**/update/**',
//                    '/**/delete', '/**/delete/**'
            ]) {
                SecRequestMap srm = SecRequestMap.findByUrl(url)
                if (!srm) {
                    srm = new SecRequestMap(url: url, configAttribute: 'ROLE_ADMIN').save(failOnError: true)
                    log.info "seeded secRequestMap: $srm"
                }
            }
        }

        // write access
        SecRequestMap.withTransaction {
            for (String url in [
                    '/s3Browse/create',
                    '/s3Browse/upload',
                    '/s3Browse/create',
                    '/s3Browse/delete',
//                    '/**/save', '/**/save/**',
//                    '/**/update', '/**/update/**',
//                    '/**/delete', '/**/delete/**'
            ]) {
                SecRequestMap srm = SecRequestMap.findByUrl(url)
                if (!srm) {
                    srm = new SecRequestMap(url: url, configAttribute: 'ROLE_WRITE').save(failOnError: true)
                    log.info "seeded secRequestMap: $srm"
                }
            }
        }

        // read access
        SecRequestMap.withTransaction {
            for (String url in [
                    '/s3Browse',
                    '/s3Browse/index',
                    '/s3Browse/list',
                    '/s3Browse/listPreviousPage',
                    '/s3Browse/listReset',
                    '/s3Browse/count',
                    '/s3Browse/show',
                    '/s3Browse/download',
            ]) {
                SecRequestMap srm = SecRequestMap.findByUrl(url)
                if (!srm) {
                    srm = new SecRequestMap(url: url, configAttribute: 'ROLE_READ', httpMethod: HttpMethod.GET).save(failOnError: true)
                    log.info "seeded secRequestMap: $srm"
                }
            }
        }

        // any requestMap update should involve a cache flush and triggers a complete reload
        log.info "refreshing request map cache"
        grailsApplication.mainContext.springSecurityService.clearCachedRequestmaps()

    }

    private seedSecUser(boolean purge = false) {
        log.info 'seeding SecUser and SecUserSecRole data ...'
        if (purge) {
            log.warn "purging existing SecUserSecRole data"
            SecUserSecRole.where {}.deleteAll()
            log.warn "purging existing SecUser data"
            SecUser.where {}.deleteAll()
        }

        SecUser.withTransaction {
            for (Map<String, String> user in [
                    [username: 'admin@tgs3b.com',   pswd: 'Admin123!', role: 'ROLE_ADMIN,ROLE_WRITE,ROLE_READ'],
                    [username: 'writer@tgs3b.com',  pswd: 'Test123!',  role: 'ROLE_WRITE,ROLE_READ'],
                    [username: 'reader@tgs3b.com',  pswd: 'Test123!',  role: 'ROLE_READ'],
            ]) {
                SecUser su = SecUser.findByUsername(user.username)
                if (!su) {
                    su = new SecUser(username: user.username, password: user.pswd).insert(failOnError: true)
                    user.role.split(',')*.trim().each { String role ->
                        SecUserSecRole susr = SecUserSecRole.create(su, SecRole.findByAuthority(role))
                        log.info "seeded secUserSecRole: $susr"
                    }
                }
            }
        }

    }

}
