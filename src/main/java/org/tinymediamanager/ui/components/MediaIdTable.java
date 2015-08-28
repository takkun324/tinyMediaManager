/*
 * Copyright 2012 - 2015 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.components;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.ScraperType;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * The class MediaIdTable is used to display / edit media ids
 *
 * @author Manuel Laggner
 */
public class MediaIdTable extends JTable {
  private static final long serialVersionUID = 8010722883277208728L;

  private Map<String, Object> idMap;
  private EventList<MediaId>  idList;
  private boolean             editable;

  /**
   * this constructor is used to display the ids
   *
   * @param ids
   *          a map containing the ids
   */
  public MediaIdTable(Map<String, Object> ids) {
    this.idMap = ids;
    this.editable = false;
    this.idList = convertIdMapToEventList(idMap);
    setModel(new DefaultEventTableModel<MediaId>(idList, new MediaIdTableFormat()));
    setTableHeader(null);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
  }

  public MediaIdTable(EventList<MediaId> ids, ScraperType type) {
    this.idMap = null;
    this.editable = true;
    this.idList = ids;
    setModel(new DefaultEventTableModel<MediaId>(idList, new MediaIdTableFormat()));
    setTableHeader(null);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    TableColumn column = getColumnModel().getColumn(0);

    Set<String> providerIds = new HashSet<>();
    for (MediaId id : ids) {
      providerIds.add(id.key);
    }
    for (MediaScraper scraper : MediaScraper.getMediaScrapers(type)) {
      providerIds.add(scraper.getId());
    }
    JComboBox<String> comboBox = new JComboBox<>(providerIds.toArray(new String[0]));

    column.setCellEditor(new DefaultCellEditor(comboBox));
  }

  public static EventList<MediaId> convertIdMapToEventList(Map<String, Object> idMap) {
    EventList<MediaId> idList = new BasicEventList<>();
    for (Entry<String, Object> entry : idMap.entrySet()) {
      MediaId id = new MediaId();
      id.key = entry.getKey();
      try {
        id.value = entry.getValue().toString();
      }
      catch (Exception e) {
        id.value = "";
      }
      idList.add(id);
    }

    return idList;
  }

  public static class MediaId {
    public String key;
    public String value;

    public MediaId() {
    }

    public MediaId(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(19, 31).append(key).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MediaId) || obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      MediaId other = (MediaId) obj;
      return StringUtils.equals(key, other.key);
    }
  }

  private class MediaIdTableFormat implements WritableTableFormat<MediaId> {
    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int column) {
      return "";
    }

    @Override
    public boolean isEditable(MediaId arg0, int arg1) {
      return editable;
    }

    @Override
    public Object getColumnValue(MediaId arg0, int arg1) {
      switch (arg1) {
        case 0:
          return arg0.key;

        case 1:
          return arg0.value;
      }
      return null;
    }

    @Override
    public MediaId setColumnValue(MediaId arg0, Object arg1, int arg2) {
      switch (arg2) {
        case 0:
          arg0.key = arg1.toString();
          break;

        case 1:
          arg0.value = arg1.toString();
          break;
      }
      return arg0;
    }
  }
}