import WikiSearchCard from './components/WikiSearchCard.vue';
import NoteFavoriteAction from '../notes/components/NoteFavoriteAction.vue';

const components = {
  'wiki-search-card': WikiSearchCard,
  'note-favorite-action': NoteFavoriteAction,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

// get override components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('WikiSearchCard');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}
