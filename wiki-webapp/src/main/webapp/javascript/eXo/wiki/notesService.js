
export function addNote(page) {
  return fetch('/portal/rest/notes/page', {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    method: 'POST',
    credentials: 'include',
    body: JSON.stringify(page)
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error when adding note page');
    }
  });
}