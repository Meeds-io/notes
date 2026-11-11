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
const SCROLLABLE_TABLE_WRAPPER_CLASS = 'rich-content-table-wrapper';

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
  // After the summary loop, which reads each table's original parent
  wrapTablesInScrollableBox(docElement);
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
  return docElement?.children[1].innerHTML;
}

export function getContentWithScrollableTables(content) {
  if (!content) {
    return content;
  }
  const docElement = new DOMParser().parseFromString(content, 'text/html').documentElement;
  wrapTablesInScrollableBox(docElement);
  return docElement?.children[1].innerHTML;
}

/*
 * A table cannot be narrower than its content (its longest words, its images,
 * its cells' widths), and on WebKit it also keeps the width its author gave
 * it, so on a narrow screen it can be wider than the content: each top-level
 * table gets its own horizontal scroll. Only an inline width counts: the
 * platform's styles give every table a width of 100%, which overrides a width
 * attribute.
 * A table aligned left or right is floated: its box takes that float, to keep
 * the text flowing beside it, and the table stops floating inside the box,
 * since a float on the end side (right in a left-to-right text) wider than the
 * box would overflow its start side, where a scroll container cannot scroll.
 * Its percentage width (100% when it has none) moves to the box, since it
 * would otherwise resolve against the box: the table is sized to the box, and
 * scrolls inside it when its content is wider than that share, even on a wide
 * screen. A fixed width stays on the table, which the box fits up to the
 * column's width.
 * Any other box would, beside a float, shrink into the space left, down to
 * nothing. Around a table of fixed width that no auto margin centres or pushes
 * aside, it fits the table, so it sits beside a float when the table fits
 * there and below it otherwise, as the table did. Every other box takes the
 * full width, which keeps an auto margin's effect on its table: it always
 * starts below an earlier float, even when its table would fit beside it.
 */
function wrapTablesInScrollableBox(documentElement) {
  documentElement.querySelectorAll('table').forEach(table => {
    const parent = table.parentElement;
    if (parent.closest('table') || parent.classList.contains(SCROLLABLE_TABLE_WRAPPER_CLASS)) {
      return;
    }
    const wrapper = documentElement.ownerDocument.createElement('div');
    wrapper.classList.add(SCROLLABLE_TABLE_WRAPPER_CLASS, 'overflow-x-auto', 'max-width-fit');
    const float = (table.getAttribute('align') || table.style.cssFloat || '').trim().toLowerCase();
    const width = table.style.width.trim();
    const hasFixedWidth = !!width && !width.endsWith('%');
    if (float === 'left' || float === 'right') {
      wrapper.classList.add(`float-${float}`);
      table.classList.add('float-none');
      if (!hasFixedWidth) {
        wrapper.style.width = width || '100%';
        table.style.width = '100%';
      }
    } else if (hasFixedWidth && float !== 'center' && table.style.marginLeft !== 'auto' && table.style.marginRight !== 'auto') {
      wrapper.classList.add('width-fit-content');
    } else {
      wrapper.classList.add('full-width');
    }
    table.replaceWith(wrapper);
    wrapper.appendChild(table);
  });
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
