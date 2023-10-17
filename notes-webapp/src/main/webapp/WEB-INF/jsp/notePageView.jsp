<%@page import="org.exoplatform.commons.api.settings.ExoFeatureService"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<portlet:defineObjects />
<%
  String name = (String) request.getAttribute("settingName");
  boolean canEdit = (boolean) request.getAttribute("canEdit");
  String id = "NotePageView" + renderRequest.getWindowID();

  ExoFeatureService featureService = ExoContainerContext.getService(ExoFeatureService.class);
  String userName = request.getRemoteUser();
  boolean fullPageEditFeature = userName == null ? featureService.isActiveFeature("SNVFullPageAccess")
                                                 : featureService.isFeatureActiveForUser("SNVFullPageAccess", userName);
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application transparent v-application--is-ltr theme--light" flat=""
    id="<%=id%>">
    <script type="text/javascript">
      require(['PORTLET/notes/NotePageView'], app => app.init('<%=id%>', '<%=name%>', <%=canEdit%>, <%=fullPageEditFeature%>));
    </script>
  </div>
</div>