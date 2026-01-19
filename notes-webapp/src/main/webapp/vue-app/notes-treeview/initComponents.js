import NoteBreadcrumb from '../notes/components/NoteBreadcrumb.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NoteTreeViewSideBar from './components/NoteTreeViewSideBar.vue';

const components = {
  'note-treeview-drawer': NoteTreeviewDrawer,
  'note-treeview-sideBar': NoteTreeViewSideBar,
  'note-breadcrumb': NoteBreadcrumb
};

for (const key in components) {
  Vue.component(key, components[key]);
}
