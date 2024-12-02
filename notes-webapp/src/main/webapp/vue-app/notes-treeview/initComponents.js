import NoteBreadcrumb from '../notes/components/NoteBreadcrumb.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';

const components = {
  'note-treeview-drawer': NoteTreeviewDrawer,
  'note-breadcrumb': NoteBreadcrumb
};

for (const key in components) {
  Vue.component(key, components[key]);
}
