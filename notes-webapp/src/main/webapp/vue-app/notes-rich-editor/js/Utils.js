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
