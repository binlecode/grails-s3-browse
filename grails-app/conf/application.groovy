
// fix bug for grails web console plugin, default is /static/**
grails.resources.pattern = '/**'

// default logoutController only allows POST, disable to allow GET
grails.plugin.springsecurity.logout.postOnly = false

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'test.grails.s3browse.SecUser'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'test.grails.s3browse.SecUserSecRole'
grails.plugin.springsecurity.authority.className = 'test.grails.s3browse.SecRole'
grails.plugin.springsecurity.requestMap.className = 'test.grails.s3browse.SecRequestMap'

// Unlike the Config.groovy Map approach, you do NOT need to revise the Requestmap entry order because
// the plugin calculates the most specific rule that applies to the current request.
grails.plugin.springsecurity.securityConfigType = 'Requestmap'

// these static rules are auto-loaded by plugin initialization (as it defaults to in-memory rules).
// thus they are NOT effective in the case of requestMap being stored in database
//grails.plugin.springsecurity.controllerAnnotations.staticRules = [
//	[pattern: '/',               access: ['permitAll']],
//	[pattern: '/error',          access: ['permitAll']],
//	[pattern: '/index',          access: ['permitAll']],
//	[pattern: '/index.gsp',      access: ['permitAll']],
//	[pattern: '/shutdown',       access: ['permitAll']],
//	[pattern: '/assets/**',      access: ['permitAll']],
//	[pattern: '/**/js/**',       access: ['permitAll']],
//	[pattern: '/**/css/**',      access: ['permitAll']],
//	[pattern: '/**/images/**',   access: ['permitAll']],
//	[pattern: '/**/favicon.ico', access: ['permitAll']]
//]

grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/',               filters: 'none'],  // default home page
	    [pattern: '/assets/**',      filters: 'none'],
	    [pattern: '/**/js/**',       filters: 'none'],
	    [pattern: '/**/css/**',      filters: 'none'],
	    [pattern: '/**/images/**',   filters: 'none'],
	    [pattern: '/**/favicon.ico', filters: 'none'],
        [pattern: '/errors/**',      filters: 'none'],
        // system devop endpoints
        [pattern: '/actuator/health',filters: 'none'],  // spring boot actuator endpoints
//        [pattern: '/monitoring/**',  filters: 'none'],  // java melody JVM status web page
//        [pattern: '/console/**',     filters: 'none'],  // runtime web console
        // all the rest fall into security check
        [pattern: '/**',             filters: 'JOINED_FILTERS']
]
