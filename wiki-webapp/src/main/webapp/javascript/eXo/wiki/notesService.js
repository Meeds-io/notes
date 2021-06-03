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