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

      const uploadUrl = editor.config.uploadUrl;
      let uploadId = generateRandomId();
      editor.config.uploadUrl = uploadUrl + uploadId;

      // handel files comes from dataTransfer
      const fileTools = CKEDITOR.fileTools;

      function handleFileUpload(file, moveSelectionPosition) {
        if (editor.getData().trim() === '') {
          editor.setData('&nbsp;');
        }
        const loader = editor.uploadRepository.create(file);
        const reader = new FileReader();

        reader.onload = function (e) {
          const dataUrl = e.target.result;

          // Create a temporary document to safely insert the image
          const tempDoc = document.implementation.createHTMLDocument('');
          const temp = new CKEDITOR.dom.element(tempDoc.body);
          temp.data('cke-editable', 1);

          temp.appendHtml(`<img class="cke_upload_uploading" cke_upload_id="${uploadId}" src="${dataUrl}" alt="" />`);

          const img = temp.find('img').getItem(0);
          loader.data = dataUrl;
          loader.upload(editor.config.uploadUrl + uploadId); // Ensure unique upload URL

          // Insert the image and trigger autogrow
          editor.insertHtml(img.getOuterHtml());
          editor.fire('change');

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
            cleanWidget(dataUrl);
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
          insertedImage.removeClass('cke_upload_uploading');
          insertedImage.removeAttribute('data-cke-saved-src');
          insertedImage.removeAttribute('data-cke-widget-data');
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