# Hippo CMS demo in how to use Custom Session Pools with the HST


Introduction
Case Study
Session Pools
Solution
Known limitations
Implementation
Repository
Users
Groups
Domains
HST
Custom Session Pools
Authentication with Spring Security
Custom Context Credentials Provider
Demo and Source



##Introduction

Developing a high performant intranet or any other solution which uses authentication with an explicit authorization model with Hippo CMS and the HST will require to do some research about practical use of Custom Session Pools within the HST.

Our case is a high performant intranet solution with the use of a subject based session and a tremendous amount of security domains (100+). This combination can dramatically decrease performance especially when executing complex queries on faceted navigation.

In the past we have introduced a subject based session. Which the developer can set as a property on the mount:

HST also provides Repository level authorization integration, meaning JCR session used during the request will be tightly integrated with the authenticated user's subject.
If you want this option, set the following property for a mount:

- hst:subjectbasedsession = true

With the configuration above, JCR session during the request processing is created with the JCR credentials of the authenticated user's subject per each request. In this case, JCR session is not borrowed from the internal JCR session pool.

This will make sure once the visitor logins in to the site the website will only show content the visitor is allowed to have access to according to the security domains configured within the repository.

The problem with subject based session  is that having a tremendous amount of security domains configured will slow down performance by a lot. There are several things happening on the background; lucene queries which are composed can get quite big and this needs to be calculated for each user during login. The security domains have wildcards which need to be recalculated for each login. One user might not be a problem at start, but having thousands of user login in simultaneously can break the stability of the application very fast.



## Case Study

The case study has been the following:

We are going to implement an intranet solution with an over 10.000 users. All users are split up in 16 different site groups. These 16 site groups consist of a combination of 100+ custom security domains. All users are a member of one “site group” only (please see section “limitations” for more info supporting multiple site groups per user).

We are implementing authentication with Spring Security, using the HST Spring Security Plugin.

The first approach was using the subject based session on the mount as described in the Introduction. During load test we have noticed that the stability of the website has significantly decreased after 6 users login and performing HST queries on faceted navigation nodes.

After investigation we found out that the lucene queries being created after login resulted in a significant performance decrease which resulted in application instability.



## Session Pools

Before we think of a solution let us first investigate, elaborate what is actually a session pool? Which ones already exist? How do I create/configure a new one and how do I use it?

We’ve all worked with session pools. Or at least the following should be familiar:

liveuser
previewuser
configuser
sitewriter

These are (technical) users within the CMS which have access to certain parts of the repository. All of these users belong to certain groups, security domains which have restricted access.

As an example, the liveuser has access to all live (published) documents in the repository. This is the default user which is impersonated when  a visitor requests a page via the Hippo Delivery Tier .

In contrast, the previewuser has access to all preview (unpublished) documents in the repository. This is the default user for requests to  the website through the channel manager in Hippo CMS.

Notice these are (technical) users which need to be highly performant.

For each request of the website visitor a (JCR) session is being retrieved from the appropriate session pools. Which session needs to be retrieved for which user is determined by the ContextCredentialsProvider:

org.hippoecm.hst.site.request.DefaultContextCredentialsProvider

This ContextCredentialsProvider is configured to resemble the (technical) users (liveuser, previewuser etc.). This is configured within Spring e.g:

```
# session pooling repository for default live site access. (typically disallowed on unpublished contents.)
default.repository.address = vm://
default.repository.user.name = liveuser
default.repository.pool.name = default
default.repository.password =
```


```
<bean id="javax.jcr.Credentials.default" class="org.hippoecm.hst.core.jcr.SimpleCredentialsFactoryBean">
 <property name="userId" value="${default.repository.user.name}"/>
 <property name="separator" value="${repository.pool.user.name.separator}"/>
 <property name="poolName" value="${default.repository.pool.name}"/>
 <property name="password" value="${default.repository.password}"/>
 <property name="hstJmvEnabledUsers" ref="hstJmvEnabledUsers"/>
</bean>

<!-- Default request context based credentials provider -->
<bean id="org.hippoecm.hst.core.request.ContextCredentialsProvider" class="org.hippoecm.hst.site.request.DefaultContextCredentialsProvider">
 <constructor-arg ref="javax.jcr.Credentials.default" />
 <constructor-arg ref="javax.jcr.Credentials.preview" />
 <constructor-arg ref="javax.jcr.Credentials.writable" />
</bean>
```

## Solution

For the solution we would like to see in what way we can leverage the high performing session pools to use in this intranet setup.

Instead of having only the default users (liveuser, previewuser (amongst others)) We will create custom session pools for each of our 16 site groups and  configure these  similarly as the liveuser, previewuser etc are configured within the default HST configuration

To indicate in which group this user belongs to we would somehow need to set a flag in the httpsession during login. With the Spring Security Support we can take complete control of the authentication and  perform calculations to determine which session pool needs to be used for the currently logged in user.
Known limitations
For this solution there are several restrictions to consider and to be discussed with the product owner.

A user can’t belong to more site groups at the same time. 1 user can belong to 1 group at the time. We can’t combine the session of multiple groups. If group A has access to Content C and group B has access to content D. User X can’t see Content C and D at the same time having membership to group A and B. A logged in user always needs to retrieve credentials for the session pool which belongs to 1 group. Or create a tremendous amount of session pools to make this work, for each combination a custom session pool.

Wildcards in the security domains configuration won’t work. In the facet rules within security domain it is possible to configure the username wildcard “__user__” to create a domain which can use this wildcard to retrieve nodes from the repository with a facet rule that matches the username of the logged in user.

To put the known limitations short: This solutions contains the creation of 16 technical users, which belong to 16 (site) groups. During login on the site 1 of these 16 technical user are mapped to the logged in user. It is similar as impersonating the logged in user, to 1 out of the 16 technical users. And this is going to perform much better than a subject based session solution!


## Implementation
The following section will describe which implementation actions need to be taken to get this case to work. Mainly there are 2 parts; Repository and HST work which needs to be implemented.
### Repository

#### Users

First create a technical user for each site group:
```
<?xml version="1.0" encoding="UTF-8"?>
<sv:node sv:name="sitegroup-A-liveuser" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
  <sv:property sv:name="jcr:primaryType" sv:type="Name">
    <sv:value>hipposys:user</sv:value>
  </sv:property>
  <sv:property sv:name="hipposys:active" sv:type="Boolean">
    <sv:value>true</sv:value>
  </sv:property>
  <sv:property sv:name="hipposys:passkey" sv:type="String">
    <sv:value>jvm://</sv:value>
  </sv:property>
  <sv:property sv:name="hipposys:securityprovider" sv:type="String">
    <sv:value>internal</sv:value>
  </sv:property>
  <sv:property sv:name="hipposys:system" sv:type="Boolean">
    <sv:value>true</sv:value>
  </sv:property>
</sv:node>
```
* notice that this is a system user and the passkey is provided by the jvm!
#### Groups

Then add the membership of the user to the group as we do as usual:

```
<?xml version="1.0" encoding="UTF-8"?>
<sv:node sv:name="sitegroup-A-group" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
  <sv:property sv:name="jcr:primaryType" sv:type="Name">
    <sv:value>hipposys:group</sv:value>
  </sv:property>
  <sv:property sv:multiple="true" sv:name="hipposys:members" sv:type="String">
    <sv:value>sitegroup-A-liveuser</sv:value>
  </sv:property>
  <sv:property sv:name="hipposys:securityprovider" sv:type="String">
    <sv:value>internal</sv:value>
  </sv:property>
</sv:node>
```

#### Domains

Create any custom security domains and add the just defined group or user membership for it to work.

### HST
In the following example add 1 new session pool. For the case study we have implemented 16 of these custom session pools.

#### Custom Session Pools

Add new entries to the hst-config.properties file
```
sitegroupAliveuser.repository.address = vm://
sitegroupAliveuser.repository.user.name = sitegroup-A-liveuser
sitegroupAliveuser.repository.pool.name = sitegroupAliveuser
sitegroupAliveuser.repository.password =
```
Create a new xml file in META-INF/hst-assembly/overrides
```
<bean id="addSitegroupALiveuserJvmEnabled" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
 <property name="targetObject" ref="hstJmvEnabledUsers"/>
 <property name="targetMethod" value="add"/>
 <property name="arguments">
   <value>${sitegroupAliveuser.repository.user.name}</value>
 </property>
</bean>

….

<bean id="javax.jcr.Credentials.sitegroupAliveuser" class="org.hippoecm.hst.core.jcr.SimpleCredentialsFactoryBean"
     depends-on="addSitegroupALiveuserJvmEnabled">
 <property name="userId" value="${sitegroupAliveuser.repository.user.name}"/>
 <property name="separator" value="${repository.pool.user.name.separator}"/>
 <property name="poolName" value="${sitegroupAliveuser.repository.pool.name}"/>
 <property name="password" value="${sitegroupAliveuser.repository.password}"/>
 <property name="hstJmvEnabledUsers" ref="hstJmvEnabledUsers"/>
</bean>

….

<!---Abstract configuration, only needs to be added once:-->

<bean id="_abstractUserSessionPool" abstract="true" class="org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository"
     init-method="initialize" destroy-method="close">
 <!-- delegated JCR repository -->
 <property name="repositoryProviderClassName" value="${repositoryProviderClassName}"/>
 <property name="repositoryAddress" value="${default.repository.address}"/>
 <property name="defaultCredentialsUserIDSeparator" value="${repository.pool.user.name.separator}"/>
 <property name="hstJmvEnabledUsers" ref="hstJmvEnabledUsers"/>
 <!-- Pool properties. Refer to the GenericObjectPool of commons-pool library. -->
 <property name="maxActive" value="${default.repository.maxActive}"/>
 <property name="maxIdle" value="${default.repository.maxIdle}"/>
 <property name="minIdle" value="${default.repository.minIdle}"/>
 <property name="initialSize" value="${default.repository.initialSize}"/>
 <property name="maxWait" value="${default.repository.maxWait}"/>
 <property name="whenExhaustedAction" value="${default.repository.whenExhaustedAction}"/>
 <property name="testOnBorrow" value="${default.repository.testOnBorrow}"/>
 <property name="testOnReturn" value="${default.repository.testOnReturn}"/>
 <property name="testWhileIdle" value="${default.repository.testWhileIdle}"/>
 <property name="timeBetweenEvictionRunsMillis" value="${default.repository.timeBetweenEvictionRunsMillis}"/>
 <property name="numTestsPerEvictionRun" value="${default.repository.numTestsPerEvictionRun}"/>
 <property name="minEvictableIdleTimeMillis" value="${default.repository.minEvictableIdleTimeMillis}"/>
 <property name="refreshOnPassivate" value="${default.repository.refreshOnPassivate}"/>
 <property name="maxRefreshIntervalOnPassivate" value="${sessionPool.maxRefreshIntervalOnPassivate}"/>
 <property name="poolingCounter" ref="defaultPoolingCounter"/>
 <property name="maxTimeToLiveMillis" value="${default.repository.maxTimeToLiveMillis}"/>
</bean>

<bean id="_sitegroupAliveuserSessionPool" parent="_abstractUserSessionPool" class="org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository"
     init-method="initialize" destroy-method="close">
 <!-- delegated JCR repository -->
 <property name="defaultCredentialsUserID" value="${sitegroupAliveuser.repository.user.name}${repository.pool.user.name.separator}${sitegroupAliveuser.repository.pool.name}"/>
 <property name="defaultCredentialsPassword" value="${sitegroupAliveuser.repository.password}"/>
</bean>

<bean id="addSitegroupALiveuserSessionPool" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
 <property name="targetObject" ref="javax.jcr.Repository"/>
 <property name="targetMethod" value="addRepository"/>
 <property name="arguments">
   <list>
     <ref bean="javax.jcr.Credentials.sitegroupAliveuser"/>
     <ref bean="_sitegroupAliveuserSessionPool"/>
   </list>
 </property>
</bean>

<!--Only needs to be added once, depends-on is a comma separated file for each session pool:-->

<bean id="resourceLifecycleManagementList" class="org.springframework.beans.factory.config.ListFactoryBean"
     depends-on="addSitegroupALiveuserSessionPool">
 <property name="sourceList">
   <bean class="org.springframework.beans.factory.config.PropertyPathFactoryBean">
     <property name="targetBeanName" value="javax.jcr.Repository"/>
     <property name="propertyPath" value="resourceLifecycleManagements"/>
   </bean>
 </property>
</bean>
```

#### Authentication with Spring Security

In the solution of this case study we are using Spring Security to customize the authentication and access control within the project. Notice in de the demo project an example of Spring Security in action.

#### Custom Context Credentials Provider

Create an implementation of the ContextCredentialsProvider
```
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
```

Add to the same xml file as the configuration for the session pools and overwrite the default ContextCredentialsProvider:
```
<bean id="org.hippoecm.hst.core.request.ContextCredentialsProvider" class="org.onehippo.demo.security.CustomContextCredentialsProvider">
 <constructor-arg ref="javax.jcr.Credentials.default"/>
 <constructor-arg ref="javax.jcr.Credentials.sitegroupAliveuser"/>
...
 <constructor-arg ref="javax.jcr.Credentials.preview"/>
 <constructor-arg ref="javax.jcr.Credentials.writable"/>
</bean>
```
## Demo and Source






