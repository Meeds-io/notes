import './initComponents.js';

export function formatSearchResult(results,term) {
  if (results && results.jsonList && results.jsonList.length) {
    results = results.jsonList.map(note => {
      note.summary = note.summary.replace(new RegExp(`(${term})`, 'ig'), '<span class="searchMatchExcerpt">$1</span>');
      note.excerpt = note.excerpt.replace(new RegExp(`(${term})`, 'ig'), '<span class="searchMatchExcerpt">$1</span>');
      return note;
    });
  }
  return results;
}
