import NoteBreadcrumb from '../notes/components/NoteBreadcrumb.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NoteTreeViewSideBar from './components/NoteTreeViewSideBar.vue';
import NoteTreeViewSideBarItemLabel from './components/NoteTreeViewSideBarItemLabel.vue';
import NoteTreeViewSideBarItemPrepend from './components/NoteTreeViewSideBarItemPrepend.vue';

const components = {
  'note-treeview-drawer': NoteTreeviewDrawer,
  'note-treeview-sideBar': NoteTreeViewSideBar,
  'note-treeview-sidebar-item-label': NoteTreeViewSideBarItemLabel,
  'note-treeview-sidebar-item-prepend': NoteTreeViewSideBarItemPrepend,
  'note-breadcrumb': NoteBreadcrumb
};

for (const key in components) {
  Vue.component(key, components[key]);
}
