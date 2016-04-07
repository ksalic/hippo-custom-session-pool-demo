package org.onehippo.demo.security;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.demo.util.HippoUser;
import org.onehippo.demo.util.UserUtils;

/**
 * @version "\$Id$" kenan
 */
public class CustomContextCredentialsProvider implements ContextCredentialsProvider {

    private Credentials defaultCredentials;

    private Credentials sitegroupAliveuser;
    private Credentials sitegroupBliveuser;

    private Credentials defaultCredentialsForPreviewMode;
    private Credentials writableCredentials;

    private final Map<String, Credentials> credentialsMap = new HashMap<>();


    public CustomContextCredentialsProvider(final Credentials defaultCredentials,
                                            final Credentials sitegroupAliveuser,
                                            final Credentials sitegroupBliveuser,

                                            final Credentials defaultCredentialsForPreviewMode,
                                            final Credentials writableCredentials) {
        this.defaultCredentials = defaultCredentials;

        this.sitegroupAliveuser = sitegroupAliveuser;
        this.sitegroupBliveuser = sitegroupBliveuser;


        this.defaultCredentialsForPreviewMode = defaultCredentialsForPreviewMode;
        this.writableCredentials = writableCredentials;

        credentialsMap.put("sitegroup-A-group", this.sitegroupAliveuser);
        credentialsMap.put("sitegroup-B-group", this.sitegroupBliveuser);

        credentialsMap.put("admin", this.defaultCredentials);
    }

    public Credentials getDefaultCredentials(HstRequestContext requestContext) {
        if (defaultCredentialsForPreviewMode != null && requestContext.isPreview()) {
            return defaultCredentialsForPreviewMode;
        }
        final HttpSession session = requestContext.getServletRequest().getSession(false);
        if (session == null || requestContext.getServletRequest().getUserPrincipal() == null) {
            return defaultCredentials;
        }

        final HippoUser user = UserUtils.getUser(requestContext);
        String group = user.getGroup();
        if (credentialsMap.containsKey(group)) {
            return credentialsMap.get(group);
        }
        throw new CustomSessionPoolException("not allowed to access the credentials map with group " + user.getUsername());
    }

    public Credentials getWritableCredentials(HstRequestContext requestContext) {
        return writableCredentials;
    }
}