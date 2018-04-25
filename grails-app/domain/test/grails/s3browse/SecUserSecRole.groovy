package test.grails.s3browse

import grails.gorm.DetachedCriteria
import groovy.transform.ToString

import org.codehaus.groovy.util.HashCodeHelper
import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
@ToString(cache=true, includeNames=true, includePackage=false)
class SecUserSecRole implements Serializable {

	private static final long serialVersionUID = 1

	SecUser secUser
	SecRole secRole

	static constraints = {
		secRole validator: { SecRole r, SecUserSecRole ur ->
			if (ur.secUser?.id) {
				SecUserSecRole.withNewSession {
					if (SecUserSecRole.exists(ur.secUser.id, r.id)) {
						return ['userRole.exists']
					}
				}
			}
		}
	}

	static mapWith = 'mongo'
	static mapping = {
		version false
		id composite: ['secUser', 'secRole']
		secUser index: true
		secRole index: true
	}

	@Override
	boolean equals(other) {
		if (other instanceof SecUserSecRole) {
			other.secUserId == secUser?.id && other.secRoleId == secRole?.id
		}
	}

    @Override
	int hashCode() {
	    int hashCode = HashCodeHelper.initHash()
        if (secUser) {
            hashCode = HashCodeHelper.updateHash(hashCode, secUser.id)
		}
		if (secRole) {
		    hashCode = HashCodeHelper.updateHash(hashCode, secRole.id)
		}
		hashCode
	}

	static SecUserSecRole get(String secUserId, String secRoleId) {
		criteriaFor(secUserId, secRoleId).get()
	}

	static boolean exists(String secUserId, String secRoleId) {
		criteriaFor(secUserId, secRoleId).count()
	}

	private static DetachedCriteria criteriaFor(String secUserId, String secRoleId) {
		SecUserSecRole.where {
			secUser == SecUser.load(secUserId) &&
			secRole == SecRole.load(secRoleId)
		}
	}

	static SecUserSecRole create(SecUser secUser, SecRole secRole, boolean flush = false) {
		def instance = new SecUserSecRole(secUser: secUser, secRole: secRole)
		instance.save(flush: flush)
		instance
	}

	static boolean remove(SecUser u, SecRole r) {
		if (u != null && r != null) {
			SecUserSecRole.where { secUser == u && secRole == r }.deleteAll()
		}
	}

	static int removeAll(SecUser u) {
		u == null ? 0 : SecUserSecRole.where { secUser == u }.deleteAll() as int
	}

	static int removeAll(SecRole r) {
		r == null ? 0 : SecUserSecRole.where { secRole == r }.deleteAll() as int
	}


}
