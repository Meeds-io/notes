/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * Creates the creation of a new note, handed to the Properties drawer's
 * sections: each section registers what it checks before the note is
 * created, what it writes once the note exists, and what it gives back when
 * the note is dropped. A check never blocks the note's creation. Sealed while
 * the note is being created, the creation writes what the sections registered
 * when the note was posted, and applies a section leaving, or the creation's
 * giving back, only once unsealed.
 *
 * @returns {object} the creation: register, validate, commit, seal, unseal and dispose
 */
export function newNoteCreation() {
  const hooks = new Set();
  // A hook runs inside the chain, so that its own throw, synchronous or not, never escapes: a check that throws passes, a write that throws reports nothing unsaved
  const run = (call, fallback) => Promise.resolve().then(call).catch(() => fallback);
  const giveBack = hook => {
    try {
      hook.dispose?.();
    } catch (e) {
      // a section failing to give back what it holds never stops the others
    }
  };
  const leaving = new Set();
  let committed = null;
  let disposeAsked = false;
  const disposeAll = () => {
    hooks.forEach(giveBack);
    hooks.clear();
  };
  return {
    register: hook => {
      hooks.add(hook);
      return () => {
        if (committed) {
          leaving.add(hook);
        } else {
          hooks.delete(hook);
        }
      };
    },
    validate: () => Promise.all([...hooks].map(hook => run(() => (hook.validate ? hook.validate() : true), true)))
      .then(answers => answers.every(Boolean)),
    commit: noteId => Promise.all([...(committed || hooks)].map(hook => run(() => (hook.commit ? hook.commit(noteId) : []), [])))
      .then(notSaved => notSaved.flat().filter(Boolean)),
    seal: () => {
      committed = new Set(hooks);
    },
    unseal: () => {
      committed = null;
      // A section that left while the note was created is given back now: nothing else will
      leaving.forEach(hook => {
        giveBack(hook);
        hooks.delete(hook);
      });
      leaving.clear();
      if (disposeAsked) {
        disposeAsked = false;
        disposeAll();
      }
    },
    dispose: () => {
      if (committed) {
        disposeAsked = true;
      } else {
        disposeAll();
      }
    },
  };
}
