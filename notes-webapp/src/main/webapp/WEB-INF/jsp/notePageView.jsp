<%@page import="org.exoplatform.commons.api.settings.ExoFeatureService"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<portlet:defineObjects />
<%
  String name = (String) request.getAttribute("settingName");
  boolean canEdit = (boolean) request.getAttribute("canEdit");
  boolean allowHide = Boolean.TRUE.equals(request.getAttribute("allowHide"));
  boolean hidden = Boolean.TRUE.equals(request.getAttribute("hidden"));
  String id = "NotePageView" + renderRequest.getWindowID();
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application transparent v-application--is-ltr theme--light" flat=""
    id="<%=id%>">
    <script type="text/javascript">
      require(['PORTLET/notes/NotePageView'], app => app.init('<%=id%>', '<%=name%>', <%=canEdit%>, <%=allowHide%>, <%=hidden%>));
    </script>
  </div>
</div>