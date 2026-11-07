'use strict';
/**
 * Table style picker of the rich editor: a « Table style » context-menu entry
 * on a table (Default, Simple, Striped, Bordered: one class on the table) and
 * a « Number column » entry on a cell (the `num` class on every cell of its
 * column, aligned to the end of the line). Labels come from the notes bundles
 * (notes.editor.table.*), which Crowdin translates, with the plugin's English
 * as the fallback where the page has not loaded them. The classes are styled by platform-ui `rich-text-table.less`,
 * in the published view and in the editor alike (EXO-93568).
 */
(function () {
  const STYLE_CLASSES = ['table-simple', 'table-striped', 'table-bordered'];
  const NUMERIC_CLASS = 'num';
  const STYLES = [
    {command: 'tableStyleDefault', className: null, label: 'styleDefault'},
    {command: 'tableStyleSimple', className: 'table-simple', label: 'styleSimple'},
    {command: 'tableStyleStriped', className: 'table-striped', label: 'styleStriped'},
    {command: 'tableStyleBordered', className: 'table-bordered', label: 'styleBordered'},
  ];
  const BUNDLE_PREFIX = 'notes.editor.table.';

  CKEDITOR.plugins.add('tableStyle', {
    requires: 'contextmenu,tabletools',
    lang: ['en'],

    init: function (editor) {
      const label = name => window.vueI18nMessages?.[`${BUNDLE_PREFIX}${name}`] || editor.lang.tableStyle[name];

      STYLES.forEach(style => {
        const command = editor.addCommand(style.command, {
          allowedContent: 'table(table-simple,table-striped,table-bordered)',
          requiredContent: 'table',
          exec: function () {
            const table = selectedTable(editor);
            if (table) {
              editor.fire('saveSnapshot');
              STYLE_CLASSES.forEach(className => table.removeClass(className));
              if (style.className) {
                table.addClass(style.className);
              }
              editor.fire('saveSnapshot');
              editor.fire('change');
            }
          },
        });
        editor.addFeature(command);
      });

      const numericCommand = editor.addCommand('tableNumericColumn', {
        allowedContent: 'td th(num)',
        requiredContent: 'td',
        exec: function () {
          const cell = selectedCell(editor);
          if (cell) {
            const numeric = !cell.hasClass(NUMERIC_CLASS);
            editor.fire('saveSnapshot');
            columnCells(cell).forEach(columnCell => {
              if (numeric) {
                columnCell.addClass(NUMERIC_CLASS);
              } else {
                columnCell.removeClass(NUMERIC_CLASS);
              }
            });
            editor.fire('saveSnapshot');
            editor.fire('change');
          }
        },
      });
      editor.addFeature(numericCommand);

      if (!editor.addMenuItems) {
        return;
      }
      editor.addMenuGroup('tableStyle', 90);
      const subItems = {};
      STYLES.forEach((style, index) => {
        subItems[style.command] = {
          label: label(style.label),
          command: style.command,
          group: 'tableStyle',
          order: index,
        };
      });
      editor.addMenuItems({
        ...subItems,
        tableStyle: {
          label: label('tableStyle'),
          group: 'tableStyle',
          order: 1,
          getItems: function () {
            const table = selectedTable(editor);
            const current = table && STYLES.find(style => style.className && table.hasClass(style.className));
            const items = {};
            STYLES.forEach(style => {
              const active = current ? current === style : !style.className;
              items[style.command] = active ? CKEDITOR.TRISTATE_ON : CKEDITOR.TRISTATE_OFF;
            });
            return items;
          },
        },
        tableNumericColumn: {
          label: label('numericColumn'),
          command: 'tableNumericColumn',
          group: 'tableStyle',
          order: 2,
        },
      });

      editor.contextMenu?.addListener(function (element) {
        if (!element || element.isReadOnly() || !element.getAscendant('table', true)) {
          return null;
        }
        const cell = element.getAscendant({td: 1, th: 1}, true);
        const menu = {tableStyle: CKEDITOR.TRISTATE_OFF};
        if (cell) {
          menu.tableNumericColumn = cell.hasClass(NUMERIC_CLASS) ? CKEDITOR.TRISTATE_ON : CKEDITOR.TRISTATE_OFF;
        }
        return menu;
      });
    },
  });

  /**
   * The table holding the selection, if any.
   *
   * @param {CKEDITOR.editor} editor the editor
   * @returns {CKEDITOR.dom.element} the table, or null
   */
  function selectedTable(editor) {
    const start = editor.getSelection()?.getStartElement();
    return start?.getAscendant('table', true) || null;
  }

  /**
   * The cell holding the selection, if any.
   *
   * @param {CKEDITOR.editor} editor the editor
   * @returns {CKEDITOR.dom.element} the td or th, or null
   */
  function selectedCell(editor) {
    const start = editor.getSelection()?.getStartElement();
    return start?.getAscendant({td: 1, th: 1}, true) || null;
  }

  /**
   * Every cell of a cell's column, header included, by the table's grid map so
   * that row and column spans keep the column.
   *
   * @param {CKEDITOR.dom.element} cell a td or th
   * @returns {CKEDITOR.dom.element[]} the column's cells, each once
   */
  function columnCells(cell) {
    const table = cell.getAscendant('table');
    const map = CKEDITOR.tools.buildTableMap(table);
    const rowIndex = cell.getParent().$.rowIndex;
    const columnIndex = map[rowIndex].indexOf(cell.$);
    const cells = [];
    map.forEach(row => {
      const domCell = row[columnIndex];
      if (domCell && !cells.includes(domCell)) {
        cells.push(domCell);
      }
    });
    return cells.map(domCell => new CKEDITOR.dom.element(domCell));
  }
})();
