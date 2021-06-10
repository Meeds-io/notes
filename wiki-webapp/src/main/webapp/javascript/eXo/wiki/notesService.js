import { notesConstants } from './notesConstants.js';

export function getNotes(noteBookType, noteBookOwner, noteId) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${noteBookType}/${noteBookOwner}/${noteId}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    } else {
      return resp.json();
    }
  });
} 

export function getNoteById(noteId) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${noteId}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    } else {
      return resp.json();
    }
  });
} 

export function getNoteTree(noteBookType, noteBookOwner, noteId,treeType) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/wiki/tree/${treeType}?path=${noteBookType}/${noteBookOwner}/${noteId}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    } else {
      return resp.json();
    }
  });
}

export function createNote(page) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note`, {
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

export function updateNote(note) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${note.wikiType}/${note.wikiOwner}/${note.name}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'PUT',
    body: JSON.stringify(note)
  }).then(resp => {
    if (!resp || !resp.ok) {
      return resp.text().then((text) => {
        throw new Error(text);
      });
    } else {
      return resp;
    }
  });
}

export function updateNoteById(note) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${note.id}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(note),
  }).then(resp => {
    if (!resp || !resp.ok) {
      return resp.text().then((text) => {
        throw new Error(text);
      });
    } else {
      return resp;
    }
  });
}
export function getPathByNoteOwner(note) {
  if (note.wikiType==='group'){
    const spaceName = note.wikiOwner.split('/spaces/')[1];
    return `${eXo.env.portal.context}/g/:spaces:${spaceName}/${spaceName}/wiki/${note.name}`;
  } else {
    return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes/${note.name}`;
  }
}

export function deleteNotes(note) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${note.id}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp;
    } else {
      throw new Error('Error when deleting notes from label');
    }
  });
}

