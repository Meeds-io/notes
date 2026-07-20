/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
export function getContentToSave(ckEditorInstanceId, oembedMinWidth) {
  const domParser = new DOMParser();
  const newData = CKEDITOR.instances[ckEditorInstanceId].getData();
  const body = CKEDITOR.instances[ckEditorInstanceId].document.getBody().$;
  const documentElement = domParser.parseFromString(newData, 'text/html').documentElement;
  preserveEmbedded(body, documentElement, oembedMinWidth);
  preserveHighlightedCode(body, documentElement);
  return documentElement?.children[1].innerHTML;
}

export function getContentToEdit(content) {
  const domParser = new DOMParser();
  const docElement = domParser.parseFromString(content, 'text/html').documentElement;
  restoreOembed(docElement);
  restoreUnHighlightedCode(docElement);
  return docElement?.children[1].innerHTML;
}

// Accessibility (#4246): expose emojis to screen readers by wrapping
// them in a <span role="img" aria-label="..."> once their names are loaded.
let emojiNameByChar = null;
let emojiPattern = null;

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function buildEmojiIndex(emojiBank) {
  const map = new Map();
  (emojiBank.categories || []).forEach(category => {
    (category.emojis || []).forEach(item => {
      if (item.emoji && item.name && !map.has(item.emoji)) {
        map.set(item.emoji, item.name);
      }
    });
  });
  emojiNameByChar = map;
  const chars = Array.from(map.keys()).sort((a, b) => b.length - a.length);
  emojiPattern = chars.length && new RegExp(chars.map(escapeRegExp).join('|'), 'g');
}

fetch('/social/json/emojiBank.json?v=1')
  .then(resp => resp.ok && resp.json())
  .then(emojiBank => emojiBank && buildEmojiIndex(emojiBank))
  .catch(() => {
    // Emoji names not available yet: rendered emojis will remain unlabeled until a later call.
  });

function addAccessibleNameToEmojis(docElement) {
  if (!emojiPattern) {
    return;
  }
  const body = docElement.getElementsByTagName('body')[0];
  const walker = document.createTreeWalker(body, NodeFilter.SHOW_TEXT);
  const textNodes = [];
  let node;
  while ((node = walker.nextNode())) {
    emojiPattern.lastIndex = 0;
    if (node.parentElement?.getAttribute('role') !== 'img' && emojiPattern.test(node.nodeValue)) {
      textNodes.push(node);
    }
  }
  textNodes.forEach(textNode => {
    const fragment = docElement.ownerDocument.createDocumentFragment();
    let lastIndex = 0;
    let match;
    emojiPattern.lastIndex = 0;
    while ((match = emojiPattern.exec(textNode.nodeValue))) {
      if (match.index > lastIndex) {
        fragment.appendChild(docElement.ownerDocument.createTextNode(textNode.nodeValue.slice(lastIndex, match.index)));
      }
      const span = docElement.ownerDocument.createElement('span');
      span.setAttribute('role', 'img');
      span.setAttribute('aria-label', emojiNameByChar.get(match[0]));
      span.textContent = match[0];
      fragment.appendChild(span);
      lastIndex = match.index + match[0].length;
    }
    fragment.appendChild(docElement.ownerDocument.createTextNode(textNode.nodeValue.slice(lastIndex)));
    textNode.parentNode.replaceChild(fragment, textNode);
  });
}

export function getContentToDisplay(content, noteId, noteBookType, noteBookOwner, computeNavigation) {
  const internal = window.location.host + eXo.env.portal.context;
  const domParser = new DOMParser();
  const docElement = domParser.parseFromString(content, 'text/html').documentElement;
  const contentChildren = docElement.getElementsByTagName('body')[0].children;
  const links = docElement.getElementsByTagName('a');
  const tables = docElement.getElementsByTagName('table');
  for (const link of links) {
    let href = link.href.replace(/(^\w+:|^)\/\//, '');
    if (href.endsWith('/')) {
      href = href.slice(0, -1);
    }
    if (href !== window.location.host && !href.startsWith(internal)) {
      link.setAttribute('rel', 'noopener noreferrer');
    }
  }
  for (const table of tables) {
    if (!table.hasAttribute('summary') || table?.summary?.trim().length) {
      const customId = table.parentElement.id.split('-').pop();
      const tableSummary = document.getElementById(`summary-${customId}`);
      if ( tableSummary !== null && tableSummary.innerText.trim().length) {
        table.setAttribute('summary', tableSummary.innerText);
      } else {
        table.removeAttribute('summary');
      }
    }
  }
  if (contentChildren && computeNavigation) {
    for (let i = 0; i < contentChildren.length; i++) { // NOSONAR not iterable
      const child = contentChildren[i];
      if (child.classList.value.includes('navigation-img-wrapper')) {
        // Props object
        const componentProps = {
          noteId: noteId,
          source: '',
          noteBookType: noteBookType,
          noteBookOwner: noteBookOwner,
        };
        contentChildren[i].innerHTML = `<component v-bind:is="vTreeComponent" note-id="${componentProps.noteId}" note-book-type="${componentProps.noteBookType}" note-book-owner="${componentProps.noteBookOwner}"></component>`;
      }
    }
  }
  addAccessibleNameToEmojis(docElement);
  return docElement?.children[1].innerHTML;
}

function restoreUnHighlightedCode(documentElement) {
  documentElement.querySelectorAll('code.hljs').forEach(code => {
    code.innerHTML = code.innerText.replace(/</g, '&lt;').replace(/>/g, '&gt;');
    code.classList.remove('hljs');
  });
}

function restoreOembed(documentElement) {
  documentElement.querySelectorAll('div.embed-wrapper').forEach(wrapper => {
    const oembed = document.createElement('oembed');
    oembed.innerHTML = wrapper.dataset.url;
    wrapper.replaceWith(oembed);
  });
}

function preserveEmbedded(body, documentElement, oembedMinWidth) {
  const iframes = body.querySelectorAll('[data-widget="embedSemantic"] div iframe');
  if (iframes.length) {
    documentElement.querySelectorAll('oembed').forEach((oembed, index) => {
      const wrapper = document.createElement('div');
      wrapper.dataset.url = decodeURIComponent(oembed.innerHTML);
      wrapper.innerHTML = iframes[index]?.parentNode?.innerHTML;
      const width = iframes[index]?.parentNode?.offsetWidth;
      const height = iframes[index]?.parentNode?.offsetHeight;
      const aspectRatio = width / height;
      const minHeight = parseInt(oembedMinWidth) / aspectRatio;
      const style = `
        min-height: ${minHeight}px;
        min-width: ${oembedMinWidth}px;
        width: 100%;
        margin-bottom: 10px;
        aspect-ratio: ${aspectRatio};
      `;
      wrapper.setAttribute('style', style);
      wrapper.setAttribute('class', 'embed-wrapper d-flex position-relative ml-auto mr-auto');
      oembed.replaceWith(wrapper);
    });
  }
}

function preserveHighlightedCode(body, documentElement) {
  const codes = body.querySelectorAll('pre[data-widget="codeSnippet"] code');
  if (codes.length) {
    documentElement.querySelectorAll('code').forEach((code, index) => {
      code.innerHTML = codes[index]?.innerHTML;
      code.setAttribute('class', codes[index]?.getAttribute('class'));
    });
  }
}
export function isSameContent(content, originalContent) {
  // check if content composed only by text and then compare only the text content
  const containOnlyText = Array.from(new DOMParser().parseFromString(content, 'text/html').body.childNodes).every(node => (node.nodeName === 'P' && node.textContent.trim()) || node.nodeType === Node.TEXT_NODE)
      && Array.from(new DOMParser().parseFromString(originalContent, 'text/html').body.childNodes).every(node => (node.nodeName === 'P' && node.textContent.trim()) || node.nodeType === Node.TEXT_NODE);
  if (containOnlyText) {
    return getString(content) === getString(originalContent);
  }
  // get nodes and remove all empty paragraph elements then compare.
  const originalContentNode = Array.from(new DOMParser().parseFromString(originalContent, 'text/html').body.childNodes).filter(node => !(node.nodeName === 'P' && !node.textContent.trim() && !node.children.length > 0));
  let index = originalContentNode.length - 1;
  //remove all empty text nodes at the end.
  while (index >= 0 && originalContentNode[index].nodeType === Node.TEXT_NODE && !originalContentNode[index].textContent.trim() ) {
    originalContentNode.pop();
    index--;
  }
  const currentContentNode = Array.from(new DOMParser().parseFromString(content, 'text/html').body.childNodes).filter(node => !(node.nodeName === 'P' && !node.textContent.trim() && !node.children.length > 0));
  index = currentContentNode.length - 1;
  while (index >= 0 && currentContentNode[index].nodeType === Node.TEXT_NODE && !currentContentNode[index].textContent.trim() ) {
    currentContentNode.pop();
    index--;
  }
  // isEqualNode : Two nodes are equal when they have the same type and the same content.
  return  originalContentNode.length === currentContentNode.length && originalContentNode.every((node, index) => node.isEqualNode(currentContentNode[index]));
}
function getString(body) {
  return new DOMParser().parseFromString(body, 'text/html').documentElement.textContent.replace(/&nbsp;/g, '').trim();
}
