package test.grails.s3browse

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
@EqualsAndHashCode(includes='authority')
@ToString(includes='authority', includeNames=true, includePackage=false)
class SecRole implements Serializable {

	private static final long serialVersionUID = 1

	static final String SEC_ROLE_NAME_ADMIN = 'ROLE_PR_ADMIN'
	static final String SEC_ROLE_NAME_READ = 'ROLE_PR_HOST'

	public static final List<String> SEC_ROLE_NAME_LIST = [SEC_ROLE_NAME_ADMIN, SEC_ROLE_NAME_READ]

	Date dateCreated
	Date lastUpdated
	String id
	String authority

	static constraints = {
		authority blank: false, unique: true
	}

	static mapWith = 'mongo'
	static mapping = {
		cache true
	}
}
