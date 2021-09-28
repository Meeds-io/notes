import { notesConstants } from './notesConstants.js';

export function getNotes(noteBookType, noteBookOwner, noteId,source) {
  let url = `${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${noteBookType}/${noteBookOwner}/${noteId}`;
  if (source){
    url=`${url}?source=${source}`;
  }
  return fetch(url, {
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

export function getNoteById(noteId,source,type, owner) {
  let url = `${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/${noteId}`;
  if (source){
    url=`${url}${getSeparator(url)}source=${source}`;
  } if (type){
    url=`${url}${getSeparator(url)}noteBookType=${type}`;
  } if (owner){
    url=`${url}${getSeparator(url)}noteBookOwner=${owner}`;
  }
  return fetch(url, {
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

export function getSeparator(url) {
  return url.indexOf('?') !== -1 ? '&' : '?';
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
    if (!resp || !resp.ok) {
      if (resp.status===409) {
        throw new Error('error.duplicate.title', resp);
      } else {
        throw new Error('error', resp);
      }
    } else {
      return resp.json();    }
  });
}

export function saveDraftNote(draftNote, parentPageId) {
  if (parentPageId) {
    draftNote.parentPageId = parentPageId;
  }
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/saveDraft`, {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    method: 'POST',
    credentials: 'include',
    body: JSON.stringify(draftNote)
  }).then((resp) => {
    if (!resp || !resp.ok) {
      if (resp.status === 409) {
        throw new Error('error.duplicate.title', resp);
      } else {
        throw new Error('error', resp);
      }
    } else {
      return resp.json();
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
      if (resp.status===409){
        throw new Error('error.duplicate.title', resp);
      } else {
        throw new Error('error', resp);
      }
    } else {
      return resp.json();
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
      if (resp.status===409){
        throw new Error('error.duplicate.title', resp);
      } else {
        throw new Error('error', resp);
      }
    } else {
      return resp.json();
    }
  });
}

export function restoreNoteVersion(note,version) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/restore/${version}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(note),
  }).then(resp => {
    if (!resp || !resp.ok) {
      if (resp.status===409){
        throw new Error('error.duplicate.title', resp);
      } else {
        throw new Error('error', resp);
      }
    } else {
      return resp.json();
    }
  });
}

export function getPathByNoteOwner(note,noteAppName) {
  if (!noteAppName){
    noteAppName = 'notes';
  }
  if (note.wikiType==='group'){
    const spaceName = note.wikiOwner.split('/spaces/')[1];
    return `${eXo.env.portal.context}/g/:spaces:${spaceName}/${spaceName}/${noteAppName}/${note.id}`;
  } else {
    return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes/${note.id}`;
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

export function deleteDraftNote(note) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/draftNote/${note.id}`, {
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

export function moveNotes(note,destination) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/note/move/${note.id}/${destination.id}`, {
    credentials: 'include',
    method: 'PATCH',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp;
    } else {
      throw new Error('Error when deleting notes from label');
    }
  });
}

export function getNoteVersionsByNoteId(noteId) {
  return fetch(`${notesConstants.PORTAL}/${notesConstants.PORTAL_REST}/notes/versions/${noteId}`, {
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
