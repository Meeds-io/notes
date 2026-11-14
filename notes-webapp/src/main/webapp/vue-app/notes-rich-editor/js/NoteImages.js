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
 * Selects, among the images a note shows, those its featured image may be
 * picked from: the images this platform serves through its REST API, as the
 * note's uploads (`/portal/rest/v1/social/attachments/...`) and the documents
 * picker's images (`/portal/rest/images/repository/...`) are. An image from
 * another site, an inline `data:` image, an upload still shown from its
 * `blob:` URL until the draft save gives it its own, and the platform's
 * static files (the editor's smileys) are left out. Each image is kept once.
 *
 * @param {Array<{src: string, alt: string}>} images the note's images, in the
 *          order the note shows them, with their `src` attribute as written
 * @param {string} origin the page's origin, against which a relative `src`
 *          resolves and to which a picked image must belong
 * @returns {Array<{src: string, alt: string}>} the images that can be picked,
 *          each with its absolute URL and its alternative text
 */
export function selectPickableImages(images, origin) {
  const pickable = [];
  const seen = new Set();
  (images || []).forEach(image => {
    const url = toUrl(image?.src, origin);
    if (url
        && url.origin === origin
        && url.pathname.split('/').includes('rest')
        && !seen.has(url.href)) {
      seen.add(url.href);
      pickable.push({
        src: url.href,
        alt: image.alt || '',
      });
    }
  });
  return pickable;
}

/**
 * Lists the images of a note's HTML content, in the order it shows them.
 *
 * @param {string} content the note's HTML content
 * @returns {Array<{src: string, alt: string}>} each image's `src` and `alt`
 *          attributes as written; none for an empty content
 */
export function listContentImages(content) {
  if (!content) {
    return [];
  }
  const doc = new DOMParser().parseFromString(content, 'text/html');
  return Array.from(doc.querySelectorAll('img[src]'))
    .map(img => ({
      src: img.getAttribute('src'),
      alt: img.getAttribute('alt') || '',
    }));
}

/**
 * Parses an image's `src` against the page's origin.
 *
 * @param {string} src the `src` attribute, absolute or relative
 * @param {string} origin the page's origin
 * @returns {URL|null} the parsed URL; null for an empty or unparsable `src`
 */
function toUrl(src, origin) {
  if (!src?.trim()) {
    return null;
  }
  try {
    return new URL(src.trim(), origin);
  } catch (e) {
    return null;
  }
}
