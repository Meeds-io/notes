/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.webui.control.action;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.commons.WikiConstants;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.WikiPageHistory;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.webui.*;
import org.exoplatform.wiki.webui.control.filter.IsEditAddModeFilter;
import org.exoplatform.wiki.webui.control.filter.IsEditAddPageModeFilter;
import org.exoplatform.wiki.webui.control.listener.UISubmitToolBarActionListener;
import org.xwiki.rendering.syntax.Syntax;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ComponentConfig(
  template = "app:/templates/wiki/webui/control/action/SavePageActionComponent.gtmpl",
  events = {
    @EventConfig(listeners = SavePageActionComponent.SavePageActionListener.class, phase = Phase.DECODE)
  }
)
public class SavePageActionComponent extends UIComponent {

  public static final String                   ACTION   = "SavePage";

  private static final Log log = ExoLogger.getLogger("wiki:SavePageActionComponent");

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsEditAddModeFilter(), new IsEditAddPageModeFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  protected boolean isNewMode() {
    return (WikiMode.ADDPAGE.equals(getAncestorOfType(UIWikiPortlet.class).getWikiMode()));
  }

  protected String getPageTitleInputId() {
    return UIWikiPageTitleControlArea.FIELD_TITLEINPUT;
  }

  protected String getActionLink() throws Exception {
    return Utils.createFormActionLink(this, ACTION, ACTION);
  }

  public static class SavePageActionListener extends
                                            UISubmitToolBarActionListener<SavePageActionComponent> {
    @Override
    protected void processEvent(Event<SavePageActionComponent> event) throws Exception {
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      Page page = Utils.getCurrentWikiPage();
      if (page != null) {
        UIWikiPageTitleControlArea pageTitleControlForm = wikiPortlet.findComponentById(UIWikiPageControlArea.TITLE_CONTROL);
        UIWikiPageEditForm pageEditForm = wikiPortlet.findFirstComponentOfType(UIWikiPageEditForm.class);
        UIWikiRichTextArea wikiRichTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
        UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class).getUIStringInput();
        UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
        UIFormStringInput commentInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_COMMENT);
        String syntaxId = Utils.getDefaultSyntax();
        RenderingService renderingService = (RenderingService) PortalContainer.getComponent(RenderingService.class);
        Utils.setUpWikiContext(wikiPortlet);
        String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
        boolean isRenamedPage = false;
        boolean isContentChange = false;
  
        if (wikiPortlet.getWikiMode() == WikiMode.ADDPAGE && 
            (titleInput.getValue() == null || titleInput.getValue().isEmpty())){
          // Add a new page with empty title, set title value to Untitled
          WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
          ResourceBundle res = context.getApplicationResourceBundle();
          titleInput.setValue(res.getString("UIWikiPageTitleControlArea.label.Untitled"));
        }
        
        String title = titleInput.getValue().trim();
        if(StringUtils.isEmpty(title)){
          event.getRequestContext().getUIApplication()
                  .addMessage(new ApplicationMessage("WikiPageNameValidator.msg.EmptyTitle", null, ApplicationMessage.WARNING));
          Utils.redirect(Utils.getCurrentWikiPageParams(), WikiMode.EDITPAGE);
          return;
        } else if (title.length() > WikiConstants.MAX_LENGTH_TITLE) {
          event.getRequestContext().getUIApplication()
                  .addMessage(new ApplicationMessage("WikiPageNameValidator.msg.TooLongTitle", new Object[] {WikiConstants.MAX_LENGTH_TITLE} , ApplicationMessage.WARNING));
          Utils.redirect(Utils.getCurrentWikiPageParams(), WikiMode.EDITPAGE);
          return;
        }
        if (wikiRichTextArea.isRendered()) {
          String htmlContent = wikiRichTextArea.getUIFormTextAreaInput().getValue();
          String markupContent = renderingService.render(htmlContent,
                                                         Syntax.XHTML_1_0.toIdString(),
                                                         syntaxId,
                                                         false);
          markupInput.setValue(markupContent);
        }
        String markup = (markupInput.getValue() == null) ? "" : markupInput.getValue();
        markup = markup.trim();
  
        String newPageName = TitleResolver.getId(title, false);
        if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(page.getName()) && wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
          // as wiki home page has fixed name (never edited anymore), every title changing is accepted.
          ;
        } else if (newPageName.equals(page.getName()) && wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
          // if page title is not changed in editing phase, do not need to check its existence.
          ;
        } else if (wikiService.isExisting(pageParams.getType(), pageParams.getOwner(), newPageName)) {
          // if new page title is duplicated with existed page's.
          if (log.isDebugEnabled()) log.debug("The title '" + title + "' is already existing!");
          event.getRequestContext()
               .getUIApplication()
               .addMessage(new ApplicationMessage("SavePageAction.msg.warning-page-title-already-exist",
                                                  null,
                                                  ApplicationMessage.WARNING));
          if (wikiRichTextArea.isRendered()) {
            SavePageActionComponent component = event.getSource();
            UIWikiPageEditForm wikiPageEditForm = component.getAncestorOfType(UIWikiPageEditForm.class);
            Utils.feedDataForWYSIWYGEditor(wikiPageEditForm,null);
          }
          Utils.redirect(pageParams, wikiPortlet.getWikiMode());
          return;
        }
        pageParams.setPageId(newPageName);
        try {
          if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
            if (wikiPortlet.getEditMode() == EditMode.SECTION) {
              newPageName = page.getName();
              title = page.getTitle();
              markup = renderingService.updateContentOfSection(page.getContent(),
                                                               page.getSyntax(),
                                                               wikiPortlet.getSectionIndex(),
                                                               markup);
              isContentChange = true;
            }
            
            // Check if publish activity on activity stream
            UICheckBoxInput publishActivityCheckBox = wikiPortlet.findComponentById(UIWikiPageEditForm.FIELD_PUBLISH_ACTIVITY_UPPER);
            page.setMinorEdit(!publishActivityCheckBox.isChecked());
            pageEditForm.synPublishActivityStatus(false);
            
            // Check if the title is change or not
            if (!page.getTitle().equals(title)) {
              isRenamedPage = true;
            }
            
            // Rename page if need
            if (!page.getName().equals(newPageName)) {
              wikiService.renamePage(pageParams.getType(),
                                     pageParams.getOwner(),
                                     page.getName(),
                                     newPageName,
                                     title);
              if(!WikiNodeType.Definition.WIKI_HOME_NAME.equals(page.getName())) {
                page.setName(newPageName);
              }
            }
  
            synchronized (page.getId()) {
              page.setComment(commentInput.getValue());
              page.setSyntax(syntaxId);
              pageTitleControlForm.getUIFormInputInfo().setValue(title);
              pageParams.setPageId(page.getName());
              page.setUrl(Utils.getURLFromParams(pageParams));
              
              if (!page.getContent().equals(markup)) {
                page.setContent(markup);
                isContentChange = true;
              }

              if (!pageEditForm.getTitle().equals(title)) {
                page.setTitle(title);
                pageParams.setPageId(newPageName);
              }

              // update the page and create a version
              wikiService.updatePage(page);
              wikiService.createVersionOfPage(page);

              if (!"__anonim".equals(currentUser)) {
                wikiService.removeDraftOfPage(pageParams);
              }
             }
            
            // Post edit content activity
            if (isRenamedPage && isContentChange) {
              wikiService.postUpdatePage(pageParams.getType(), pageParams.getOwner(), pageParams.getPageId(), page, PageWikiListener.EDIT_PAGE_CONTENT_AND_TITLE_TYPE);
            } else if (isRenamedPage) {
              wikiService.postUpdatePage(pageParams.getType(), pageParams.getOwner(), pageParams.getPageId(), page, PageWikiListener.EDIT_PAGE_TITLE_TYPE);
            } else if (isContentChange) {
              wikiService.postUpdatePage(pageParams.getType(), pageParams.getOwner(), pageParams.getPageId(), page, PageWikiListener.EDIT_PAGE_CONTENT_TYPE);
            }
          } else if (wikiPortlet.getWikiMode() == WikiMode.ADDPAGE) {
            Page draftPage = Utils.getCurrentNewDraftWikiPage();
            // TODO attachments
            //Collection<Attachment> attachs = draftPage.getAttachments();
            Wiki wiki = new Wiki(pageParams.getType(), pageParams.getOwner());
            Page newPage = new Page();
            newPage.setName(title);
            newPage.setTitle(title);
            newPage.setContent(markup);
            newPage.setSyntax(syntaxId);
            Page createdPage = wikiService.createPage(wiki, page.getName(), newPage);
            pageParams.setPageId(newPageName);
            createdPage.setUrl(Utils.getURLFromParams(pageParams));
            // TODO attachments
            //createdPage.getAttachments().addAll(attachs);
            // TODO use wikiService
            /*
            UpdateAttachmentMixin updateAttachment = createdPage.createUpdateAttachmentMixin();
            createdPage.setUpdateAttachmentMixin(updateAttachment);
            createdPage.checkin();
            createdPage.checkout();
            draftPage.remove();
            */
            // remove the draft for new page
            Page parentPage = wikiService.getParentPageOf(createdPage);
            DraftPage contentDraftPage = findTheMatchDraft(title, parentPage);
            if (contentDraftPage == null) {
              Map<String, WikiPageHistory> pageLogs = org.exoplatform.wiki.utils.Utils.getLogOfPage(parentPage.getName());
              WikiPageHistory log = pageLogs.get(currentUser);
              if ((log != null) && log.isNewPage()) {
                wikiService.removeDraft(log.getDraftName());
              }
            } else {
              // TODO use wikiService
              //contentDraftPage.remove();
            }
            
            // remove log edit page
            
            // Post add activity
            wikiService.postAddPage(pageParams.getType(), pageParams.getOwner(), pageParams.getPageId(), createdPage);
          }
          org.exoplatform.wiki.utils.Utils.removeLogEditPage(pageParams, currentUser);
        } catch (Exception e) {
          log.error("An exception happens when saving the page with title:" + title, e);
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIPageToolBar.msg.Exception", null, ApplicationMessage.ERROR));
        } finally {
          wikiPortlet.changeMode(WikiMode.VIEW);
          Utils.redirect(pageParams, WikiMode.VIEW);
        }
      } else {
        wikiPortlet.changeMode(WikiMode.VIEW);
        Utils.redirect(pageParams, WikiMode.VIEW);
      }
    }

    private DraftPage findTheMatchDraft(String pageTitle, Page parentPage) throws Exception {
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      String parentUUID = parentPage.getId();
      String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
      List<DraftPage> draftPages = wikiService.getDraftsOfUser(currentUser);
      for (DraftPage draftPage : draftPages) {
        if (draftPage.getTitle() != null && draftPage.getTitle().equals(pageTitle)
                && draftPage.getTargetPage() != null && draftPage.getTargetPage().equals(parentUUID)) {
          return draftPage;
        }
      } 
      return null;
    }
  }
}
