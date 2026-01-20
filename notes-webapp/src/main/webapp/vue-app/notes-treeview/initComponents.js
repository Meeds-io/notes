import NoteBreadcrumb from '../notes/components/NoteBreadcrumb.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NoteTreeViewSideBar from './components/NoteTreeViewSideBar.vue';
import NoteTreeViewSideBarItemLabel from './components/NoteTreeViewSideBarItemLabel.vue';
import NoteTreeViewSideBarItemPrepend from './components/NoteTreeViewSideBarItemPrepend.vue';
import NoteTreeviewToolbar from './components/NoteTreeviewToolbar.vue';
import NoteTreeViewFilterDrawer from './components/NoteTreeViewFilterDrawer.vue';

const components = {
  'note-treeview-drawer': NoteTreeviewDrawer,
  'note-treeview-sideBar': NoteTreeViewSideBar,
  'note-treeview-sidebar-item-label': NoteTreeViewSideBarItemLabel,
  'note-treeview-sidebar-item-prepend': NoteTreeViewSideBarItemPrepend,
  'note-treeview-toolbar': NoteTreeviewToolbar,
  'note-treeview-filter-drawer': NoteTreeViewFilterDrawer,
  'note-breadcrumb': NoteBreadcrumb
};

for (const key in components) {
  Vue.component(key, components[key]);
}
