package org.onehippo.demo.util;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @version "\$Id$" kenan
 */
public class HippoUser extends User {

    private List<String> groups;
    private String group;

    public HippoUser(final String username, final String password, final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public HippoUser(final String username, final String password, final boolean enabled, final boolean accountNonExpired, final boolean credentialsNonExpired, final boolean accountNonLocked, final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(final List<String> groups) {
        this.groups = groups;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }
}
