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

export function getNoteTree(noteBookType, noteBookOwner, noteId) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/wiki/tree/ALL?path=${noteBookType}/${noteBookOwner}/${noteId}`, {
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

export function addNote(page) {
  return fetch(`/${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/${page}`, {
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