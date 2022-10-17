// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package nl.jusx.pycharm.lockprofiler.render;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.ui.treeStructure.treetable.TreeTable;
//import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import nl.jusx.pycharm.lockprofiler.profile.Profile;
import nl.jusx.pycharm.lockprofiler.profile.ProfileSchema;
import org.apache.commons.collections.map.Flat3Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LockProfilerToolWindow implements Disposable {
    private Profile profile;
    private Project project;

    private JPanel wrapper;
    private JTabbedPane tabs;
    private JPanel lockContent;
    private JPanel lineContent;
    private JTable lockTable;
    private JScrollPane lockScroll;
    private JScrollPane lineScroll;
    private JTable lineTable;


    private static class LockTableModel extends DefaultTableModel {
        private final int firstTimeColumn;
        private final Class[] columnClass;
        private final long[] maxTimes;

        public LockTableModel(Object[][] data, Object[] columnNames, int firstTimeColumn) {
            super(data, columnNames);
            if (data.length > 0) {
                columnClass = Arrays.stream(data[0]).map(Object::getClass).toArray(Class[]::new);
                maxTimes = new long[data[0].length];
                // TODO there's probably a better way to compute this
                Arrays.stream(data).forEach(e -> {
                    // NOTE: `- 2` to include `hits` and `acquires`
                    for (int i = firstTimeColumn - 2; i < e.length; i++) {
                        maxTimes[i] = Math.max(maxTimes[i], (long) e[i]);
                    }
                });
            } else {
                 columnClass = new Class[] {};
                maxTimes = new long[] {};
            }
            this.firstTimeColumn = firstTimeColumn;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
//            if (columnIndex >= firstTimeColumn) {
//                return Long.class;
//            }
//            return String.class;
        }

        public long getMaxColumnValue(int columnIndex) {
            return maxTimes[columnIndex];
        }
    }

    private static class TimeRenderer extends JLabel implements TableCellRenderer {
        private final int firstTimeColumn;

        public TimeRenderer(int firstTimeColumn) {
            // TODO this makes selection highlight not show up on the affected cells, but is needed for background color
            super.setOpaque(true);
            this.firstTimeColumn = firstTimeColumn;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {

            // NOTE: `- 2` to include `hits` and `acquires`
            if (table.convertColumnIndexToModel(column) >= firstTimeColumn - 2) {
                long val = (long) value;

                // TODO make compatible with IDE color themes
                LockTableModel model = (LockTableModel) table.getModel();
                long maxTime = model.getMaxColumnValue(table.convertColumnIndexToModel(column));
                float timeFraction = 0.7f * (float) val / (float) maxTime;
                super.setBackground(new Color(timeFraction, 0.f, 0.f));

                if (table.convertColumnIndexToModel(column) >= firstTimeColumn) {
                    super.setText(ProfileSchema.formatTime(val));
                } else {
                    super.setText(String.valueOf(value));
                }
            } else {
                // NOTE: apparently `this` can be reused between multiple cells. It appears that `setText` is only
                //  called internally if the current text is "". Without manually calling `setText` here, this leads
                //  to columns 1 and 2 having time strings displayed, presumably because the JLabel from the last
                //  column of the previous row was reused.
                super.setText(String.valueOf(value));
                super.setBackground(new Color(0.f, 0.f, 0.f));
            }
            return this;
        }
    }

    public LockProfilerToolWindow(ToolWindow toolWindow, Project project) {
        this.project = project;

        lockTable.setDefaultRenderer(Long.class, new TimeRenderer(3));
        lineTable.setDefaultRenderer(Long.class, new TimeRenderer(5));

        lockTable.setAutoCreateRowSorter(true);
        lineTable.setAutoCreateRowSorter(true);
    }

    public void update(Profile profile) {
        String[] lockColumns = {"Name", "Hits", "Acquires", "Total Wait", "Avg Wait", "Max Wait", "Total Hold", "Avg Hold", "Max Hold"};
        Object[][] lockData = profile.schema.lock_stats
                .entrySet()
                .stream()
                .map(e -> {
                    ProfileSchema.LockStats val = e.getValue();
                    return new Object[] {
                            profile.schema.getLockName(e.getKey()),
                            val.hits,
                            val.acquires,
                            val.total_acquire_time,
                            val.avg_acquire_time,
                            val.max_acquire_time,
                            val.total_hold_time,
                            val.avg_hold_time,
                            val.max_hold_time,
                    };
                }).toArray(Object[][]::new);
        lockTable.setModel(new LockTableModel(lockData, lockColumns, 3));

        String[] lineColumns = {"File", "Line", "Name", "Hits", "Acquires", "Total Wait", "Avg Wait", "Max Wait", "Total Hold", "Avg Hold", "Max Hold"};
        List<Object[]> lineData = new ArrayList<>();

        profile.schema.file_stats.forEach((file, line_stats)
                -> line_stats.forEach((lineNo, lock_stats)
                -> lock_stats.forEach((lock_hash, val)
                -> lineData.add(new Object[] {
                file,
                lineNo,
                profile.schema.getLockName(lock_hash),
                val.hits,
                val.acquires,
                val.total_acquire_time,
                val.avg_acquire_time,
                val.max_acquire_time,
                val.total_hold_time,
                val.avg_hold_time,
                val.max_hold_time,
        }))));
        lineTable.setModel(new LockTableModel(lineData.toArray(Object[][]::new), lineColumns, 5));
    }

    public JPanel getContent() {
        return wrapper;
    }

    @Override
    public void dispose() {

    }
}
