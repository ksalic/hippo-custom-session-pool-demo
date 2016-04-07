package org.onehippo.demo.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.onehippo.demo.util.HippoUser;
import org.onehippo.forge.security.support.springsecurity.authentication.HippoUserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @version "\$Id$" kenan
 */
public class CustomSessionPoolUserDetailService extends HippoUserDetailsServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(CustomSessionPoolUserDetailService.class);

    private static final String DEFAULT_SINGLE_GROUP_OF_USER_QUERY = "//element(*, hipposys:group)[@hipposys:members = ''{0}'' and @hipposys:securityprovider = ''internal'']";


    public UserDetails loadUserByUsernameAndPassword(String username, String password) {
        HippoUser user = null;
        Session session = null;

        try {
            if (getSystemCredentials() != null) {
                session = getSystemRepository().login(getSystemCredentials());
            } else {
                session = getSystemRepository().login();
            }

            String statement = MessageFormat.format(getUserQuery(), username);

            if (log.isDebugEnabled()) {
                log.debug("Searching user with query: " + statement);
            }

            Query q = session.getWorkspace().getQueryManager().createQuery(statement, getQueryLanguage());
            QueryResult result = q.execute();
            NodeIterator nodeIt = result.getNodes();
            Node userNode = (nodeIt.hasNext() ? userNode = nodeIt.nextNode() : null);

            String passwordProp = userNode.getProperty("hipposys:password").getString();
            boolean enabled = userNode.getProperty("hipposys:active").getBoolean();
            boolean accountNonExpired = true;
            boolean credentialsNonExpired = true;
            boolean accountNonLocked = true;
            Collection<? extends GrantedAuthority> authorities = getGrantedAuthoritiesOfUser(username);
            final List<String> groupsOfUser = getGroupsOfUser(username);
            if (groupsOfUser != null | !groupsOfUser.isEmpty()) {
                user = new HippoUser(username, password != null ? password : passwordProp, enabled, accountNonExpired,
                        credentialsNonExpired, accountNonLocked, authorities);
                user.setGroups(groupsOfUser);
                user.setGroup(groupsOfUser.get(0));
            } else {
                throw new CustomSessionPoolException("no groups for user " + username);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to load user.", e);
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                }
            }
        }

        return user;
    }

    private List<String> getGroupsOfUser(final String username) throws RepositoryException {
        Session session = null;
        List<String> groups = new ArrayList<>();
        try {
            if (getSystemCredentials() != null) {
                session = getSystemRepository().login(getSystemCredentials());
            } else {
                session = getSystemRepository().login();
            }

            String statement = MessageFormat.format(DEFAULT_SINGLE_GROUP_OF_USER_QUERY, username);

            if (log.isDebugEnabled()) {
                log.debug("Searching groups of user with query: " + statement);
            }

            Query q = session.getWorkspace().getQueryManager().createQuery(statement, getQueryLanguage());
            QueryResult result = q.execute();
            NodeIterator nodeIt = result.getNodes();

            while (nodeIt.hasNext()) {
                final String group = nodeIt.nextNode().getName();
                groups.add(group);
            }
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                    log.warn("Could not log out from jcr session", ignore);
                }
            }
        }
        return groups;
    }
}
