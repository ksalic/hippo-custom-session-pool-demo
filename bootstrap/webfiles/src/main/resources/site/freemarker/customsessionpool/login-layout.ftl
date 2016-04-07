<!doctype html>
<#include "../include/imports.ftl">
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <link rel="stylesheet" href="<@hst.webfile  path="/css/bootstrap.css"/>" type="text/css"/>
    <@hst.defineObjects/>
    <#if hstRequest.requestContext.cmsRequest>
      <link rel="stylesheet" href="<@hst.webfile  path="/css/cms-request.css"/>" type="text/css"/>
    </#if>
<@hst.headContributions categoryExcludes="htmlBodyEnd, scripts" xhtml=true/>
</head>
<body>
<div class="container">
  <form class="form-signin" action="<@hst.link path='/'/>login" method="post">
    <h2 class="form-signin-heading">Please sign in</h2>

    <label for="inputEmail" class="sr-only">Username</label>
    <input type="text" id="inputUsername" class="form-control" placeholder="Username" required="" autofocus="" name="username">

    <label for="inputPassword" class="sr-only">Password</label>
    <input type="password" id="inputPassword" class="form-control" placeholder="Password" required="" name="password">


    <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
  </form>
</div>
<@hst.headContributions categoryIncludes="htmlBodyEnd, scripts" xhtml=true/>
</body>
</html>