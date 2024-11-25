'use strict';
(function () {
  CKEDITOR.plugins.add('insertImage', {
    requires: 'uploadwidget,autogrow',

    onLoad: function () {
      CKEDITOR.addCss(
        '.cke_upload_uploading {' +
        'opacity: 0.3' +
        '}' +
        '.cke_widget_image {' +
        '    max-width: 100%;' +
        '    margin: 10px 5px 10px 5px !important' +
        '}' +
        '.cke_widget_image img {' +
        '    max-width: 100%;' +
        '    text-align: center' +
        '}'
      );
    },
    lang: ['en', 'fr'],
    icons: 'insertImage',

    init: function (editor) {
      editor.ui.addButton('insertImage', {
        label: editor.lang.insertImage.buttonTooltip,
        command: 'insertImage',
        toolbar: 'insert'
      });

      // add insert image command
      editor.addCommand('insertImage', {
        exec: function () {
          const input = document.createElement('input');
          input.type = 'file';
          input.accept = 'image/*';
          input.click();

          input.onchange = function () {
            const file = input.files[0];
            if (file) {
              handleFileUpload(file, false);
            }
          };
        }
      });
      document.addEventListener('update-processed-image-url', (event) => {
        const stringHtmlContent = event.detail.content;
        replaceProcessedImageUrl(stringHtmlContent);
      });

      function replaceProcessedImageUrl(content) {
        replaceNewlyUploadedImageUrl(content);
        replaceTheCopiedOrMovedImagUrl(content);
      }

      function replaceNewlyUploadedImageUrl(content) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(content, 'text/html');

        // Find all <img> elements with the attribute 'archive-cke-id' in the HTML content
        const newImages = doc.querySelectorAll('img[archived_cke_uploadId]');

        newImages.forEach((newImg) => {
          const archiveCkeId = newImg.getAttribute('archived_cke_uploadId'); // Get the archive-cke-id
          const newSrc = newImg.getAttribute('src'); // Get the new src from the provided HTML string

          const existingImg = editor.document.findOne(`img[cke_upload_id="${archiveCkeId}"]`);
          if (existingImg) {
            const oldSrc = existingImg.getAttribute('src');
            existingImg.setAttribute('src', newSrc);
            const widget = Object.values(editor.widgets.instances).find(w => w.name === 'image' && w.data.src === oldSrc);
            if (widget) {
              widget.data.src = newSrc;
              existingImg.removeClass('cke_upload_uploading');
              existingImg.removeAttribute('cke_upload_id');
              existingImg.removeAttribute('data-cke-widget-data');
              existingImg.removeAttribute('data-cke-saved-src');

            }
          }
        });
      }

      function replaceTheCopiedOrMovedImagUrl(content) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(content, 'text/html');

        // Find all <img> elements with 'src' starting with '/attachment/v1/social/'
        const images = Array.from(doc.querySelectorAll('img[src^="/portal/rest/v1/social/attachments/"]'));

        images.forEach((image) => {
          const src = image.getAttribute('src'); // Get the src attribute
          const fileId = src.split('/').pop(); // Extract the file ID (last part after '/')

          if (fileId) {
            // Find the corresponding image in the editor with the same file ID
            const existingImg = editor.document.findOne(`img[src$="/${fileId}"]`);
            if (existingImg) {
              // Update the existing image's src with the new src
              existingImg.setAttribute('src', src);
              existingImg.removeAttribute('data-cke-widget-data');
              existingImg.removeAttribute('data-cke-saved-src');

              // Optionally update any associated CKEditor widgets
              const widget = Object.values(editor.widgets.instances).find(
                (w) => w.name === 'image' && w.data.src.includes(fileId)
              );
              if (widget) {
                widget.data.src = src;
              }
            }
          }
        });
      }

      const uploadUrl = editor.config.uploadUrl;
      let uploadId = generateRandomId();
      editor.config.uploadUrl = uploadUrl + uploadId;

      // handel files comes from dataTransfer
      const fileTools = CKEDITOR.fileTools;

      function dataURLtoBlob(dataURL) {
        // Split the data URL into the metadata and the base64 data
        const [header, base64Data] = dataURL.split(',');
        // Extract the MIME type from the header
        const mimeMatch = header.match(/^data:([a-zA-Z0-9.+-]+\/[a-zA-Z0-9.+-]+);base64$/);
        if (!mimeMatch) {
          throw new Error('Invalid Data URL format.');
        }
        const mime = mimeMatch[1];
        // Decode the base64 data
        const binaryString = atob(base64Data);
        const length = binaryString.length;
        const u8Array = new Uint8Array(length);
        // Convert binary string to Uint8Array
        for (let i = 0; i < length; i++) {
          u8Array[i] = binaryString.charCodeAt(i);
        }
        return new Blob([u8Array], {type: mime});
      }

      function handleFileUpload(file, moveSelectionPosition) {
        if (editor.getData().trim() === '') {
          editor.insertHtml('<p></p>');
          editor.focus();
        }
        const loader = editor.uploadRepository.create(file);
        const reader = new FileReader();

        reader.onload = function (e) {
          const dataUrl = e.target.result;

          const blob = dataURLtoBlob(dataUrl);
          const blobUrl = URL.createObjectURL(blob);

          // Create a temporary document to safely insert the image
          const tempDoc = document.implementation.createHTMLDocument('');
          const temp = new CKEDITOR.dom.element(tempDoc.body);
          temp.data('cke-editable', 1);

          temp.appendHtml(`<img class="cke_upload_uploading" cke_upload_id="${uploadId}" src="${blobUrl}" alt="" />`);

          const img = temp.find('img').getItem(0);
          loader.data = dataUrl;
          loader.upload(editor.config.uploadUrl + uploadId); // Ensure unique upload URL

          // Insert the image and trigger autogrow
          editor.insertHtml(img.getOuterHtml());
          editor.focus();

          if (moveSelectionPosition) {
            const range = editor.getSelection().getRanges()[0];
            range.moveToPosition(range.endContainer, CKEDITOR.POSITION_AFTER_END);
            editor.getSelection().selectRanges([range]);
          }
          editor.execCommand('autogrow');

          // Bind notifications for the upload process
          fileTools.bindNotifications(editor, loader);

          loader.on('uploaded', function () {
            // Clean up the uploaded image once done
            cleanWidget(blobUrl);
            editor.fire('change');
          });
        };

        reader.readAsDataURL(file);
      }

      // handel temp upload
      editor.on('fileUploadRequest', function (evt) {
        evt.stop();
        const fileLoader = evt.data.fileLoader;
        const formData = new FormData();
        const xhr = fileLoader.xhr;

        fileLoader.uploadId = uploadId;
        fileLoader.thumbnailURL = evt.data.fileLoader.data;
        fileLoader.uploadUrl = editor.config.uploadUrl;

        xhr.open('POST', fileLoader.uploadUrl, true);
        formData.append('upload', fileLoader.file, fileLoader.fileName);
        fileLoader.xhr.send(formData);

        uploadId = generateRandomId();
        editor.config.uploadUrl = uploadUrl + uploadId;
      },);
      editor.on('fileUploadResponse', function (evt) {
        evt.stop();
        const data = evt.data;
        const xhr = data.fileLoader.xhr;
        const status = xhr.status;

        if (status === 200) {
          data.url = data.fileLoader.thumbnailURL;
        } else {
          data.message = editor.lang.imageError;
          evt.cancel();
          return abortUpload(data.fileLoader.uploadId);
        }
      });

      editor.on('paste', function (evt) {
        // For performance reason do not parse data if it does not contain img.
        const files = Array.from(evt.data.dataTransfer._.files);
        if (files.length === 0) {
          return;
        }
        files.forEach((file) => {
          handleFileUpload(file, true);
        });
        evt.stop();
      });

      function cleanWidget(dataUrl) {
        const insertedImage = editor.document.findOne(`img[src="${dataUrl}"]`);
        if (insertedImage) {
          insertedImage.removeAttribute('data-cke-saved-src');
          insertedImage.removeAttribute('data-cke-widget-data');
          setTimeout(() => {
            insertedImage.removeClass('cke_upload_uploading');
          }, 1500);
        }
      }
    }
  });
})();

function generateRandomId() {
  const MAX_RANDOM_NUMBER = 100000;
  const random = Math.round(Math.random() * MAX_RANDOM_NUMBER);
  const now = Date.now();
  return `${random}-${now}`;
}

function abortUpload(uploadId) {
  return fetch(`${eXo.env.portal.context}/upload?uploadId=${uploadId}&action=abort`, {
    method: 'POST',
    credentials: 'include'
  });
}